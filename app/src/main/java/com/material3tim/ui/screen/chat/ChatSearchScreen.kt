package com.material3tim.ui.screen.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.material3tim.R
import com.material3tim.data.model.ChatMessage
import com.material3tim.data.model.ChatMessageType
import com.material3tim.data.source.AppDataSources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** 查找聊天记录页的 UI 状态。 */
internal sealed interface ChatSearchUiState {
    /** 还没有输入关键词。 */
    data object Idle : ChatSearchUiState

    /** 正在搜索。 */
    data object Loading : ChatSearchUiState

    /** 搜索成功但没有匹配记录。 */
    data object Empty : ChatSearchUiState

    /** 搜索失败（宿主数据源报错）。 */
    data object Failed : ChatSearchUiState

    /** 独立 App / 宿主未注册：没有聊天记录搜索数据源。 */
    data object HostOnly : ChatSearchUiState

    /** 搜索成功的结果（顺序由宿主数据源决定，通常是时间倒序）。 */
    data class Results(val messages: List<ChatMessage>) : ChatSearchUiState
}

/** 输入防抖时长：连续输入时只搜索最后一次关键词。 */
private const val SearchDebounceMillis = 300L

/** 单次搜索最多返回的聊天记录条数。 */
private const val SearchResultLimit = 50

/**
 * 查找聊天记录页入口（从聊天信息页「查找聊天记录」进入，`Route.ChatSearch(chatId)`）。
 *
 * 搜索框关键词、防抖搜索与结果状态都在这里维护，再按 [LocalUiMode] 分发两套纯展示实现。
 * 数据来自 [AppDataSources.chatSettingSource]（宿主里非空；独立 App 为 null → 显示仅宿主可用）。
 */
@Composable
fun ChatSearchScreen(chatId: String) {
    val navigator = LocalNavigator.current
    var keyword by rememberSaveable(chatId) { mutableStateOf("") }
    var state by remember(chatId) { mutableStateOf<ChatSearchUiState>(ChatSearchUiState.Idle) }

    LaunchedEffect(chatId, keyword) {
        val query = keyword.trim()
        if (query.isEmpty()) {
            state = ChatSearchUiState.Idle
            return@LaunchedEffect
        }
        // 防抖：关键词继续变化时本协程被取消，只有停手 300ms 后才真正搜索。
        delay(SearchDebounceMillis)
        state = ChatSearchUiState.Loading
        val source = AppDataSources.chatSettingSource
        if (source == null) {
            state = ChatSearchUiState.HostOnly
            return@LaunchedEffect
        }
        // 数据源是阻塞调用，必须放 IO 线程；异常/失败都归到 Failed 状态。
        val raw = withContext(Dispatchers.IO) {
            runCatching { source.searchChatMessages(chatId, query, SearchResultLimit) }.getOrNull()
        }
        // 结果行以 message.id 作为列表 key，宿主返回里若有重复 id 先去重，避免 LazyColumn 崩溃。
        val result = raw?.distinctBy { it.id }
        state = when {
            result == null -> ChatSearchUiState.Failed
            result.isEmpty() -> ChatSearchUiState.Empty
            else -> ChatSearchUiState.Results(result)
        }
    }

    val onBack: () -> Unit = { navigator.pop() }
    val onKeywordChange: (String) -> Unit = { keyword = it }
    // 点击结果：进聊天详情页，用 jumpToSeq 定位到该条消息（定位由 ChatDetailScreen 实现）。
    val keyboardController = LocalSoftwareKeyboardController.current
    val onOpenMessage: (ChatMessage) -> Unit = { message ->
        // 先收起搜索键盘，避免进入聊天页后 IME 还挡着消息列表。
        keyboardController?.hide()
        navigator.push(Route.ChatDetail(chatId = chatId, jumpToSeq = message.seq, jumpToMsgId = 0L))
    }
    when (LocalUiMode.current) {
        UiMode.Miuix -> ChatSearchMiuix(
            keyword = keyword,
            state = state,
            onKeywordChange = onKeywordChange,
            onBack = onBack,
            onOpenMessage = onOpenMessage,
        )
        UiMode.Material -> ChatSearchMaterial(
            keyword = keyword,
            state = state,
            onKeywordChange = onKeywordChange,
            onBack = onBack,
            onOpenMessage = onOpenMessage,
        )
    }
}

/**
 * 结果行的内容摘要：优先显示 content；为空时按消息类型兜底（图片/语音/文件等）。
 * （宿主映射出来的图片消息 content 一般是「[图片]」，这里再兜一层空值保护。）
 */
@Composable
internal fun chatSearchMessagePreview(message: ChatMessage): String {
    val content = message.content.trim()
    if (content.isNotEmpty()) return content
    return when (message.type) {
        ChatMessageType.IMAGE -> stringResource(R.string.chat_image_message)
        ChatMessageType.VIDEO -> "[" + stringResource(R.string.chat_video_title) + "]"
        ChatMessageType.VOICE -> stringResource(R.string.chat_search_voice_message)
        ChatMessageType.FILE -> stringResource(R.string.chat_file_message)
        ChatMessageType.RED_PACKET -> "[" + stringResource(R.string.chat_red_packet) + "]"
        ChatMessageType.LINK -> stringResource(R.string.chat_link_message)
        ChatMessageType.CHAT_RECORD -> "[" + stringResource(R.string.chat_record_title) + "]"
        else -> stringResource(R.string.chat_search_no_content)
    }
}

/** 结果行的发送者名称：空值时自己显示「我」、对方显示「未知发送者」。 */
@Composable
internal fun chatSearchSenderName(message: ChatMessage): String = message.senderName.ifBlank {
    if (message.isMe) stringResource(R.string.chat_sender_me) else stringResource(R.string.chat_search_unknown_sender)
}
