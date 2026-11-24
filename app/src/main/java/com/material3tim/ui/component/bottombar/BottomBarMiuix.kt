package com.material3tim.ui.component.bottombar

import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.ui.LocalMainPagerState
import com.material3tim.ui.component.FloatingBottomBar
import com.material3tim.ui.component.FloatingBottomBarItem
import com.material3tim.ui.theme.LocalEnableFloatingBottomBar
import com.material3tim.ui.theme.LocalEnableFloatingBottomBarBlur
import com.material3tim.ui.theme.LocalFloatingNavigationBarPosition
import com.material3tim.ui.theme.LocalFloatingNavigationBarStyle
import com.material3tim.ui.theme.LocalNavigationBarMode
import com.material3tim.ui.util.BlurredBar
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Miuix 底栏（三种形态，由主题外观页「底栏」分组控制）：
 * - 标准底栏：[NavigationBar]，显示模式 = 图标+文本 / 仅图标 / 选中项显示文本；
 * - 悬浮底栏风格 0 Original：[FloatingNavigationBar]（可居中/靠左/靠右），有模糊时按胶囊 shape 铺 textureBlur；
 * - 悬浮底栏风格 1 Glass：miuix-glass [GlassNavigationBar]（HyperOS4 默认观感）；
 * - 悬浮底栏风格 2 iOS-like：自定义液体玻璃 [FloatingBottomBar]（从 v0.5.267 恢复）。
 */
