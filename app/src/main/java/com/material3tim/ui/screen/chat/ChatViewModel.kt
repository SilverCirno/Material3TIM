package com.material3tim.ui.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.material3tim.data.model.ChatItem
import com.material3tim.data.repository.LiveChats
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.component.SearchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 模拟刷新耗时，保证行为可预期（第一阶段没有真实数据源）。 */
private const val REFRESH_DURATION_MILLIS = 600L

/**
 * 消息页 ViewModel：第一阶段直接使用 [ChatRepository] 的示例数据，
 * 搜索过滤是同步的，因此不需要 debounce / 协程调度。
 */
class ChatViewModel : ViewModel() {

    /** 宿主里先展示空列表并异步拉真实会话；独立 App 直接用示例数据。 */
    private var allChats: List<ChatItem> = if (AppDataSources.hasLiveDataSource) {
        emptyList()
    } else {
        LiveChats.current
    }

    private val _uiState = MutableStateFlow(
        ChatUiState(
            chats = allChats,
            pinnedChats = allChats.filter { it.isPinned },
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    // ---------- 首页滑动期间的 LiveChats 推送合并 ----------
    // 实测滑动 6s 内会有 12~16 次会话列表推送；滑动中每次都替换 chats 会让可见行在滚动手势帧里重组。
    // 滑动中只暂存最新一份，停止后再一次性 apply（图片/动图加载不受影响，只有数据刷新被延后到停滑）。
    @Volatile
    private var listScrollInProgress = false
    private var pendingLiveChats: List<ChatItem>? = null

    init {
        if (AppDataSources.hasLiveDataSource) {
            viewModelScope.launch {
                LiveChats.liveFlow.collect { live ->
                    if (live != null) {
                        allChats = live
                        // 滑动中只暂存（allChats 已更新，搜索等主动操作仍用最新数据）；
                        // 停止滑动后由 setListScrollInProgress(false) 一次性应用，避免掉帧。
                        if (listScrollInProgress) {
                            pendingLiveChats = live
                        } else {
                            applyChats(live)
                        }
                    }
                }
            }
        }
        reloadChats(showRefreshing = false)
    }

    /** 下拉刷新：宿主里重新拉取 TIM 真实会话；独立 App 保留 600ms 的模拟刷新。 */
    fun onRefresh() {
        refreshJob?.cancel()
        if (!AppDataSources.hasLiveDataSource) {
            refreshJob = viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                delay(REFRESH_DURATION_MILLIS)
                _uiState.update { it.copy(isRefreshing = false) }
            }
            return
        }
        reloadChats(showRefreshing = true)
    }

    private fun reloadChats(showRefreshing: Boolean) {
        if (!AppDataSources.hasLiveDataSource) return
        refreshJob = viewModelScope.launch {
            if (showRefreshing) _uiState.update { it.copy(isRefreshing = true) }
            val loaded = withContext(Dispatchers.IO) { LiveChats.refresh() }
            if (loaded != null) {
                allChats = loaded
                applyChats(loaded)
            }
            if (showRefreshing) _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun applyChats(chats: List<ChatItem>) {
        val query = _uiState.value.searchStatus.searchText
        val filtered = if (query.isBlank()) chats else chats.filter { chat ->
            chat.name.contains(query, ignoreCase = true) ||
                chat.lastMessage.contains(query, ignoreCase = true) ||
                chat.draft?.contains(query, ignoreCase = true) == true
        }
        val pinned = filtered.filter { it.isPinned }
        // 等值短路：LiveChats 推送内容与当前一致（ChatItem/List 都是 data class 等值比较）时
        // 直接返回，不触发 _uiState.update → 可见列表行不会做无意义重组。
        val current = _uiState.value
        if (filtered == current.chats && pinned == current.pinnedChats) return
        _uiState.update { state ->
            state.copy(
                chats = filtered,
                pinnedChats = pinned,
            )
        }
    }

    /**
     * 首页列表滑动状态回调（由 ChatPagerMaterial/ChatPagerMiuix 的
     * `LazyListState.isScrollInProgress` 驱动，页面 dispose 时复位 false）。
     * 停止滑动时若有暂存推送，立即应用一次补上。
     */
    fun setListScrollInProgress(scrolling: Boolean) {
        listScrollInProgress = scrolling
        if (!scrolling) {
            val pending = pendingLiveChats ?: return
            pendingLiveChats = null
            viewModelScope.launch { applyChats(pending) }
        }
    }

    /** 同步 SearchStatus（Miuix 搜索状态机使用），同时过滤会话列表。 */
    fun updateSearchStatus(status: SearchStatus) {
        val query = status.searchText
        val filtered = filterChats(query)
        _uiState.update { state ->
            state.copy(
                searchStatus = status.copy(
                    resultStatus = if (query.isEmpty()) {
                        SearchStatus.ResultStatus.DEFAULT
                    } else {
                        SearchStatus.ResultStatus.SHOW
                    }
                ),
                chats = filtered,
                pinnedChats = filtered.filter { it.isPinned },
            )
        }
    }

    fun updateSearchText(text: String) {
        updateSearchStatus(_uiState.value.searchStatus.copy(searchText = text))
    }

    fun clearSearch() {
        updateSearchStatus(_uiState.value.searchStatus.copy(searchText = ""))
    }

    // ---------- 会话卡片长按菜单：置顶 / 标为未读 / 删除 ----------
    // 统一策略：先本地即时更新（乐观反馈）→ IO 调宿主 ChatSettingSource → 成功后刷新真实列表；
    // 失败回滚本地并回调 onFailure（由 UI 弹 Toast）。独立 App 没有数据源，同样走失败回滚分支。

    /** 置顶 / 取消置顶。 */
    fun setPinned(chat: ChatItem, pinned: Boolean, onFailure: () -> Unit = {}) {
        applyLocalChatUpdate(chat.id) { it.copy(isPinned = pinned) }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                AppDataSources.chatSettingSource?.setChatPinned(chat.id, pinned) ?: false
            }
            if (ok) {
                reloadChatsSilently()
            } else {
                applyLocalChatUpdate(chat.id) { it.copy(isPinned = chat.isPinned) }
                onFailure()
            }
        }
    }

    /** 标为未读 / 标为已读（未读时 at least 1，已读时清零）。 */
    fun setUnread(chat: ChatItem, unread: Boolean, onFailure: () -> Unit = {}) {
        applyLocalChatUpdate(chat.id) {
            it.copy(unreadCount = if (unread) it.unreadCount.coerceAtLeast(1) else 0)
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                AppDataSources.chatSettingSource?.setChatUnread(chat.id, unread) ?: false
            }
            if (ok) {
                reloadChatsSilently()
            } else {
                applyLocalChatUpdate(chat.id) { it.copy(unreadCount = chat.unreadCount) }
                onFailure()
            }
        }
    }

