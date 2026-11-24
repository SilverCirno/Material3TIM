package com.material3tim.ui.screen.chat

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.ChatInfoData
import com.material3tim.data.repository.ChatRepository
import com.material3tim.data.repository.MessageRepository
import com.material3tim.data.source.AppDataSources
import com.material3tim.data.source.DataUpdate
import com.material3tim.data.source.LiveEvents
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 聊天信息页入口（单聊资料 / 群资料）。
 *
 * 从聊天详情页的「更多」按钮通过 `Route.ChatInfo(chatId)` 进入，按 [LocalUiMode] 分发两套实现。
 *
 * 宿主冷启动时 [AppDataSources.chatSource]?.loadChatInfo 可能短暂返回 null 或 members 未就绪，
 * 这里带重试（群聊要求 members 非空才算就绪）；加载中显示 loading，失败显示重试入口，
 * 不在宿主里展示示例成员数据（否则会因找不到 isMe 而隐藏群管理入口）。
 */
@Composable
fun ChatInfoScreen(chatId: String) {
    val navigator = LocalNavigator.current
    val isHost = AppDataSources.hasLiveDataSource
    // 宿主模式：先用真实 ChatItem 构造「页面骨架」（无示例成员/公告/简介），
    // 让群名、群号、置顶等静态信息先显示；成员区单独 loading，live 到达后再替换。
    val skeleton = remember(chatId) {
        ChatRepository.chatById(chatId)?.let { chat ->
            if (isHost) {
                ChatInfoData(
                    chat = chat,
                    members = emptyList(),
                    notice = "",
                    timId = groupCodeFromChatId(chatId),
                    description = "",
                    memberCount = chat.memberCount,
                )
            } else {
                MessageRepository.chatInfoFor(chat)
            }
        }
    }
    var data by remember(chatId) { mutableStateOf(skeleton) }
    var loading by remember(chatId) { mutableStateOf(isHost) }
    var loadFailed by remember(chatId) { mutableStateOf(false) }
    var refreshTick by remember(chatId) { mutableStateOf(0) }

    // 首屏快慢拆分：先 loadChatInfoFast（毫秒级缓存）立即渲染，再 loadChatInfo 全量覆盖；
    // 有 fast 数据时不再走 4×800ms 长重试；无 fast 才保留短重试保底。
    LaunchedEffect(chatId, refreshTick) {
        if (!isHost) return@LaunchedEffect
        if (refreshTick > 0) {
            // 管理操作/用户手动刷新：绕过缓存拉最新，短补偿 150ms 等内核写入（原 300ms）。
            AppDataSources.chatSource?.invalidateChatInfoCache(chatId)
            kotlinx.coroutines.delay(150L)
        }
        if (data == null) {
            loading = true
            loadFailed = false
        }
        val fast = withContext(Dispatchers.IO) {
            runCatching { AppDataSources.chatSource?.loadChatInfoFast(chatId) }.getOrNull()
        }
        if (fast != null) {
            data = fast
            loading = false
            loadFailed = false
        }
        val live = if (fast != null) {
            // 已有 fast 数据：只调一次（bridge 内部缓存优先 + 后台刷新），不再长重试。
            withContext(Dispatchers.IO) {
                runCatching { AppDataSources.chatSource?.loadChatInfo(chatId) }.getOrNull()
            }
        } else {
            loadChatInfoWithRetry(chatId, maxAttempts = 3, delayMillis = 600L)
        }
        if (live != null) {
            val liveChat = withContext(Dispatchers.IO) {
                runCatching {
                    AppDataSources.chatSource?.loadChats()?.firstOrNull { it.id == chatId }
                }.getOrNull()
            }
            data = if (liveChat != null) live.copy(chat = liveChat) else live
            loadFailed = false
        } else if (data == null) {
            // 无骨架数据且全量失败：显示加载失败 + 重试入口。
            loadFailed = true
        }
        loading = false
    }
    // 后台慢查询完成（桥接 emit）：直接用现已更新的缓存数据局部覆盖，不再走 refreshTick/invalidate 全量重试。
    LaunchedEffect(chatId) {
        if (!isHost) return@LaunchedEffect
        LiveEvents.dataUpdates.collect { update ->
            if (update.chatId == chatId &&
                (update.kind == DataUpdate.Kind.CHAT_INFO || update.kind == DataUpdate.Kind.MEMBERS)
            ) {
                val fresh = withContext(Dispatchers.IO) {
                    runCatching { AppDataSources.chatSource?.loadChatInfo(chatId) }.getOrNull()
                }
                if (fresh != null) {
                    val liveChat = withContext(Dispatchers.IO) {
                        runCatching {
                            AppDataSources.chatSource?.loadChats()?.firstOrNull { it.id == chatId }
                        }.getOrNull()
                    }
                    data = if (liveChat != null) fresh.copy(chat = liveChat) else fresh
                    loadFailed = false
                    loading = false
                }
            }
        }
    }
    val onBack: () -> Unit = { navigator.pop() }

    val current = data
    if (current == null) {
        ChatInfoLoadStatePage(
            loading = loading,
            failed = loadFailed,
            onBack = onBack,
            onRetry = { refreshTick += 1 },
        )
        return
    }

    val onViewAllMembers: () -> Unit = {
        navigator.push(com.material3tim.ui.navigation3.Route.ChatMembers(chatId))
    }
    val onOpenNotice: () -> Unit = {
        navigator.push(com.material3tim.ui.navigation3.Route.ChatNotice(chatId))
    }
    // 「查找聊天记录」：跳转到新搜索页；独立 App（无 chatSettingSource）在搜索页里提示仅宿主可用。
    val onSearchHistory: () -> Unit = {
        navigator.push(com.material3tim.ui.navigation3.Route.ChatSearch(chatId))
    }
    // 群管理操作成功后刷新资料/成员角色；退群/解散成功后回到会话列表。
    val onRefresh: () -> Unit = { refreshTick += 1 }
    val onExit: () -> Unit = {
        navigator.popUntil { route -> route is com.material3tim.ui.navigation3.Route.Main }
    }
    when (LocalUiMode.current) {
        UiMode.Miuix -> ChatInfoMiuix(
            data = current,
            chatId = chatId,
            onBack = onBack,
            onViewAllMembers = onViewAllMembers,
            onOpenNotice = onOpenNotice,
            onSearchHistory = onSearchHistory,
            onRefresh = onRefresh,
            onExit = onExit,
            loading = loading,
            loadFailed = loadFailed,
            onRetry = { refreshTick += 1 },
        )
        UiMode.Material -> ChatInfoMaterial(
            data = current,
            chatId = chatId,
            onBack = onBack,
            onViewAllMembers = onViewAllMembers,
            onOpenNotice = onOpenNotice,
            onSearchHistory = onSearchHistory,
            onRefresh = onRefresh,
            onExit = onExit,
            loading = loading,
            loadFailed = loadFailed,
            onRetry = { refreshTick += 1 },
        )
    }
}

