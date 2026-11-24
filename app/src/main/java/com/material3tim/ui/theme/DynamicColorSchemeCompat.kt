package com.material3tim.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.rememberDynamicColorScheme
import com.materialkolor.PaletteStyle as MaterialKolorPaletteStyle

/**
 * MaterialKolor 6.0.0 适配层：项目内仍沿用本地 [PaletteStyle] / [ColorSpecVersion] 枚举
 * （用于设置持久化与 UI 展示，枚举常量名与旧版 materialkolor 保持一致，老设置可直接读取），
 * 这里把它们映射到 MaterialKolor 6.0 的类型后调用官方 `rememberDynamicColorScheme`。
 *
 * 背景：MaterialKolor 6.0.0 已适配 material-color-utilities 6.0.0，与 miuix 0.9.5 依赖的
 * utilities 版本一致，不会再出现旧版 (5.0.1) 与 miuix 的 scheme/dynamiccolor 包冲突。
 */
@Composable
fun rememberM3TimDynamicColorScheme(
    seedColor: Color,
    isDark: Boolean,
    style: PaletteStyle,
    specVersion: ColorSpecVersion,
): ColorScheme = rememberDynamicColorScheme(
    seedColor = seedColor,
    isDark = isDark,
    isAmoled = false,
    style = style.toMaterialKolorStyle(),
    specVersion = specVersion.toMaterialKolorSpec(),
)

/** 本地枚举 → MaterialKolor 6.0 PaletteStyle（sealed interface 的 data object）。 */
internal fun PaletteStyle.toMaterialKolorStyle(): MaterialKolorPaletteStyle = when (this) {
    PaletteStyle.TonalSpot -> MaterialKolorPaletteStyle.TonalSpot
    PaletteStyle.Neutral -> MaterialKolorPaletteStyle.Neutral
    PaletteStyle.Vibrant -> MaterialKolorPaletteStyle.Vibrant
    PaletteStyle.Expressive -> MaterialKolorPaletteStyle.Expressive
    PaletteStyle.Rainbow -> MaterialKolorPaletteStyle.Rainbow
    PaletteStyle.FruitSalad -> MaterialKolorPaletteStyle.FruitSalad
    PaletteStyle.Monochrome -> MaterialKolorPaletteStyle.Monochrome
    PaletteStyle.Fidelity -> MaterialKolorPaletteStyle.Fidelity
    PaletteStyle.Content -> MaterialKolorPaletteStyle.Content
    // CMF 是 MaterialKolor 6.0 的 class（可带第二个种子色）；这里只用主种子色。
    PaletteStyle.Cmf -> MaterialKolorPaletteStyle.Cmf()
}

/** 本地枚举 → material-color-utilities ColorSpec.SpecVersion。 */
internal fun ColorSpecVersion.toMaterialKolorSpec(): ColorSpec.SpecVersion = when (this) {
    ColorSpecVersion.SPEC_2021 -> ColorSpec.SpecVersion.SPEC_2021
    ColorSpecVersion.SPEC_2025 -> ColorSpec.SpecVersion.SPEC_2025
    ColorSpecVersion.SPEC_2026 -> ColorSpec.SpecVersion.SPEC_2026
}
