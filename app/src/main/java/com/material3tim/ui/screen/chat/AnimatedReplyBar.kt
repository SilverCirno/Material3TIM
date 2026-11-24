package com.material3tim.ui.screen.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.material3tim.data.model.ChatMessage
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import top.yukonga.miuix.kmp.anim.folmeSpring

/**
 * 输入栏上方的引用横条动画：
 * - 从输入栏顶部向上展开（展开高度 + 由下往上滑入）
 * - 收起时原路向下收缩
 * - 弹性曲线按 UI 模式：Miuix 用 folmeSpring，Material 用 M3E motionScheme
 */
@Composable
internal fun AnimatedReplyBar(
    target: ChatMessage?,
    content: @Composable (ChatMessage) -> Unit,
) {
    // 退出动画期间 target 已置空，需要保留最后一条用于渲染内容
    var lastTarget by remember { mutableStateOf<ChatMessage?>(null) }
    if (target != null) lastTarget = target

    val motionScheme = MaterialTheme.motionScheme
    val sizeSpec: FiniteAnimationSpec<IntSize> = when (LocalUiMode.current) {
        UiMode.Miuix -> folmeSpring<IntSize>(damping = 1f, response = 0.35f)
        UiMode.Material -> motionScheme.fastSpatialSpec<IntSize>()
    }
    val offsetSpec: FiniteAnimationSpec<IntOffset> = when (LocalUiMode.current) {
        UiMode.Miuix -> folmeSpring<IntOffset>(damping = 1f, response = 0.35f)
        UiMode.Material -> motionScheme.fastSpatialSpec<IntOffset>()
    }

    AnimatedVisibility(
        visible = target != null,
        enter = expandVertically(animationSpec = sizeSpec, expandFrom = Alignment.Bottom) +
            slideInVertically(animationSpec = offsetSpec) { fullHeight -> fullHeight } +
            fadeIn(animationSpec = tween(durationMillis = 120)),
        exit = shrinkVertically(animationSpec = sizeSpec, shrinkTowards = Alignment.Bottom) +
            slideOutVertically(animationSpec = offsetSpec) { fullHeight -> fullHeight } +
            fadeOut(animationSpec = tween(durationMillis = 100)),
    ) {
        lastTarget?.let { content(it) }
    }
}
