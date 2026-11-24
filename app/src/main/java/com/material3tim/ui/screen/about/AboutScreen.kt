package com.material3tim.ui.screen.about

import androidx.compose.runtime.Composable
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutMiuix(onBack = { navigator.pop() })
        UiMode.Material -> AboutMaterial(onBack = { navigator.pop() })
    }
}
