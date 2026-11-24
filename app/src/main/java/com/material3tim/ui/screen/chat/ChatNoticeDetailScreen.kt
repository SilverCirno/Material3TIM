package com.material3tim.ui.screen.chat

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
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 单条群公告详情页：展示完整公告正文（不截断）。 */
@Composable
fun ChatNoticeDetailScreen(chatId: String, noticeId: String) {
    val navigator = LocalNavigator.current
    var notice by remember(chatId, noticeId) { mutableStateOf<GroupNotice?>(null) }
    var loaded by remember(chatId, noticeId) { mutableStateOf(false) }

    LaunchedEffect(chatId, noticeId) {
        // 兜底逻辑必须保证 loaded 一定置 true，避免异常/无数据时永久停留在加载态。
        notice = runCatching { loadGroupNotice(chatId, noticeId) }.getOrNull()
        loaded = true
    }

    val onBack: () -> Unit = { navigator.pop() }
    when (LocalUiMode.current) {
        UiMode.Miuix -> ChatNoticeDetailMiuix(notice = notice, loaded = loaded, onBack = onBack)
        UiMode.Material -> ChatNoticeDetailMaterial(notice = notice, loaded = loaded, onBack = onBack)
    }
}

/**
 * 读取目标群公告（与群公告列表页 [ChatNoticeScreen] 的数据分支保持一致）：
 * - 宿主(TIM 进程)：优先真实群公告列表；列表为空时用群资料里的公告合成 id="memo" 兜底；
 * - 独立 App(无数据源)：用示例会话 + [MessageRepository.chatInfoFor] 合成群公告，
 *   避免从列表页点进详情后永久空白。
 * 最后按 noticeId 匹配；匹配不到时退回第一条。
 */
private suspend fun loadGroupNotice(chatId: String, noticeId: String): GroupNotice? {
    val source = AppDataSources.chatSource
    val list: List<GroupNotice> = if (source != null) {
        val liveNotices = withContext(Dispatchers.IO) { source.loadGroupNotices(chatId) }.orEmpty()
        if (liveNotices.isNotEmpty()) {
            liveNotices
        } else {
            val memoNotice = withContext(Dispatchers.IO) { source.loadChatInfo(chatId) }
                ?.notice
                .orEmpty()
            if (memoNotice.isNotBlank()) {
                listOf(GroupNotice(id = "memo", title = "群公告", content = memoNotice, timeMillis = 0L))
            } else {
                emptyList()
            }
        }
    } else {
        ChatRepository.chatById(chatId)?.let { chat ->
            val sampleNotice = MessageRepository.chatInfoFor(chat).notice
            if (sampleNotice.isNotBlank()) {
                listOf(GroupNotice(id = "memo", title = "群公告", content = sampleNotice, timeMillis = 0L))
            } else {
                emptyList()
            }
        }.orEmpty()
    }
    return list.firstOrNull { it.id == noticeId } ?: list.firstOrNull()
}
