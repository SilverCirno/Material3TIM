package com.material3tim.ui.screen.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.material3tim.data.model.ServiceGroup
import com.material3tim.data.repository.ContentRepository
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator

/**
 * 服务页。根据 [LocalUiMode] 分发到 Material / Miuix 两套实现。
 * 搜索、图标映射等纯数据逻辑放在本文件，避免两套实现重复。
 */
@Composable
fun ServicesScreen() {
    val navigator = LocalNavigator.current
    val onBack: () -> Unit = { navigator.pop() }
    when (LocalUiMode.current) {
        UiMode.Material -> ServicesMaterial(onBack = onBack)
        UiMode.Miuix -> ServicesMiuix(onBack = onBack)
    }
}

/** ServiceEntry.icon（数据层字符串 id）→ Material ImageVector。 */
internal fun serviceIcon(icon: String): ImageVector = when (icon) {
    "wallet" -> Icons.Filled.AccountBalanceWallet
    "vip" -> Icons.Filled.WorkspacePremium
    "game" -> Icons.Filled.SportsEsports
    "mail" -> Icons.Filled.Mail
    "recharge" -> Icons.Filled.Bolt
    "movie" -> Icons.Filled.Movie
    "travel" -> Icons.Filled.Flight
    "bill" -> Icons.Filled.Receipt
    "doc" -> Icons.Filled.Description
    "meeting" -> Icons.Filled.Videocam
    "cloud" -> Icons.Filled.Cloud
    "more" -> Icons.Filled.Apps
    else -> Icons.Filled.Apps
}

/** 按名称本地过滤服务分组；过滤后为空的组不展示。 */
internal fun filterServiceGroups(query: String): List<ServiceGroup> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return ContentRepository.serviceGroups
    return ContentRepository.serviceGroups.mapNotNull { group ->
        val entries = group.entries.filter { it.name.contains(keyword, ignoreCase = true) }
        if (entries.isEmpty()) null else group.copy(entries = entries)
    }
}
