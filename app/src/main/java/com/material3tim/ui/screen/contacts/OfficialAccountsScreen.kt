package com.material3tim.ui.screen.contacts

import androidx.compose.runtime.Composable
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator

/**
 * 公众号页。根据 [LocalUiMode] 分发到 Material / Miuix 两套实现。
 */
@Composable
fun OfficialAccountsScreen() {
    val navigator = LocalNavigator.current
    val onBack: () -> Unit = { navigator.pop() }
    when (LocalUiMode.current) {
        UiMode.Material -> OfficialAccountsMaterial(onBack = onBack)
        UiMode.Miuix -> OfficialAccountsMiuix(onBack = onBack)
    }
}
