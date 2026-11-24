package com.material3tim

import android.content.Context

/**
 * 全局 Context 抽象。
 *
 * - 独立 App：初始化成 [M3TimApplication]。
 * - Xposed 宿主（TIM 进程）：由 [com.material3tim.xposed.UiHost] 初始化成「模块 Context 包装」，
 *   它的 `resources` / `sharedPreferences` 都指向模块 APK 自己的资源与设置。
 *
 * 注意：宿主模式下**不能**把它换成 `applicationContext`（那会丢掉模块资源包装），
 * 所以这里是原样保存传入的 Context。
 */
object AppEnvironment {

    @Volatile
    private var contextRef: Context? = null

    fun init(context: Context) {
        contextRef = context
    }

    val context: Context
        get() = contextRef ?: error("AppEnvironment not initialized")
}

internal fun appContext(): Context = AppEnvironment.context
