package com.material3tim.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 聊天气泡 / 联系人通用圆形头像。
 *
 * 以主题色（Material 或 Miuix，取决于当前 [LocalUiMode]）生成渐变背景，
 * 中间显示名称首字，可选在线绿点或群聊角标。
 */
@Composable
fun ChatAvatar(
    name: String,
    seed: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    isGroup: Boolean = false,
    avatarPath: String? = null,
) {
    // 按显示尺寸解码：列表 40~48dp 只需 ~144px，避免小头像全按 256px 解码占内存/带宽。
    val avatarMaxDimension = (size.value * 3).toInt().coerceIn(96, 256)
    val realAvatar = rememberLocalImage(avatarPath, maxDimension = avatarMaxDimension)
    val uiMode = LocalUiMode.current
    val gradients = rememberAvatarGradients()
    val gradient = gradients[seed.coerceAtLeast(0) % gradients.size]

    val surfaceColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surface
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
    }
    val markerBackground = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.secondaryContainer
        UiMode.Miuix -> MiuixTheme.colorScheme.secondaryContainer
    }
    val markerContent = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.onSecondaryContainer
        UiMode.Miuix -> MiuixTheme.colorScheme.onSecondaryContainer
    }

    val density = LocalDensity.current
    val initial = remember(name) {
        name.trim().take(1).ifEmpty { "?" }.uppercase()
    }
    val initialFontSize = with(density) { (size * 0.4f).toSp() }
    val markerSize = (size * 0.25f).coerceAtLeast(9.dp)
    val markerBorderWidth = (size * 0.035f).coerceAtLeast(1.dp)

    Box(modifier = modifier.size(size)) {
        if (realAvatar != null) {
            Image(
                bitmap = realAvatar,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(gradient.start, gradient.end))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    color = gradient.content,
                    fontSize = initialFontSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }

        if (isGroup) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(markerSize)
                    .clip(CircleShape)
                    .background(markerBackground)
                    .border(markerBorderWidth, surfaceColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = markerContent,
                    modifier = Modifier.fillMaxSize(0.62f),
                )
            }
        } else if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(markerSize)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(markerBorderWidth, surfaceColor, CircleShape),
            )
        }
    }
}

private val OnlineGreen = Color(0xFF4CAF50)

private data class AvatarGradient(
    val start: Color,
    val end: Color,
    val content: Color,
)

/**
 * 根据渐变两端颜色的平均亮度选一个对比度足够的首字颜色。
 * 不同主题（尤其是 Miuix 默认调色板）的 container / on-container 配对不一定适用于
 * “两个不同 container 混合成渐变”的场景，因此这里用亮度判断兜底。
 */
private fun contentColorForGradient(start: Color, end: Color): Color {
    val startLuminance = start.luminance()
    val endLuminance = end.luminance()
    val average = (startLuminance + endLuminance) / 2f
    return if (average > 0.45f) Color(0xFF1B1B1F) else Color.White
}

@Composable
private fun rememberAvatarGradients(): List<AvatarGradient> {
    fun build(primaryContainer: Color, tertiaryContainer: Color, secondaryContainer: Color): List<AvatarGradient> {
        val pairs = listOf(
            primaryContainer to tertiaryContainer,
            tertiaryContainer to secondaryContainer,
            secondaryContainer to primaryContainer,
            primaryContainer to secondaryContainer,
        )
        return pairs.map { (start, end) ->
            AvatarGradient(start, end, contentColorForGradient(start, end))
        }
    }

    return when (LocalUiMode.current) {
        UiMode.Material -> {
            val colors = MaterialTheme.colorScheme
            remember(colors.primaryContainer, colors.tertiaryContainer, colors.secondaryContainer) {
                build(colors.primaryContainer, colors.tertiaryContainer, colors.secondaryContainer)
            }
        }

        UiMode.Miuix -> {
            val colors = MiuixTheme.colorScheme
            remember(colors.primaryContainer, colors.tertiaryContainer, colors.secondaryContainer) {
                build(colors.primaryContainer, colors.tertiaryContainer, colors.secondaryContainer)
            }
        }
    }
}
