package com.material3tim.ui.screen.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.Navigator

/**
 * 「我的」页入口（底部 Tab 之一）。
 * TIM 布局：账号信息卡 + 常用功能分组 + 设置 / 主题 / 关于分组。
 * 按 [LocalUiMode] 分发给 Material / Miuix 两套实现。
 */
@Composable
fun ProfilePager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    @Suppress("UNUSED_PARAMETER") isCurrentPage: Boolean = true,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ProfileMiuix(navigator = navigator, bottomInnerPadding = bottomInnerPadding)
        UiMode.Material -> ProfileMaterial(navigator = navigator, bottomInnerPadding = bottomInnerPadding)
    }
}
