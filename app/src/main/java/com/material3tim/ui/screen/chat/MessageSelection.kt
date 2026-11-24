package com.material3tim.ui.screen.chat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.material3tim.data.model.ChatMessage

/** 聊天页多选状态（通过 CompositionLocal 传给消息行）。 */
internal class MessageSelectionState(
    val active: Boolean,
    val selectedIds: Set<Long>,
    val onToggle: (ChatMessage) -> Unit,
)

internal val LocalMessageSelection =
    compositionLocalOf<MessageSelectionState?> { null }

/**
 * 多选模式下的整行点击拦截：
 * - 点击消息行（气泡 / 图片 / 视频 / 链接 / 聊天记录 / 头像 / 时间 / checkbox 等任意位置）
 *   都切换该消息的选中状态；
 * - 抬起事件在 Initial pass 消费，避免气泡里的图片/视频/链接/聊天记录等子组件
 *   把这次点击当成自己的点击（弹出预览、打开链接等）；
 * - 拖动超过 touchSlop 时不消费、也不切换选中，保证列表仍可正常滚动；
 * - 长按（超过系统 longPressTimeout）不当作点击，避免误触选择。
 */
internal fun Modifier.selectMessageRow(
    active: Boolean,
    messageId: String,
    onToggle: () -> Unit,
): Modifier = if (!active) {
    this
} else {
    this.pointerInput(messageId) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val downAt = android.os.SystemClock.uptimeMillis()
            var canceled = false
            var up = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    up = true
                    break
                }
                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                    canceled = true
                    break
                }
            }
            if (!canceled && up) {
                val isLongPress = android.os.SystemClock.uptimeMillis() - downAt >
                    viewConfiguration.longPressTimeoutMillis
                if (!isLongPress) {
                    // 消费抬起事件，阻止子组件（图片/视频/链接/聊天记录等）触发自身点击。
                    currentEvent?.changes?.firstOrNull { it.id == down.id }?.consume()
                    onToggle()
                }
            }
        }
    }
}
