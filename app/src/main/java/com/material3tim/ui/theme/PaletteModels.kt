package com.material3tim.ui.theme

/**
 * 自定义调色板风格（与 materialkolor PaletteStyle 名称保持一致，便于设置迁移）。
 * materialkolor 5.0.1 与 miuix 0.9.5 的 material-color-utilities 5/6 冲突，
 * 项目改为直接依赖 utilities 6 并保留同名枚举，避免旧库的 scheme 包引用。
 */
enum class PaletteStyle {
    TonalSpot,
    Neutral,
    Vibrant,
    Expressive,
    Rainbow,
    FruitSalad,
    Monochrome,
    Fidelity,
    Content,
    /** SPEC 2026 专属的 CMF（Color, Material, Finish）风格；仅在 spec=SPEC_2026 时使用。 */
    Cmf,
}

/** 动态配色规范版本（对应 material-color-utilities ColorSpecVersion）。 */
enum class ColorSpecVersion {
    SPEC_2021,
    SPEC_2025,
    SPEC_2026,
    ;

    companion object {
        val Default: ColorSpecVersion = SPEC_2025
    }
}
