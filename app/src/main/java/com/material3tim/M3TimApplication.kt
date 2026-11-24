package com.material3tim

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.material3tim.data.repository.SettingsRepositoryImpl

class M3TimApplication : Application(), ViewModelStoreOwner {

    companion object {
        /**
         * 与 KernelSU Manager 相同的做法：预测性返回手势由系统开关控制，
         * 需要在 Activity 创建前通过隐藏字段设置。失败时静默回退。
         */
        fun setEnableOnBackInvokedCallback(appInfo: ApplicationInfo, enable: Boolean) {
            runCatching {
                val method = ApplicationInfo::class.java.getDeclaredMethod(
                    "setEnableOnBackInvokedCallback",
                    Boolean::class.javaPrimitiveType,
                )
                method.isAccessible = true
                method.invoke(appInfo, enable)
            }
        }
    }

    private val appViewModelStore by lazy { ViewModelStore() }

    override fun onCreate() {
        super.onCreate()
        AppEnvironment.init(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val enable = SettingsRepositoryImpl().enablePredictiveBack
            setEnableOnBackInvokedCallback(applicationInfo, enable)
        }
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}
