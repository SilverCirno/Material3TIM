package com.material3tim.ui.screen.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.material3tim.R
import com.material3tim.data.model.ChatItem
import com.material3tim.data.model.ChatPreviewTag
import com.material3tim.data.repository.ChatRepository
import com.material3tim.ui.component.SearchStatus

/** 消息页 UI 状态。 */
@Immutable
data class ChatUiState(
    val isRefreshing: Boolean = false,
    /** 当前可见（已按搜索词过滤）的会话列表。 */
    val chats: List<ChatItem> = ChatRepository.sampleChats,
    /** 置顶会话（同样按搜索词过滤），搜索页的默认内容使用。 */
    val pinnedChats: List<ChatItem> = ChatRepository.sampleChats.filter { it.isPinned },
    val searchStatus: SearchStatus = SearchStatus(label = ""),
)

/** 消息页交互回调，由 [ChatPager] 组装（导航动作不放进 ViewModel）。 */
@Immutable
data class ChatActions(
    val onRefresh: () -> Unit,
    val onSearchTextChange: (String) -> Unit,
    val onSearchStatusChange: (SearchStatus) -> Unit,
    val onClearSearch: () -> Unit,
    val onOpenChat: (String) -> Unit,
    /** 右上角加号菜单：创建群聊 / 加好友群 / 扫一扫。 */
    val onCreateGroup: () -> Unit = {},
    val onAddFriend: () -> Unit = {},
    val onScan: () -> Unit = {},
    /** 会话卡片长按菜单：置顶 / 标为未读 / 删除。 */
    val onSetPinned: (ChatItem, Boolean) -> Unit = { _, _ -> },
    val onSetUnread: (ChatItem, Boolean) -> Unit = { _, _ -> },
    val onDeleteChat: (ChatItem) -> Unit = {},
    /** 首页会话列表滑动状态变化（滑动中暂存 LiveChats 推送，停止后补刷；避免滑动掉帧）。 */
    val onListScrollStateChange: (Boolean) -> Unit = {},
)

/** 未读数展示文本，超过 99 显示 99+。 */
internal fun unreadBadgeText(count: Int): String = if (count > 99) "99+" else count.toString()

/** 列表摘要：群聊在最后一条消息前附带成员数，草稿前缀由数据层 [ChatItem.displaySummary] 提供。 */
@Composable
internal fun chatSummary(chat: ChatItem): String {
    return if (chat.isGroup && chat.memberCount > 0) {
        stringResource(R.string.chat_member_count, chat.memberCount) + " · " + chat.displaySummary
    } else {
        chat.displaySummary
    }
}

/** 提醒标签文案（TIM 风格的方括号文本，如「[有人@我]」）。 */
@Composable
internal fun chatPreviewTagLabel(tag: ChatPreviewTag): String = stringResource(
    when (tag) {
        ChatPreviewTag.AtMe -> R.string.chat_tag_at_me
        ChatPreviewTag.AtAll -> R.string.chat_tag_at_all
        ChatPreviewTag.NewFile -> R.string.chat_tag_new_file
        ChatPreviewTag.NewHomework -> R.string.chat_tag_new_homework
        ChatPreviewTag.GroupNotice -> R.string.chat_tag_group_notice
        ChatPreviewTag.MyRedPacket -> R.string.chat_tag_my_red_packet
        ChatPreviewTag.MyGift -> R.string.chat_tag_my_gift
        ChatPreviewTag.QQRedPacket -> R.string.chat_tag_qq_red_packet
        ChatPreviewTag.ReplyMe -> R.string.chat_tag_reply_me
        ChatPreviewTag.SpecialCare -> R.string.chat_tag_special_care
    },
)

/**
 * 带红色提醒标签前缀的会话摘要（AnnotatedString）：
 * - 有草稿时只显示 [chatSummary]（即数据层的「[草稿] …」），不显示提醒标签；
 * - 否则把「[标签] 」放在整段摘要最前面（群成员数前缀等原有逻辑保留在其后），标签用 [tagColor] 着色。
 */
@Composable
internal fun chatSummaryAnnotated(chat: ChatItem, tagColor: Color): AnnotatedString {
    val summary = chatSummary(chat)
    val tag = chat.previewTag
    if (tag == null || chat.draft != null) return AnnotatedString(summary)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = tagColor)) {
            append(chatPreviewTagLabel(tag))
        }
        append(' ')
        append(summary)
    }
}
