package com.material3tim.ui.screen.chat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.material3tim.R
import com.material3tim.ui.component.material.ExpressiveConfirmDialog
import com.material3tim.data.model.ChatItem
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.component.material.OffsetAnchoredExpressiveMenu
import com.material3tim.ui.component.miuix.glass.GlassConfirmDialog
import com.material3tim.ui.component.miuix.glass.GlassMenuEntry
import com.material3tim.ui.component.miuix.glass.LocalGlassMenu
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete as MiuixDeleteIcon
import top.yukonga.miuix.kmp.icon.extended.Email
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Pin
import top.yukonga.miuix.kmp.icon.extended.Unpin
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.dp

/** 菜单里「删除」的红色（与 MessageActionMenu 的 DELETE 红一致）。 */
internal val ChatCardDeleteRed: Color = Color(0xFFE5484D)

/** 会话卡片长按菜单动作。 */
private enum class ChatCardAction {
    Pin,
    Unpin,
    MarkRead,
    MarkUnread,
    AskDelete,
}

/** 一条菜单项（图标/文案/是否删除/点击动作）。 */
private data class ChatCardMenuEntry(
    val label: String,
    val icon: ImageVector,
    val isDelete: Boolean,
    val onClick: () -> Unit,
)

/**
 * 会话卡片长按菜单宿主：给卡片套一层长按检测（约 500ms，长按后吞掉后续事件避免触发打开聊天），
 * 菜单锚点取按下的位置；两套 UI 菜单结构参照 [BubbleActionMenuHost]（消息气泡长按菜单）。
 * 删除项点击后弹确认对话框，确认才真正删除。
 */
@Composable
internal fun ChatCardActionMenuHost(
    chat: ChatItem,
    actions: ChatActions,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var show by remember(chat.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember(chat.id) { mutableStateOf(false) }
    var pressOffset by remember(chat.id) { mutableStateOf(IntOffset.Zero) }
    // 长按点（相对卡片左上角）：1dp 透明锚点 Box 放在这里，作为 morph 展开/收缩的中心。
    var pressPosition by remember(chat.id) { mutableStateOf(Offset.Zero) }
    // 1x1 morph 锚点：GlassTransformPopup 从手指长按点展开，预测性返回跟手收缩回该点。
    val menuAnchor = rememberGlassPopupAnchor()
    val haptic = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val onAction: (ChatCardAction) -> Unit = { action ->
        show = false
        when (action) {
            ChatCardAction.Pin -> actions.onSetPinned(chat, true)
            ChatCardAction.Unpin -> actions.onSetPinned(chat, false)
            ChatCardAction.MarkRead -> actions.onSetUnread(chat, false)
            ChatCardAction.MarkUnread -> actions.onSetUnread(chat, true)
            ChatCardAction.AskDelete -> showDeleteConfirm = true
        }
    }
    Box(
        modifier = modifier
            .pointerInput(chat.id) {
                // 与消息气泡长按一致：Initial pass 检测长按，避免卡片的 onClick 先消费事件。
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val releasedBeforeLongPress = withTimeoutOrNull(
                        viewConfiguration.longPressTimeoutMillis,
                    ) {
                        var finished = false
                        while (!finished) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                finished = true
                            } else if (
                                (change.position - down.position).getDistance() >
                                viewConfiguration.touchSlop
                            ) {
                                finished = true
                            }
                        }
                        true
                    } != null
                    if (!releasedBeforeLongPress) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        pressPosition = down.position
                        pressOffset = IntOffset(
                            down.position.x.roundToInt(),
                            down.position.y.roundToInt(),
                        )
                        show = true
                        // 长按已触发：消费后续事件，避免松手时触发卡片的 openChat。
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                }
            },
    ) {
        // 1dp 透明锚点：放在手指长按位置（窗口矩形≈1x1），GlassTransformPopup 从该点展开/收缩；
        // cornerRadius 与展开面板一致（24dp，start==end）→ 不会出现负圆角。
        Box(
            modifier = Modifier
                .offset { IntOffset(pressPosition.x.roundToInt(), pressPosition.y.roundToInt()) }
                .size(1.dp)
                .glassPopupAnchor(anchor = menuAnchor, cornerRadius = 24.dp, floating = true),
        )
        content()
        when (LocalUiMode.current) {
            UiMode.Miuix -> MiuixChatCardMenu(
                chat = chat,
                show = show,
                anchor = menuAnchor,
                onDismiss = { show = false },
                onAction = onAction,
            )

            UiMode.Material -> MaterialChatCardMenu(
                chat = chat,
                show = show,
                pressOffset = pressOffset,
                onDismiss = { show = false },
                onAction = onAction,
            )
        }
    }
    when (LocalUiMode.current) {
        // Miuix：玻璃确认对话框必须常驻组合（用 show 控制显隐），离场动画/预测性返回才完整。
        UiMode.Miuix -> MiuixDeleteChatDialog(
            show = showDeleteConfirm,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                actions.onDeleteChat(chat)
            },
        )

        // Material：ExpressiveConfirmDialog 走自绘遮罩，保持原有 if(show) 组合方式。
        UiMode.Material -> if (showDeleteConfirm) {
            MaterialDeleteChatDialog(
                onDismiss = { showDeleteConfirm = false },
                onConfirm = {
                    showDeleteConfirm = false
                    actions.onDeleteChat(chat)
                },
            )
        }
    }
}

