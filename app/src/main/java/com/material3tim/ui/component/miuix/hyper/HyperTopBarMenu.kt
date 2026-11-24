package com.material3tim.ui.component.miuix.hyper

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.theme.LocalTopAppBarStyle
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * HyperOS4 顶栏圆形 Glass 按钮 + 「按钮形状 morph 成 popup」菜单（参考 miuix example MainPage/GlassPage）。
 *
 * - 玻璃模式判定：顶栏风格为 Glass（[LocalTopAppBarStyle] == 1）、页面通过 [LocalPageBackdrop]
 *   提供了采样源，且设备支持 runtime shader（与 [HyperTopAppBar] 的条件一致）；
 *   顶栏风格切到 Original 时按钮回退普通 IconButton。
 * - 触发按钮：玻璃模式用 [GlassIconButton]（圆形玻璃），否则普通 [IconButton]（纯色回退）。
 * - 菜单 morph：按钮挂 [glassPopupAnchor]（记录控件窗口 bounds、打开时隐藏按钮本体），
 *   内容图标挂 [glassPopupAnchorContent]；菜单用 [GlassTransformPopup] 从按钮矩形展开，
 *   自带预测性返回（内部注册 navigationevent，无需外部 BackHandler）。
 * - 菜单必须**常驻组合**（用 show 控制显隐），不要 `if (show)` 包裹。
 */

/** 顶栏 morph 菜单的一项：文案 + 可选图标 + 业务回调。 */
data class HyperTopBarMenuEntry(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

/**
 * 当前顶栏是否处于玻璃模式：顶栏风格 = Glass（[LocalTopAppBarStyle] == 1）、页面提供 backdrop
 * 且设备支持 runtime shader。与 [HyperTopAppBar] 内部选择 GlassTopAppBar 的条件保持一致——
 * 风格切到 Original（纯色 TopAppBar）时，顶栏按钮也要回退普通 IconButton。
 */
@Composable
fun isHyperTopBarGlassMode(): Boolean {
    val backdrop = LocalPageBackdrop.current
    return LocalTopAppBarStyle.current == 1 && backdrop != null && isRuntimeShaderSupported()
}

/**
 * 顶栏圆形图标按钮（页面级通用封装）：
 * - 玻璃模式（[isHyperTopBarGlassMode]）→ miuix-glass [GlassIconButton]；
 * - 否则 → 普通 [IconButton]，并用 44dp 尺寸与 GlassIconButton 对齐，切换顶栏风格时布局不跳动。
 *
 * 供各 Miuix 页面顶栏的返回/更多/关闭等按钮使用，替代直接调用 [GlassIconButton]。
 */
@Composable
fun HyperTopBarGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (isHyperTopBarGlassMode()) {
        GlassIconButton(onClick = onClick, modifier = modifier) {
            content()
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(44.dp),
        ) {
            content()
        }
    }
}

/**
 * 顶栏圆形图标按钮。
 *
 * @param onClick 点击回调
 * @param contentDescription 无障碍描述
 * @param icon 图标（普通 Miuix/Material 图标即可；玻璃模式下用 Glass 图标观感更佳）
 * @param modifier 作用于按钮容器
 * @param anchor 菜单 morph 锚点：非空时会记录按钮/图标窗口矩形供 [HyperTopBarDropdownMenu] 使用
 */
@Composable
fun HyperTopBarIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    anchor: GlassPopupAnchor? = null,
) {
    val glassMode = isHyperTopBarGlassMode()
    // 圆形按钮：cornerRadius = 直径/2（GlassTopAppBar 按钮直径 44dp → 24dp 圆角观感一致）。
    val anchorModifier = if (anchor != null) {
        Modifier.glassPopupAnchor(
            anchor = anchor,
            cornerRadius = 24.dp,
            floating = true,
        )
    } else {
        Modifier
    }
    val iconModifier = if (anchor != null) {
        // morph 时被「复制」的内容范围：只复制图标（按钮背景由 GlassIconButton 表面提供）。
        Modifier
            .size(28.dp)
            .glassPopupAnchorContent(anchor)
    } else {
        Modifier
    }
    val iconTint = MiuixTheme.colorScheme.onSurface
    if (glassMode) {
        GlassIconButton(
            onClick = onClick,
            modifier = modifier.then(anchorModifier),
        ) {
            MiuixIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = iconModifier,
                tint = iconTint,
            )
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier.then(anchorModifier),
        ) {
            MiuixIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = iconModifier,
                tint = iconTint,
            )
        }
    }
}

/**
 * 顶栏 morph 菜单：从触发按钮矩形展开为玻璃菜单（[GlassTransformPopup]）。
 *
 * 放在页面/根级 `Box` 的最后（`BoxScope`），保持常驻组合；[anchor] 与
 * [HyperTopBarIconButton] 共用同一个 `rememberGlassPopupAnchor()`。
 * 预测性返回由 GlassTransformPopup 内部处理（依赖 LocalNavigationEventDispatcherOwner）。
 *
 * @param show 是否展开
 * @param onDismissRequest 外部点击/返回手势请求关闭
 * @param anchor 与触发按钮共用的锚点
 * @param entries 菜单项（文案/图标/回调，顺序即展示顺序）
 * @param anchorContent 收起态展示的按钮内容副本（通常与按钮图标一致）
 * @param backdrop 背景采样；null 时回退到按钮自身 backdrop / 不透明填充
 */
@Composable
fun BoxScope.HyperTopBarDropdownMenu(
    show: Boolean,
    onDismissRequest: () -> Unit,
    anchor: GlassPopupAnchor,
    entries: List<HyperTopBarMenuEntry>,
    anchorContent: @Composable () -> Unit,
    backdrop: Backdrop? = LocalPageBackdrop.current,
) {
    GlassTransformPopup(
        show = show,
        onDismissRequest = onDismissRequest,
        anchor = anchor,
        backdrop = backdrop,
        anchorContent = anchorContent,
    ) {
        entries.forEach { entry ->
            GlassPopupItem(
                text = entry.label,
                icon = entry.icon,
                onClick = {
                    // 与 miuix 菜单语义一致：先关闭菜单，再执行菜单项业务动作。
                    onDismissRequest()
                    entry.onClick()
                },
            )
        }
    }
}
