package com.material3tim.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 「真实莫奈取色」使用的 Context（需要能访问系统资源 `android.R.color.system_accent*`）。
 *
 * - 独立 App：不需要提供（为 null 时回退到 LocalContext）。
 * - Xposed 宿主(TIM)：`LocalContext` 是模块 Context，其 Resources 只包含模块 APK，
 *   读不到系统强调色 → 主题会退化成默认紫。宿主侧通过这个 local 传入宿主 Activity。
 */
val LocalMonetContext = staticCompositionLocalOf<Context?> { null }

/**
 * 从系统壁纸取色（Monet）得到的种子色。
 *
 * 直接读 `android.R.color.system_accent1_200`（Android 12+ 由系统壁纸生成）。
 * 不用 `dynamicLightColorScheme(context)`：在 TIM 宿主里该 API 实测返回全 0 色板。
 */
@Composable
fun rememberMonetSeedColor(): Color? {
    val context = LocalMonetContext.current ?: androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            context.resources.getColor(android.R.color.system_accent1_200, null)
        }.getOrNull()
            ?.takeIf { it != 0 }
            ?.let { Color(it) }
    }
}
