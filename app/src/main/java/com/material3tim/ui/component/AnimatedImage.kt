package com.material3tim.ui.component

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 动图（GIF / 动态 WebP / APNG）支持：
 * - API 28+ 用 `ImageDecoder` 解出 [AnimatedImageDrawable]（无限循环、由 ImageView 在挂载时自动播放）；
 * - 静态图 / 低版本 / 解码失败回退到原来的 [rememberLocalImage] + Compose Image；
 * - 气泡与图片预览页共用，保证「聊天页气泡」和「点开预览」都在循环播放、不中断。
 */

private sealed interface AnimatedImageState {
    data object Loading : AnimatedImageState
    data class Animated(val drawable: AnimatedImageDrawable) : AnimatedImageState
    data object Static : AnimatedImageState
}

@Composable
private fun rememberAnimatedImageState(path: String?, maxDimension: Int): AnimatedImageState {
    val canDecodeAnimated = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    if (path.isNullOrBlank() || !canDecodeAnimated) return AnimatedImageState.Static
    var state by remember(path, maxDimension) { mutableStateOf<AnimatedImageState>(AnimatedImageState.Loading) }
    LaunchedEffect(path, maxDimension) {
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                val source = android.graphics.ImageDecoder.createSource(File(path))
                android.graphics.ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                    // 动图同样限制解码尺寸：GIF/APNG 原始分辨率可能极大，逐帧上传会拖垮低端机。
                    val width = info.size.width
                    val height = info.size.height
                    if (maxDimension > 0 && (width > maxDimension || height > maxDimension)) {
                        var sample = 1
                        while (width / sample > maxDimension || height / sample > maxDimension) {
                            sample *= 2
                        }
                        decoder.setTargetSize(
                            (width / sample).coerceAtLeast(1),
                            (height / sample).coerceAtLeast(1),
                        )
                    }
                }
            }.getOrNull() as? AnimatedImageDrawable
        }
        decoded?.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        decoded?.start()
        state = if (decoded != null) {
            AnimatedImageState.Animated(decoded)
        } else {
            AnimatedImageState.Static
        }
    }
    DisposableEffect(state) {
        val animated = (state as? AnimatedImageState.Animated)?.drawable
        onDispose {
            animated?.stop()
        }
    }
    return state
}

/**
 * 优先按动图渲染本地图片；静态图自动回退到 [rememberLocalImage]。
 * [contentScale] 用 Compose 的 ContentScale；ImageView 会自动映射 FIT/CROP。
 *
 * [animate] = false 时强制静态渲染（如网格滚动中暂停多个动图同时播放，降低解码/绘制压力）。
 * [loadEnabled] = false 时只显示已缓存位图/占位，不启动新解码（滚动中延迟新图加载）。
 */
@Composable
fun AnimatedOrStaticImage(
    path: String?,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    loadEnabled: Boolean = true,
    maxDimension: Int = 960,
) {
    if (path.isNullOrBlank()) return
    if (!animate || !loadEnabled) {
        // 暂停/滚动中：只读缓存不解码；未缓存显示占位，滚动停止后调用方切回 true。
        val bitmap = rememberCachedLocalImage(path, maxDimension)
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier,
            )
        } else {
            Box(modifier)
        }
        return
    }
    when (val state = rememberAnimatedImageState(path, maxDimension)) {
        is AnimatedImageState.Loading -> {
            // 解码中优先显示缓存里的静态位图：避免滚动停止/动图解码时短暂白屏闪烁。
            val cached = rememberCachedLocalImage(path, maxDimension)
            if (cached != null) {
                Image(
                    bitmap = cached,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = modifier,
                )
            } else {
                Box(modifier)
            }
        }
        is AnimatedImageState.Animated -> {
            val drawable = state.drawable
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = contentScale.toImageViewScaleType()
                        setImageDrawable(drawable)
                    }
                },
                modifier = modifier,
                update = { view ->
                    view.scaleType = contentScale.toImageViewScaleType()
                    if (view.drawable !== drawable) view.setImageDrawable(drawable)
                    if (!drawable.isRunning) drawable.start()
                },
                onRelease = { view ->
                    view.setImageDrawable(null)
                    drawable.stop()
                },
            )
        }
        AnimatedImageState.Static -> {
            val bitmap = rememberLocalImage(path, maxDimension)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = modifier,
                )
            } else {
                Box(modifier)
            }
        }
    }
}

private fun ContentScale.toImageViewScaleType(): ImageView.ScaleType = when (this) {
    ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
    ContentScale.FillBounds -> ImageView.ScaleType.FIT_XY
    ContentScale.Inside -> ImageView.ScaleType.CENTER_INSIDE
    else -> ImageView.ScaleType.FIT_CENTER
}

@Suppress("unused")
private fun Drawable.isAnimated(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && this is AnimatedImageDrawable
