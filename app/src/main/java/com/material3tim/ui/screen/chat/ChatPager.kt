package com.material3tim.ui.screen.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material3tim.R
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.component.SearchStatus
import com.material3tim.ui.navigation3.Navigator
import com.material3tim.ui.navigation3.Route

/**
 * 消息页入口：持有 [ChatViewModel]，按 [LocalUiMode] 分发到 Material / Miuix 两套列表形式。
 * 列表视觉参考 KernelSU Manager 的超级用户列表。
 */
@Composable
fun ChatPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val viewModel = viewModel<ChatViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestUiState by rememberUpdatedState(uiState)
    val context = LocalContext.current
    // 会话卡片菜单动作失败（置顶/标未读/删除）统一 Toast 提示。
    val onChatActionFailure: () -> Unit = {
        Toast.makeText(context, R.string.chat_card_action_failed, Toast.LENGTH_SHORT).show()
    }

    // 切换到其他页时收起 Miuix 搜索浮层，避免滑回来还是展开状态。
    LaunchedEffect(isCurrentPage) {
        val status = latestUiState.searchStatus
        if (!isCurrentPage && !status.isCollapsed()) {
            viewModel.updateSearchStatus(
                status.copy(searchText = "", current = SearchStatus.Status.COLLAPSED)
            )
        }
    }

    val actions = ChatActions(
        onRefresh = viewModel::onRefresh,
        onSearchTextChange = viewModel::updateSearchText,
        onSearchStatusChange = viewModel::updateSearchStatus,
        onClearSearch = viewModel::clearSearch,
        onOpenChat = { chatId -> navigator.push(Route.ChatDetail(chatId)) },
        onCreateGroup = { navigator.push(Route.CreateGroup) },
        onAddFriend = { navigator.push(Route.AddFriend) },
        onScan = { navigator.push(Route.ScanQr) },
        onSetPinned = { chat, pinned -> viewModel.setPinned(chat, pinned, onChatActionFailure) },
        onSetUnread = { chat, unread -> viewModel.setUnread(chat, unread, onChatActionFailure) },
        onDeleteChat = { chat -> viewModel.deleteChat(chat, onChatActionFailure) },
        // 列表滑动状态：滑动中暂停应用 LiveChats 推送，停止后一次性补刷（不冻结动图/图片加载）。
        onListScrollStateChange = viewModel::setListScrollInProgress,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> ChatPagerMiuix(
            uiState = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )

        UiMode.Material -> ChatPagerMaterial(
            uiState = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )
    }
}
