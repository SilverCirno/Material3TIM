package com.material3tim.ui.screen.profile

import androidx.compose.runtime.Composable
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator

/**
 * 「编辑资料」页：按 [LocalUiMode] 分发 Material / Miuix 两种界面风格。
 * 只维护本地状态（示例数据），不写回仓库。
 */
@Composable
fun EditProfileScreen() {
    val navigator = LocalNavigator.current
    when (LocalUiMode.current) {
        UiMode.Miuix -> EditProfileMiuix(onBack = { navigator.pop() })
        UiMode.Material -> EditProfileMaterial(onBack = { navigator.pop() })
    }
}
