package com.material3tim.ui.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults as MiuixProgressIndicatorDefaults

/**
 * 消息气泡里的进度条组件（文件下载 / 语音播放共用）。
 *
 * Material 3 Expressive 用波浪进度条（[LinearWavyProgressIndicator]），Miuix 用标准直线进度条；
 * `progress == null` 表示不确定态（下载/播放进度未知）。
 * 语音气泡额外提供可拖动 seek 版本（[DraggableWavyProgressBar] / [DraggableMiuixProgressBar]）。
 */

/** Material3 Expressive 波浪进度条：null → 不确定态；[amplitude] 控制波浪幅度（越大越"飘"）。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WavyProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
    amplitude: Dp = 4.dp,
) {
    val density = LocalDensity.current
    val amplitudePx = with(density) { amplitude.toPx() }
    if (progress == null) {
        LinearWavyProgressIndicator(
            modifier = modifier,
            color = color,
            trackColor = trackColor,
            amplitude = amplitudePx,
        )
    } else {
        LinearWavyProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier,
            color = color,
            trackColor = trackColor,
            amplitude = { amplitudePx },
        )
    }
}

/** Miuix 标准进度条：null → 不确定态（Miuix 不跟随 M3E 大波浪，保持 4dp 标准样式）。 */
@Composable
fun MiuixProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
) {
    MiuixLinearProgressIndicator(
        modifier = modifier,
        progress = progress?.coerceIn(0f, 1f),
        colors = MiuixProgressIndicatorDefaults.progressIndicatorColors(
            foregroundColor = color,
            backgroundColor = trackColor,
        ),
        height = 4.dp,
    )
}

/**
 * 可拖动 seek 的 Material3 波浪进度条。
 *
 * - [modifier] 决定命中区域（调用方给约 24dp 高，方便手指拖动），进度条视觉高度由 [barHeight] 决定、垂直居中；
 * - 拖动中 [progress] 变化不生效（本地 [dragFraction] 优先），松手回调 [onSeekFinished] 并恢复外部 progress；
 * - 未拖动时显示外部 [progress]。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DraggableWavyProgressBar(
    progress: Float,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
    amplitude: Dp = 4.dp,
    barHeight: Dp = 10.dp,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var barWidthPx by remember { mutableIntStateOf(0) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        WavyProgressBar(
            progress = dragFraction ?: progress.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight),
            color = color,
            trackColor = trackColor,
            amplitude = amplitude,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .onSizeChanged { barWidthPx = it.width }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragFraction = fractionAt(offset.x, barWidthPx)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragFraction = fractionAt(change.position.x, barWidthPx)
                        },
                        onDragEnd = {
                            val fraction = dragFraction
                            dragFraction = null
                            if (fraction != null) onSeekFinished(fraction)
                        },
                        onDragCancel = { dragFraction = null },
                    )
                },
        )
    }
}

/** 可拖动 seek 的 Miuix 标准进度条（结构与 [DraggableWavyProgressBar] 一致）。 */
@Composable
fun DraggableMiuixProgressBar(
    progress: Float,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var barWidthPx by remember { mutableIntStateOf(0) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        MiuixProgressBar(
            progress = dragFraction ?: progress.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = color,
            trackColor = trackColor,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .onSizeChanged { barWidthPx = it.width }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragFraction = fractionAt(offset.x, barWidthPx)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragFraction = fractionAt(change.position.x, barWidthPx)
                        },
                        onDragEnd = {
                            val fraction = dragFraction
                            dragFraction = null
                            if (fraction != null) onSeekFinished(fraction)
                        },
                        onDragCancel = { dragFraction = null },
                    )
                },
        )
    }
}

/** 手指 x 坐标（相对进度条）→ 0..1 的比例。 */
private fun fractionAt(x: Float, widthPx: Int): Float =
    if (widthPx <= 0) 0f else (x / widthPx).coerceIn(0f, 1f)
