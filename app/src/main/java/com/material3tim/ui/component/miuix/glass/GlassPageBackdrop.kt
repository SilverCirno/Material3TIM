package com.material3tim.ui.component.miuix.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 页面级背景采样源：供后续 GlassDialog / GlassBottomSheet / GlassIconDropdownMenu 等
 * 需要采样「页面内容」的玻璃组件使用（页面内用 [rememberPageBackdrop] 创建并 provide）。
 */
val LocalPageBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * 页面采样 backdrop（Miuix Glass Phase 2 基础设施）。
 *
 * - 采样内容 = 不透明底色 surface + 页面内容（通过 `layerBackdrop(backdrop)` 记录）；
 *   **不包含玻璃组件自身**（顶栏/弹窗等玻璃控件绘制在采样层之外，否则会自我采样产生脏边）；
 * - `enabled = false`（用户关闭模糊）或设备不支持 runtime shader 时返回 null，
 *   玻璃组件自动回退纯色填充，页面保持正常背景；
 * - 使用方把 `Modifier.layerBackdrop(backdrop)` 放在 inset/padding **之前**（内容会被记录），
 *   再叠加 `.background(surface)` 保证采样底是不透明色而非透明。
 */
@Composable
fun rememberPageBackdrop(enabled: Boolean): LayerBackdrop? {
    if (!enabled || !isRuntimeShaderSupported()) return null
    val surface = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surface)
        drawContent()
    }
}
