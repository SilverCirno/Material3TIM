package com.material3tim.xposed

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * LSPosed 模块入口（libxposed API 102）。
 *
 * 入口类由 `META-INF/xposed/java_init.list` 声明；框架在宿主进程里实例化本类并回调
 * [onPackageReady]。第二步（本阶段）的目标：在 TIM 的主 Activity 上挂载自写的
 * Compose 根界面，接管整个 TIM UI。
 */
class MainModule : XposedModule() {

    companion object {
        const val TAG = "Material3Tim"
        const val TARGET_PACKAGE = "com.tencent.tim"

        @Volatile
        var instance: MainModule? = null
            private set

        /** 宿主(TIM)的 ClassLoader：TimBridge 反射 TIM 类时要用。 */
        @Volatile
        var hostClassLoader: ClassLoader? = null
            private set
    }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        instance = this
        Log.i(TAG, "onModuleLoaded process=${param.processName} systemServer=${param.isSystemServer}")
    }

    /**
     * 读取模块设置：优先 TIM 本地副本（material3tim_host，在 TIM 设置页里改的那份），
     * 没有再读模块 App 的远端偏好。
     *
     * 注意 onPackageReady 阶段 `ActivityThread.currentApplication()` 可能还是 null，
     * 所以直接用 applicationInfo.dataDir 读 TIM 私有目录里的 xml 文件。
     */
    private fun readHostBoolSetting(
        dataDir: String?,
        key: String,
        defaultValue: Boolean,
    ): Boolean {
        // 1) 直接读 TIM 的 shared_prefs/material3tim_host.xml（同进程/同 uid，可读）
        if (!dataDir.isNullOrEmpty()) {
            val fromFile = runCatching {
                val file = java.io.File(dataDir, "shared_prefs/material3tim_host.xml")
                if (!file.exists()) return@runCatching null
                val text = file.readText()
                val regex = Regex("""<boolean name="${Regex.escape(key)}" value="(true|false)"\s*/>""")
                regex.find(text)?.groupValues?.get(1)?.toBoolean()
            }.getOrNull()
            if (fromFile != null) return fromFile
        }
        // 2) 已有的 Context 读 SharedPreferences
        val local = runCatching {
            val app = Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as? android.content.Context
            app?.getSharedPreferences("material3tim_host", android.content.Context.MODE_PRIVATE)
        }.getOrNull()
        if (local != null && local.contains(key)) return local.getBoolean(key, defaultValue)
        // 3) 模块 App 的远端偏好
        return runCatching {
            getRemotePreferences("settings").getBoolean(key, defaultValue)
        }.getOrDefault(defaultValue)
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != TARGET_PACKAGE) return
        Log.i(TAG, "onPackageReady ${param.packageName} first=${param.isFirstPackage}")
        hostClassLoader = param.classLoader
        try {
            // TIM(targetSdk 31) 默认不参与预测性返回；是否开启跟随模块设置里的
            // 「预测性返回手势」开关（enable_predictive_back，默认关闭）。
            // onPackageReady 阶段设置才来得及影响后续窗口创建。
            val enabled = readHostBoolSetting(param.applicationInfo.dataDir, "enable_predictive_back", false)
            com.material3tim.M3TimApplication.setEnableOnBackInvokedCallback(param.applicationInfo, enabled)
            Log.i(TAG, "enableOnBackInvokedCallback=$enabled for ${param.packageName}")
        } catch (t: Throwable) {
            Log.w(TAG, "enableOnBackInvokedCallback failed", t)
        }
        try {
            TimUiHook.install(this, param.classLoader)
        } catch (t: Throwable) {
            Log.e(TAG, "install hooks failed", t)
        }
    }
}
