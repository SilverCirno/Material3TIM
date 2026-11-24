package com.material3tim.ui.component.miuix.hyper

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.material3tim.ui.theme.LocalShowTopAppBar
import com.material3tim.ui.theme.LocalTopAppBarStyle
import androidx.compose.runtime.computedStateOf

/**
 * HyperOS 4 风格顶栏（简化版 AdaptiveTopAppBar，参考 miuix example）。
 *
 * - 「显示顶栏」关闭（[LocalShowTopAppBar] = false）→ 不渲染顶栏，仅占位状态栏高度；
 * - 顶栏风格 [LocalTopAppBarStyle] = 1（Glass）且 `backdrop != null` 且设备支持 runtime shader
 *   → [GlassTopAppBar] 玻璃材质；风格 = 0（Original）或其它回退条件 → [TopAppBar] 纯色。
 * - `isContentScrolled` 缺省时用 [computedStateOf] 从 scrollBehavior 的 contentOffset/overscrollOffset
 *   派生：只在布局/绘制阶段读取，不在组合期解包，避免滚动中整栏重组。
 */
@Composable
fun HyperTopAppBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    backdrop: Backdrop?,
    subtitle: String = "",
    isContentScrolled: Boolean? = null,
    expandable: Boolean = true,
    titleAlpha: () -> Float = { 1f },
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 「显示顶栏」关闭：不渲染顶栏。仍占位状态栏高度——本项目各页 Scaffold 的 contentWindowInsets
    // 只有水平方向（垂直方向原本由顶栏消费），直接 return 会让列表内容顶到状态栏下面。
    if (!LocalShowTopAppBar.current) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        return
    }
    // 顶栏风格：0=Original（普通 TopAppBar，即使模糊开启），1=Glass（原有玻璃逻辑）。
    val glassStyle = LocalTopAppBarStyle.current == 1
    if (glassStyle && backdrop != null && isRuntimeShaderSupported()) {
        val surface = MiuixTheme.colorScheme.surface
        val bandBrush = remember(surface) { GlassTopAppBarDefaults.bandBrush(surface) }
        val contentScrolled by remember(scrollBehavior, isContentScrolled) {
            computedStateOf {
                isContentScrolled ?: (
                    scrollBehavior.state.contentOffset < -1f ||
                        scrollBehavior.state.overscrollOffset < -1f
                    )
            }
        }
        GlassTopAppBar(
            title = title,
            modifier = modifier,
            isContentScrolled = contentScrolled,
            expandable = expandable,
            subtitle = subtitle,
            backdrop = backdrop,
            scrollBehavior = scrollBehavior,
            bandBrush = bandBrush,
            titleAlpha = titleAlpha,
            navigationIcon = navigationIcon,
            actions = actions,
            bottomContent = bottomContent,
        )
    } else {
        TopAppBar(
            title = title,
            modifier = modifier,
            expandable = expandable,
            color = MiuixTheme.colorScheme.surface,
            subtitle = subtitle,
            scrollBehavior = scrollBehavior,
            navigationIcon = navigationIcon,
            actions = actions,
            bottomContent = bottomContent,
        )
    }
}
