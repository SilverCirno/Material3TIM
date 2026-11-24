package com.material3tim.ui.screen.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import top.yukonga.miuix.kmp.anim.folmeSpring

/**
 * 左滑引用阈值常量（与 Miuix 交互原则对齐）：
 * - 触发距离：max(112.dp, 行宽 35%)（Miuix 无现成横向 swipe 组件，参考其下拉刷新「需明显位移 + 意图判定」）；
 * - 方向锁定：过 touchSlop 时 |dy| >= |dx| 判为纵向滚动、不消费；右滑判为返回手势、不消费；
 * - 超出最大位移后按 0.35 系数阻尼（参考 Miuix SpringMath.obtainDampingDistance 的 overscroll 阻尼思路）。
 */
private val MinSwipeThreshold = 112.dp

/** 行宽比例阈值：行宽 35%（窄消息行更远，宽行需要更明显动作）。 */
private const val SwipeThresholdWidthRatio = 0.35f

/** 最大跟手位移 = 阈值 + 该额外距离，超出部分按阻尼系数衰减。 */
private val MaxSwipeExtra = 56.dp

/** 超出最大位移后的阻尼系数（0..1，越小越"重"）。 */
private const val OverdragResistance = 0.35f

/** 回复提示开始出现的进度（相对阈值），避免刚过 slop 就预显示。 */
private const val HintAppearProgress = 0.35f

/** 回弹曲线：Miuix 用自家 folmeSpring，Material 用 M3E MotionScheme 的 fast spatial spring。 */
@Composable
private fun rememberSwipeSettleSpec(): FiniteAnimationSpec<Float> {
    val motionScheme = MaterialTheme.motionScheme
    return when (LocalUiMode.current) {
        UiMode.Miuix -> folmeSpring<Float>(damping = 1f, response = 0.35f)
        UiMode.Material -> motionScheme.fastSpatialSpec<Float>()
    }
}

/**
 * [offset] 为负值（左移），超过 -[maxSwipePx] 后按阻尼衰减，避免硬边界。
 */
private fun dampedSwipeOffset(offset: Float, maxSwipePx: Float): Float {
    if (offset >= 0f) return 0f
    val max = -maxSwipePx
    if (offset >= max) return offset
    return max + (offset - max) * OverdragResistance
}

/**
 * 消息行左滑引用（Miuix 风格严格手感）：
 * - 拖动时整行（气泡 + 头像 + 昵称 + 时间）跟随手指左移，超出最大位移后带阻尼；
 * - 回复箭头提示随位移渐进出现（拖到接近阈值才完全显示）；
 * - 方向锁定：斜滑/纵向滚动不消费事件，右滑返回也完全不消费；
 * - 松手只看位移是否达到触发距离（与速度无关）：达标触发引用，否则回弹。
 */
@Composable
internal fun SwipeToReplyRow(
    messageId: String,
    onReply: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val minThresholdPx = with(density) { MinSwipeThreshold.toPx() }
    val maxSwipeExtraPx = with(density) { MaxSwipeExtra.toPx() }
    var rowWidthPx by remember(messageId) { mutableStateOf(0f) }
    // 触发距离：max(112dp, 行宽 * 35%)
    val thresholdPx = max(minThresholdPx, rowWidthPx * SwipeThresholdWidthRatio)
    val maxSwipePx = thresholdPx + maxSwipeExtraPx
    val offsetX = remember(messageId) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val settleSpec = rememberSwipeSettleSpec()

    Box(modifier = Modifier.fillMaxWidth()) {
        // 回复提示：默认不显示，随左移进度渐显/放大，松手回弹时淡出。
        val dragProgress = (-offsetX.value / thresholdPx).coerceIn(0f, 1f)
        val hintProgress = ((dragProgress - HintAppearProgress) / (1f - HintAppearProgress)).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .size(24.dp)
                .graphicsLayer {
                    alpha = hintProgress
                    scaleX = 0.72f + 0.28f * hintProgress
                    scaleY = 0.72f + 0.28f * hintProgress
                },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowWidthPx = it.width.toFloat() }
                .graphicsLayer { translationX = offsetX.value }
                .pointerInput(messageId, enabled) {
                    // 多选模式下禁用左滑引用，避免与「点击整行切换选中」互相抢手势。
                    if (!enabled) return@pointerInput
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        // 每次手势开始时读取行宽（onSizeChanged 后更新），按 max(112dp, 行宽*35%) 计算阈值。
                        val width = rowWidthPx
                        val threshold = max(minThresholdPx, width * SwipeThresholdWidthRatio)
                        val maxSwipe = threshold + maxSwipeExtraPx
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // 0 = 未锁定；-1 = 左滑引用（消费）；1 = 纵向滚动；2 = 右滑返回（不消费）
                        var axisLock = 0
                        var dragging = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val dx = change.position.x - down.position.x
                            val dy = change.position.y - down.position.y
                            if (!dragging) {
                                if (abs(dx) <= slop && abs(dy) <= slop) {
                                    if (!change.pressed) break
                                    continue
                                }
                                // 方向锁定：斜滑/竖向优先判为列表滚动；从起点就向右判为返回手势。
                                if (abs(dy) >= abs(dx)) {
                                    axisLock = 1
                                    break
                                }
                                if (dx > 0f) {
                                    axisLock = 2
                                    break
                                }
                                dragging = true
                                axisLock = -1
                                change.consume()
                                val initial = dampedSwipeOffset(dx, maxSwipe)
                                scope.launch { offsetX.snapTo(initial) }
                                if (!change.pressed) break
                                continue
                            }
                            if (!change.pressed) break
                            // 方向锁定后只消费左滑手势，右滑回拉不再抢返回手势。
                            val dragAmount = change.positionChange().x
                            change.consume()
                            val next = dampedSwipeOffset(offsetX.value + dragAmount, maxSwipe)
                            scope.launch { offsetX.snapTo(next) }
                        }
                        if (dragging && axisLock == -1) {
                            // 松手只看位移是否达到触发距离（与速度无关）。
                            val triggered = offsetX.value <= -threshold
                            scope.launch { offsetX.animateTo(targetValue = 0f, animationSpec = settleSpec) }
                            if (triggered) onReply()
                        } else {
                            scope.launch { offsetX.animateTo(targetValue = 0f, animationSpec = settleSpec) }
                        }
                    }
                },
        ) {
            content()
        }
    }
}
