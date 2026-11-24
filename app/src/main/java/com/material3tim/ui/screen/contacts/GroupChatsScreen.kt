package com.material3tim.ui.screen.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.material3tim.data.repository.ChatRepository
import com.material3tim.data.repository.LiveChats
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 群聊页入口：按 [LocalUiMode] 分发到 Material / Miuix 两套实现。
 * 点击群聊进入对应聊天详情。
 *
 * 数据分支：
 * - 宿主(TIM 进程)：订阅 [LiveChats.liveFlow]，从真实会话里过滤群聊；
 * - 独立 App(无数据源)：回落 [ChatRepository.groupChats] 的示例群聊。
 */
@Composable
fun GroupChatsScreen() {
    val liveChats by LiveChats.liveFlow.collectAsStateWithLifecycle()
    val sampleGroupChats = remember { ChatRepository.groupChats() }

    // 宿主里 LiveChats 尚未刷新时主动拉一次，避免从联系人页进入群聊页时短暂空白。
    LaunchedEffect(Unit) {
        if (AppDataSources.hasLiveDataSource && LiveChats.peek() == null) {
            withContext(Dispatchers.IO) { LiveChats.refresh() }
        }
    }

    val groupChats = if (AppDataSources.hasLiveDataSource) {
        liveChats.orEmpty().filter { it.isGroup }
    } else {
        sampleGroupChats
    }

    when (LocalUiMode.current) {
        UiMode.Material -> GroupChatsMaterial(groupChats = groupChats)
        UiMode.Miuix -> GroupChatsMiuix(groupChats = groupChats)
    }
}
