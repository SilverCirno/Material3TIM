package com.material3tim.xposed

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.content.res.Resources

/**
 * 宿主进程里的模块 Context：
 * - `resources` / `assets` / `theme` 指向模块 APK 自己的资源（Compose 里的 `R.string.*` 才能解析对）
 * - `getSharedPreferences("settings")` 返回 [HostSettingsPrefs]（远端只读 + 本进程可写）
 * - 其它能力（window、ContentResolver、包信息…）沿用宿主 Activity
 */
internal class ModuleContext(
    base: Context,
    private val moduleResources: Resources,
    private val settingsPrefs: SharedPreferences,
) : ContextWrapper(base) {

    override fun getResources(): Resources = moduleResources

    override fun getAssets(): AssetManager = moduleResources.assets

    /**
     * 宿主 ComposeView 的 Context 主题。
     *
     * 注意：不能返回 `moduleResources.newTheme()` 这种空主题。Compose/Android 平台组件会从
     * `view.context.theme` 读取平台属性；空主题里 `magnifierWidth` / `magnifierHeight` /
     * `magnifierZoom` 全是 0，长按文本输入框时系统文本选择放大镜（android.widget.Magnifier）
     * 会在 `performPixelCopy` 里执行 `Bitmap.createBitmap(0, 0)` → 直接崩溃
     * （IllegalArgumentException: width and height must be > 0）。
     *
     * 这里以系统 DeviceDefault 作为基础主题，保证放大镜、文本选择工具栏等平台组件能读到正常的
     * 默认尺寸/缩放值。Compose 的 Material/Miuix 主题仍在 Compose 层单独提供，不受影响。
     */
    @Suppress("DEPRECATION")
    private val hostTheme: Resources.Theme = moduleResources.newTheme().apply {
        // 在模块空主题上叠加系统 DeviceDefault 的默认属性（含 magnifierStyle）。
        applyStyle(android.R.style.Theme_DeviceDefault, true)
    }

    override fun getTheme(): Resources.Theme = hostTheme

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
        if (name == SETTINGS_PREFS) settingsPrefs else super.getSharedPreferences(name, mode)

    private companion object {
        const val SETTINGS_PREFS = "settings"
    }
}