    /** 删除会话（仅从消息列表移除）：本地先移除，失败按原索引恢复。 */
    fun deleteChat(chat: ChatItem, onFailure: () -> Unit = {}) {
        val removedIndex = allChats.indexOfFirst { it.id == chat.id }
        applyLocalChatRemoval(chat.id)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                AppDataSources.chatSettingSource?.deleteChat(chat.id) ?: false
            }
            if (ok) {
                reloadChatsSilently()
            } else {
                restoreChat(chat, removedIndex)
                onFailure()
            }
        }
    }

    /** 后台刷新真实会话列表（失败则保留本地乐观状态）。 */
    private suspend fun reloadChatsSilently() {
        val refreshed = withContext(Dispatchers.IO) { LiveChats.refresh() }
        if (refreshed != null) {
            allChats = refreshed
            applyChats(refreshed)
        }
    }

    private fun applyLocalChatUpdate(chatId: String, transform: (ChatItem) -> ChatItem) {
        allChats = allChats.map { chat -> if (chat.id == chatId) transform(chat) else chat }
        applyChats(allChats)
    }

    private fun applyLocalChatRemoval(chatId: String) {
        allChats = allChats.filterNot { it.id == chatId }
        applyChats(allChats)
    }

    private fun restoreChat(chat: ChatItem, index: Int) {
        val restored = allChats.toMutableList()
        if (index in 0..restored.size) {
            restored.add(index, chat)
        } else {
            restored.add(chat)
        }
        allChats = restored
        applyChats(restored)
    }

    private fun filterChats(query: String): List<ChatItem> {
        if (query.isBlank()) return allChats
        return allChats.filter { chat ->
            chat.name.contains(query, ignoreCase = true) ||
                chat.lastMessage.contains(query, ignoreCase = true) ||
                chat.draft?.contains(query, ignoreCase = true) == true
        }
    }
}
