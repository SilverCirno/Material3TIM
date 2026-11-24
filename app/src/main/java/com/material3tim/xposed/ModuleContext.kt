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

    override fun getTheme(): Resources.Theme = moduleResources.newTheme()

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
        if (name == SETTINGS_PREFS) settingsPrefs else super.getSharedPreferences(name, mode)

    private companion object {
        const val SETTINGS_PREFS = "settings"
    }
}
