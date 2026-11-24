package com.material3tim.ui.screen.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator

/**
 * 钱包页。根据 [LocalUiMode] 分发到 Material / Miuix 两套实现；
 * 两套实现共享本文件中的图标映射（数据层只存 id，不存 ImageVector）。
 */
@Composable
fun WalletScreen() {
    val navigator = LocalNavigator.current
    val onBack: () -> Unit = { navigator.pop() }
    when (LocalUiMode.current) {
        UiMode.Material -> WalletMaterial(onBack = onBack)
        UiMode.Miuix -> WalletMiuix(onBack = onBack)
    }
}

/** 钱包服务网格（收付款/转账/红包/零钱/账单/卡券）的图标映射。 */
internal fun walletServiceIcon(id: String): ImageVector = when (id) {
    "w1" -> Icons.Filled.QrCodeScanner
    "w2" -> Icons.Filled.SwapHoriz
    "w3" -> Icons.Filled.Redeem
    "w4" -> Icons.Filled.AccountBalanceWallet
    "w5" -> Icons.AutoMirrored.Filled.ReceiptLong
    "w6" -> Icons.Filled.ConfirmationNumber
    else -> Icons.Filled.AccountBalanceWallet
}

/** 钱包功能入口（账单/银行卡/支付设置/安全保障）的图标映射。 */
internal fun walletEntryIcon(id: String): ImageVector = when (id) {
    "we1" -> Icons.AutoMirrored.Filled.ReceiptLong
    "we2" -> Icons.Filled.CreditCard
    "we3" -> Icons.Filled.Tune
    "we4" -> Icons.Filled.Security
    else -> Icons.Filled.AccountBalanceWallet
}
