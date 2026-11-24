package com.material3tim.ui.screen.chat

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.material3tim.data.model.GroupNotice
import com.material3tim.data.repository.ChatRepository
import com.material3tim.data.repository.MessageRepository
import com.material3tim.data.source.AppDataSources
import com.material3tim.data.source.DataUpdate
import com.material3tim.data.source.LiveEvents
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 群公告列表页：展示该群的全部历史群公告（按时间倒序），
 * 每条公告卡片可点击进入详情页看完整正文（不截断）。
 */
@Composable
fun ChatNoticeScreen(chatId: String) {
    val navigator = LocalNavigator.current
    var groupName by remember(chatId) { mutableStateOf("") }
    var notices by remember(chatId) { mutableStateOf<List<GroupNotice>>(emptyList()) }
    var loaded by remember(chatId) { mutableStateOf(false) }
    var loading by remember(chatId) { mutableStateOf(AppDataSources.hasLiveDataSource) }
    var canManage by remember(chatId) { mutableStateOf(false) }
    var refreshTick by remember(chatId) { mutableStateOf(0) }

    // 首屏快路径：公告列表 loadGroupNotices（bridge 现在缓存优先、后台刷新）立即返回，
    // 先渲染列表/空态，不再等成员角色重试 6×1s；角色由 fast 资料 + dataUpdates 稍后补齐。
    LaunchedEffect(chatId, refreshTick) {
        if (refreshTick > 0) kotlinx.coroutines.delay(150L)
        if (notices.isEmpty() && !loaded) loading = true
        var list = withContext(Dispatchers.IO) {
            runCatching { AppDataSources.chatSource?.loadGroupNotices(chatId) }.getOrNull()
        }.orEmpty()
        var name = ""
        var manage = false
        if (AppDataSources.hasLiveDataSource) {
            // fast 群资料（毫秒级）：拿群名 + 若 members 已就绪则先算发布权限。
            val fast = withContext(Dispatchers.IO) {
                runCatching { AppDataSources.chatSource?.loadChatInfoFast(chatId) }.getOrNull()
            }
            if (fast != null) {
                name = fast.chat.name
                val me = fast.members.firstOrNull { it.isMe }
                manage = fast.members.isNotEmpty() && (me?.isOwner == true || me?.isAdmin == true)
            }
            // 先放开首屏（列表/空态立即可见）。
            groupName = name
            notices = list
            canManage = manage
            loaded = true
            loading = false
            // 后台全量资料（缓存优先）：补最新公告 memo 与真实 members 角色。
            val live = withContext(Dispatchers.IO) {
                runCatching { AppDataSources.chatSource?.loadChatInfo(chatId) }.getOrNull()
            }
            if (live != null) {
                name = live.chat.name
                val me = live.members.firstOrNull { it.isMe }
                manage = live.members.isNotEmpty() && (me?.isOwner == true || me?.isAdmin == true)
                if (list.isEmpty() && live.notice.isNotBlank()) {
                    list = listOf(
                        GroupNotice(id = "memo", title = "群公告", content = live.notice, timeMillis = 0L),
                    )
                }
                Log.i(
                    "Material3Tim",
                    "notice page load: chat=$chatId notices=${list.size} members=${live.members.size} " +
                        "me=${me?.name} owner=${me?.isOwner} admin=${me?.isAdmin} canManage=$manage",
                )
            }
        } else {
            ChatRepository.chatById(chatId)?.let { chat ->
                name = chat.name
                val sample = MessageRepository.chatInfoFor(chat)
                if (list.isEmpty() && sample.notice.isNotBlank()) {
                    list = listOf(
                        GroupNotice(
                            id = "memo",
                            title = "群公告",
                            content = sample.notice,
                            timeMillis = 0L,
                        ),
                    )
                }
            }
        }
        groupName = name
        notices = list
        canManage = manage
        loaded = true
        loading = false
    }
    // 后台公告/资料刷新完成：直接重读缓存数据局部更新列表与发布权限，不重试、不整页 loading。
    LaunchedEffect(chatId) {
        if (!AppDataSources.hasLiveDataSource) return@LaunchedEffect
        LiveEvents.dataUpdates.collect { update ->
            if (update.chatId == chatId &&
                (update.kind == DataUpdate.Kind.NOTICES ||
                    update.kind == DataUpdate.Kind.CHAT_INFO ||
                    update.kind == DataUpdate.Kind.MEMBERS)
            ) {
                val freshList = withContext(Dispatchers.IO) {
                    runCatching { AppDataSources.chatSource?.loadGroupNotices(chatId) }.getOrNull()
                }.orEmpty()
                val info = withContext(Dispatchers.IO) {
                    runCatching { AppDataSources.chatSource?.loadChatInfo(chatId) }.getOrNull()
                }
                notices = if (freshList.isEmpty() && !info?.notice.isNullOrBlank()) {
                    listOf(
                        GroupNotice(
                            id = "memo",
                            title = "群公告",
                            content = info.notice,
                            timeMillis = 0L,
                        ),
                    )
                } else {
                    freshList
                }
                if (info != null) {
                    groupName = info.chat.name
                    val me = info.members.firstOrNull { it.isMe }
                    canManage = info.members.isNotEmpty() &&
                        (me?.isOwner == true || me?.isAdmin == true)
                }
                loaded = true
                loading = false
            }
        }
    }

    val onBack: () -> Unit = { navigator.pop() }
    val onOpenNotice: (GroupNotice) -> Unit = { notice ->
        navigator.push(Route.ChatNoticeDetail(chatId, notice.id))
    }
    when (LocalUiMode.current) {
        UiMode.Miuix -> ChatNoticeMiuix(
            groupName = groupName,
            notices = notices,
            loaded = loaded,
            loading = loading,
            chatId = chatId,
            canManage = canManage,
            onBack = onBack,
            onOpenNotice = onOpenNotice,
            onRefresh = { refreshTick += 1 },
        )
        UiMode.Material -> ChatNoticeMaterial(
            groupName = groupName,
            notices = notices,
            loaded = loaded,
            loading = loading,
            chatId = chatId,
            canManage = canManage,
            onBack = onBack,
            onOpenNotice = onOpenNotice,
            onRefresh = { refreshTick += 1 },
        )
    }
}

/** 群公告时间显示：yyyy-MM-dd HH:mm；0 表示没有时间（兜底公告）。 */
internal fun formatNoticeTime(timeMillis: Long): String {
    if (timeMillis <= 0L) return ""
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timeMillis))
}