/** 动态菜单项：按 isPinned / unreadCount 切换「置顶/取消置顶」「标为未读/已读」。 */
@Composable
private fun chatCardMenuEntries(
    chat: ChatItem,
    pinIcon: ImageVector,
    unpinIcon: ImageVector,
    markUnreadIcon: ImageVector,
    markReadIcon: ImageVector,
    deleteIcon: ImageVector,
    onAction: (ChatCardAction) -> Unit,
): List<ChatCardMenuEntry> {
    val pinned = chat.isPinned
    val unread = chat.unreadCount > 0
    return listOf(
        ChatCardMenuEntry(
            label = stringResource(if (pinned) R.string.chat_card_unpin else R.string.chat_card_pin),
            icon = if (pinned) unpinIcon else pinIcon,
            isDelete = false,
            onClick = { onAction(if (pinned) ChatCardAction.Unpin else ChatCardAction.Pin) },
        ),
        ChatCardMenuEntry(
            label = stringResource(if (unread) R.string.chat_card_mark_read else R.string.chat_card_mark_unread),
            icon = if (unread) markReadIcon else markUnreadIcon,
            isDelete = false,
            onClick = { onAction(if (unread) ChatCardAction.MarkRead else ChatCardAction.MarkUnread) },
        ),
        ChatCardMenuEntry(
            label = stringResource(R.string.chat_card_delete),
            icon = deleteIcon,
            isDelete = true,
            onClick = { onAction(ChatCardAction.AskDelete) },
        ),
    )
}

/** Material：按下位置弹 Expressive 下拉（与气泡长按菜单一致），删除项红色文字/图标。 */
@Composable
private fun MaterialChatCardMenu(
    chat: ChatItem,
    show: Boolean,
    pressOffset: IntOffset,
    onDismiss: () -> Unit,
    onAction: (ChatCardAction) -> Unit,
) {
    OffsetAnchoredExpressiveMenu(
        expanded = show,
        onDismissRequest = onDismiss,
        anchorOffset = pressOffset,
    ) {
        val entries = chatCardMenuEntries(
            chat = chat,
            pinIcon = Icons.Filled.PushPin,
            unpinIcon = Icons.Filled.PushPin,
            markUnreadIcon = Icons.Filled.MarkChatUnread,
            markReadIcon = Icons.Filled.MarkChatRead,
            deleteIcon = Icons.Filled.Delete,
            onAction = onAction,
        )
        entries.forEachIndexed { index, entry ->
            SelectableDropdownMenuItem(
                text = {
                    Text(
                        text = entry.label,
                        color = if (entry.isDelete) {
                            ChatCardDeleteRed
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                },
                selected = false,
                leadingIcon = {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        tint = if (entry.isDelete) {
                            ChatCardDeleteRed
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                    )
                },
                shapes = MenuDefaults.itemShape(index = index, count = entries.size),
                onClick = { entry.onClick() },
            )
        }
    }
}

/**
 * Miuix：锚点 = 会话卡片内 1dp 透明 Box（跟随手指长按点），`GlassTransformPopup`
 * 从手指点展开、预测性返回跟手收缩；删除项红色。
 */
@Composable
private fun MiuixChatCardMenu(
    chat: ChatItem,
    show: Boolean,
    anchor: GlassPopupAnchor,
    onDismiss: () -> Unit,
    onAction: (ChatCardAction) -> Unit,
) {
    val glassMenu = LocalGlassMenu.current
    // 页面采样只能在页面 CompositionLocal 作用域内读，这里读到后交给根级状态保存。
    val pageBackdrop = LocalPageBackdrop.current
    // 菜单发出方 token：会话卡片被移出组合时关闭残留菜单。
    val menuOwner = remember { Any() }
    DisposableEffect(menuOwner) {
        onDispose { glassMenu?.hideIfOwner(menuOwner) }
    }
    val entries = chatCardMenuEntries(
        chat = chat,
        pinIcon = MiuixIcons.Pin,
        unpinIcon = MiuixIcons.Unpin,
        markUnreadIcon = MiuixIcons.Email,
        markReadIcon = MiuixIcons.Ok,
        deleteIcon = MiuixIcons.MiuixDeleteIcon,
        onAction = onAction,
    )
    // 长按后 show 变 true：先等一帧，让 1dp 锚点 Box 移动到本次长按点并完成测量上报，
    // 再交给页面级宿主用 GlassTransformPopup 从手指点展开（预测性返回跟手收缩回手指点）。
    LaunchedEffect(show) {
        if (!show) return@LaunchedEffect
        withFrameNanos { }
        glassMenu?.show(
            owner = menuOwner,
            anchor = anchor,
            entries = entries.map { entry ->
                GlassMenuEntry(
                    label = entry.label,
                    icon = entry.icon,
                    isDanger = entry.isDelete,
                    onClick = entry.onClick,
                )
            },
            backdrop = pageBackdrop,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun MaterialDeleteChatDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    // 删除会话确认：Expressive 危险确认弹窗。
    ExpressiveConfirmDialog(
        show = true,
        title = stringResource(R.string.chat_card_delete_title),
        message = stringResource(R.string.chat_card_delete_message),
        confirmText = stringResource(R.string.chat_card_delete),
        danger = true,
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun MiuixDeleteChatDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    // 删除会话确认：HyperOS4 玻璃危险确认对话框（页面已提供 LocalPageBackdrop → 玻璃；否则不透明回退）。
    GlassConfirmDialog(
        show = show,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.chat_card_delete_title),
        summary = stringResource(R.string.chat_card_delete_message),
        confirmText = stringResource(R.string.chat_card_delete),
        cancelText = stringResource(R.string.cancel),
        destructive = true,
        onConfirm = onConfirm,
    )
}