/** 加载中 / 加载失败（带重试）的简单整页状态，两套 UiMode 通用。 */
@Composable
private fun ChatInfoLoadStatePage(
    loading: Boolean,
    failed: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val isMaterial = LocalUiMode.current == UiMode.Material
    val contentColor = if (isMaterial) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MiuixTheme.colorScheme.onSurface
    }
    val containerColor = if (isMaterial) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MiuixTheme.colorScheme.background
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.chat_detail_back),
                tint = contentColor,
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (loading || !failed) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = contentColor,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.loading),
                    color = contentColor,
                )
            } else {
                Text(
                    text = stringResource(R.string.group_files_load_failed),
                    color = contentColor,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRetry) {
                    Text(text = stringResource(R.string.group_files_retry))
                }
            }
        }
    }
}


/** 从会话 id 解析群号：`tim_2_<groupCode>` → groupCode 字符串；非群会话返回空串。 */
internal fun groupCodeFromChatId(chatId: String): String {
    val code = chatId.removePrefix("tim_2_")
    return code.takeIf { it != chatId && it.isNotBlank() && it.all(Char::isDigit) }.orEmpty()
}

/**
 * 宿主冷启动时 `loadChatInfo` 可能返回 null 或 members 还没就绪：
 * 最多重试 [maxAttempts] 次、每次间隔 [delayMillis]；群聊要求 members 非空才算就绪。
 * 全部重试后返回最后一次非 null 结果（即便 members 为空，也可用其 chat 名/群号/简介），全 null 返回 null。
 */
internal suspend fun loadChatInfoWithRetry(
    chatId: String,
    maxAttempts: Int = 4,
    delayMillis: Long = 800L,
): ChatInfoData? {
    if (AppDataSources.chatSource == null) return null
    var last: ChatInfoData? = null
    repeat(maxAttempts) { attempt ->
        val live = withContext(Dispatchers.IO) {
            runCatching { AppDataSources.chatSource?.loadChatInfo(chatId) }.getOrNull()
        }
        if (live != null) {
            last = live
            val ready = !live.chat.isGroup || live.members.isNotEmpty()
            if (ready) return live
        }
        if (attempt < maxAttempts - 1) delay(delayMillis)
    }
    return last
}

/**
 * 群成员数口径：宿主内优先用真实成员列表大小（TIM 缓存的 chat.memberCount 可能偏大，
 * 实测出现「缓存 6 / 真实 2」），独立 App 或未拿到成员时回退资料里的 memberCount。
 */
internal fun effectiveMemberCount(data: ChatInfoData): Int = when {
    AppDataSources.hasLiveDataSource && data.members.isNotEmpty() -> data.members.size
    data.memberCount > 0 -> data.memberCount
    data.chat.memberCount > 0 -> data.chat.memberCount
    else -> data.members.size
}

/**
 * 置顶聊天 / 消息免打扰的公共「乐观更新」逻辑（两套 UI 共用）：
 * 先改本地状态 → 协程里在 IO 线程调用宿主数据源 → 失败回滚并 Toast。
 * 独立 App（`AppDataSources.chatSettingSource == null`）只 Toast「仅宿主内可用」，不改本地状态。
 */
internal fun CoroutineScope.updateChatSetting(
    context: Context,
    newValue: Boolean,
    hostOnlyMessage: String,
    failedMessage: String,
    setLocal: (Boolean) -> Unit,
    sourceCall: () -> Boolean,
) {
    if (!AppDataSources.hasLiveChatSettings) {
        Toast.makeText(context, hostOnlyMessage, Toast.LENGTH_SHORT).show()
        return
    }
    setLocal(newValue)
    launch {
        val ok = withContext(Dispatchers.IO) {
            runCatching(sourceCall).getOrDefault(false)
        }
        if (!ok) {
            setLocal(!newValue)
            Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show()
        }
    }
}
