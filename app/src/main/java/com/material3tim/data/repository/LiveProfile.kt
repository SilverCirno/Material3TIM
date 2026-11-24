package com.material3tim.data.repository

import com.material3tim.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 真实登录账号资料（宿主里由 TIM Kernel 填充；独立 App 为 null → 用示例数据）。 */
object LiveProfile {

    @Volatile
    private var live: UserProfile? = null

    private val _flow = MutableStateFlow<UserProfile?>(null)
    val flow: StateFlow<UserProfile?> = _flow.asStateFlow()

    fun update(profile: UserProfile?) {
        live = profile
        _flow.value = profile
    }

    fun peek(): UserProfile? = live
}
