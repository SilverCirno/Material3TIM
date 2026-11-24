package com.material3tim.ui.screen.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.ui.component.rememberLocalImage

private val OnlineGreen = Color(0xFF34C759)

/**
 * 「我的」页头像：渐变圆形 + 首字，右下角在线状态圆点。
 * 两种界面风格共用，所以不依赖 Material / Miuix 任何一方的主题组件。
 */
@Composable
internal fun ProfileAvatar(
    name: String,
    avatarSeed: Int,
    isOnline: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    avatarPath: String? = null,
) {
    val realAvatar = rememberLocalImage(avatarPath, maxDimension = 320)
    val initial = name.trim().take(1).ifEmpty { "?" }
    val gradient = remember(avatarSeed) { avatarGradient(avatarSeed) }
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
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = initial,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = (size.value * 0.42f).sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.27f)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(width = 2.dp, color = Color.White, shape = CircleShape),
            )
        }
    }
}

/** 由 [avatarSeed] 取一组稳定的渐变配色。 */
internal fun avatarGradient(seed: Int): List<Color> {
    val index = ((seed % AvatarGradients.size) + AvatarGradients.size) % AvatarGradients.size
    return AvatarGradients[index]
}

private val AvatarGradients = listOf(
    listOf(Color(0xFF5B8DEF), Color(0xFF3F5BD9)),
    listOf(Color(0xFFF2994A), Color(0xFFEB5757)),
    listOf(Color(0xFF27AE60), Color(0xFF11998E)),
    listOf(Color(0xFF9B51E0), Color(0xFF7B2FF7)),
    listOf(Color(0xFF56CCF2), Color(0xFF2F80ED)),
    listOf(Color(0xFFF76B8A), Color(0xFFEC008C)),
    listOf(Color(0xFFF2C94C), Color(0xFFF2994A)),
    listOf(Color(0xFF6FCF97), Color(0xFF219653)),
    listOf(Color(0xFFBB6BD9), Color(0xFF8E2DE2)),
    listOf(Color(0xFFEB5757), Color(0xFFB32E2E)),
    listOf(Color(0xFF00B4DB), Color(0xFF0083B0)),
    listOf(Color(0xFF485563), Color(0xFF29323C)),
)