@Composable
fun BottomBarMiuix(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
    navigationBadge: NavigationBadgeState,
    modifier: Modifier,
) {
    val mainState = LocalMainPagerState.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val navigationBarMode = LocalNavigationBarMode.current
    val floatingNavigationBarStyle = LocalFloatingNavigationBarStyle.current
    val floatingNavigationBarPosition = LocalFloatingNavigationBarPosition.current

    val items = BottomBarDestination.entries.map { destination ->
        NavigationItem(
            label = stringResource(destination.label),
            icon = destination.icon,
        )
    }
    if (!enableFloatingBottomBar) {
        BlurredBar(blurBackdrop) {
            NavigationBar(
                modifier = modifier,
                color = if (blurBackdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                // 0=图标+文本，1=仅图标，2=选中项显示文本（枚举顺序与设置项一致）。
                mode = NavigationBarDisplayMode.entries[navigationBarMode.coerceIn(0, 2)],
                content = {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            modifier = Modifier.weight(1f),
                            icon = item.icon,
                            label = item.label,
                            selected = mainState.selectedPage == index,
                            onClick = {
                                mainState.animateToPage(index)
                            },
                            badge = navigationBadgeFor(index, navigationBadge),
                        )
                    }
                }
            )
        }
    } else {
        when (floatingNavigationBarStyle.coerceIn(0, 2)) {
            // 0 = Original：miuix 官方 FloatingNavigationBar（参考 example AppContent：
            // 模糊直接按胶囊 shape 用 textureBlur 铺在 bar 自己身上，而不是整条全宽磨砂；
            // 它自带导航栏 inset padding，不要再套旧 FloatingBottomBar 的手动 bottom padding）。
            0 -> {
                // 悬浮底栏模糊开关同样对 Original 风格生效（与 Glass / iOS-like 一致）；
                // 全局「模糊」关闭时 blurBackdrop 为 null，自然回退纯色胶囊。
                // 用 MainActivity 提供的 page backdrop（仅在「浮动底栏 + 模糊」时挂载 layerBackdrop），
                // 不再依赖全局 blurBackdrop（后者现在只给标准底栏用）。
                val barBlur = if (enableFloatingBottomBarBlur) backdrop as? LayerBackdrop else null
                val barShape = remember { RoundedCornerShape(FloatingToolbarDefaults.CornerRadius) }
                FloatingNavigationBar(
                    // 不要传调用方的 Modifier（其中含 BoxScope.align 父数据）：它会覆盖
                    // FloatingNavigationBar 内部 Row 的 ColumnScope.align，导致 居中/靠左/靠右 失效。
                    modifier = if (barBlur != null) {
                        Modifier.textureBlur(
                            backdrop = barBlur,
                            shape = barShape,
                            blurRadius = 25f,
                            colors = BlurColors(
                                blendColors = listOf(
                                    BlendColorEntry(
                                        color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                                    ),
                                ),
                            ),
                        )
                    } else {
                        Modifier
                    },
                    color = if (barBlur != null) {
                        Color.Transparent
                    } else {
                        MiuixTheme.colorScheme.surfaceContainer
                    },
                    horizontalAlignment = when (floatingNavigationBarPosition.coerceIn(0, 2)) {
                        1 -> Alignment.Start
                        2 -> Alignment.End
                        else -> Alignment.CenterHorizontally
                    },
                ) {
                    items.forEachIndexed { index, item ->
                        FloatingNavigationBarItem(
                            selected = mainState.selectedPage == index,
                            onClick = { mainState.animateToPage(index) },
                            icon = item.icon,
                            label = item.label,
                            badge = navigationBadgeFor(index, navigationBadge),
                        )
                    }
                }
            }

            // 1 = Glass：HyperOS4 玻璃悬浮底栏（默认，保持现有观感）。
            1 -> {
                val glassItems = BottomBarDestination.entries.mapIndexed { index, destination ->
                    val label = stringResource(destination.label)
                    GlassNavigationItem(
                        icon = destination.icon,
                        label = label,
                        contentDescription = label,
                        badge = navigationBadgeFor(index, navigationBadge),
                    )
                }
                GlassNavigationBar(
                    items = glassItems,
                    selectedIndex = mainState.selectedPage,
                    onSelect = { mainState.animateToPage(it) },
                    backdrop = if (enableFloatingBottomBarBlur) backdrop else null,
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
            }

            // 2 = iOS-like：自定义液体玻璃浮动底栏（与 v0.5.267 的用法一致：
            // 手动 bottom padding + pointerInput 吞掉空白处点击 + FloatingBottomBar 的拖拽选择）。
            else -> {
                val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    .let { inset -> if (inset != 0.dp) 8.dp + inset else 28.dp }
                FloatingBottomBar(
                    modifier = modifier
                        .pointerInput(Unit) {
                            detectTapGestures { }
                        }
                        .padding(start = 28.dp, end = 28.dp, bottom = bottomPadding),
                    selectedIndex = mainState.selectedPage,
                    onSelected = { mainState.animateToPage(it) },
                    backdrop = backdrop,
                    tabsCount = items.size,
                    isBlurEnabled = enableFloatingBottomBarBlur,
                ) { activateTab ->
                    items.forEachIndexed { index, item ->
                        FloatingBottomBarItem(
                            selected = mainState.selectedPage == index,
                            onClick = {
                                activateTab(index)
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                        ) {
                            // Icon and label take LocalContentColor so the FloatingBottomBar backdrop copy
                            // can recolor them to the accent tone inside the indicator pill.
                            val badge = navigationBadgeFor(index, navigationBadge, floating = true)
                            val icon: @Composable () -> Unit = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                )
                            }
                            if (badge != null) {
                                BadgedBox(badge = { badge() }) { icon() }
                            } else {
                                icon()
                            }
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class BottomBarDestination(
    @get:StringRes val label: Int,
    val icon: ImageVector,
) {
    Chat(R.string.tab_chats, Icons.AutoMirrored.Rounded.Chat),
    Contacts(R.string.tab_contacts, Icons.Rounded.Contacts),
    Profile(R.string.tab_profile, Icons.Rounded.Person),
}

internal fun navigationBadgeFor(
    index: Int,
    state: NavigationBadgeState,
    floating: Boolean = false,
): (@Composable () -> Unit)? {
    val badge = badgeFor(index, state) ?: return null
    return when (badge.tone) {
        BadgeTone.Alert -> {
            {
                Badge {
                    Text(badge.count.toString())
                }
            }
        }

        BadgeTone.Accent -> {
            {
                Badge(
                    containerColor = if (floating) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.primary,
                    contentColor = if (floating) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onPrimary,
                ) {
                    Text(badge.count.toString())
                }
            }
        }
    }
}
