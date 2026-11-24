package com.material3tim.ui.screen.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.WalletService
import com.material3tim.data.repository.ContentRepository
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.groupedCardItems
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 钱包页 Miuix 实现：
 * 余额 Card（primaryContainer）+ 三列服务 Card 网格 + HyperOS4 分段卡片入口 + 安全保障卡片。
 */
@Composable
fun WalletMiuix(onBack: () -> Unit) {
    val context = LocalContext.current
    val wallet = ContentRepository.wallet
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val showComingSoon: () -> Unit = {
        Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
    }

    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val enableBlur = LocalEnableBlur.current
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/无 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.wallet_title),
                    isContentScrolled = listState.canScrollBackward,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { HyperBackIcon(onClick = onBack) },
                )
            },
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    // 采样层要早于 inset/padding：记录不透明底 + 列表内容（不含玻璃顶栏自身）。
                    .then(
                        if (backdrop != null) {
                            Modifier
                                .layerBackdrop(backdrop)
                                .background(colorScheme.surface)
                        } else {
                            Modifier
                        },
                    ),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item(key = "balance") {
                    WalletBalanceCardMiuix(
                        balance = wallet.balance,
                        onRecharge = showComingSoon,
                        onCashOut = showComingSoon,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp),
                    )
                }
                item(key = "services") {
                    WalletServiceGridMiuix(
                        services = wallet.services,
                        onClick = showComingSoon,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp),
                    )
                }
                // HyperOS4 分组卡片：钱包入口合并为 squircle 分段卡片。
                groupedCardItems(
                    keyPrefix = "walletEntries",
                    outerTopPadding = 12.dp,
                    outerBottomPadding = 6.dp,
                    items = wallet.entries.map { entry ->
                        CardItem(entry.id) {
                            ArrowPreference(
                                title = entry.title,
                                summary = entry.summary.takeIf { it.isNotBlank() },
                                startAction = {
                                    Icon(
                                        imageVector = walletEntryIcon(entry.id),
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = showComingSoon,
                            )
                        }
                    },
                )
                item(key = "security") {
                    WalletSecurityFooterMiuix(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp),
                    )
                }
                item(key = "bottom") {
                    Spacer(
                        Modifier.height(
                            WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding() + 6.dp
                        )
                    )
                }
            }
        }
    }
}

/** 顶栏返回按钮：跟随顶栏风格（HyperTopBarGlassButton → Glass 圆钮 / Original 普通 IconButton）。 */
@Composable
private fun HyperBackIcon(onClick: () -> Unit) {
    HyperTopBarGlassButton(onClick = onClick) {
        Icon(
            imageVector = MiuixIcons.Back,
            contentDescription = stringResource(R.string.back),
            tint = colorScheme.onSurface,
        )
    }
}

@Composable
private fun WalletBalanceCardMiuix(
    balance: String,
    onRecharge: () -> Unit,
    onCashOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(20.dp),
        colors = CardDefaults.defaultColors(
            color = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(
            text = stringResource(R.string.wallet_balance),
            fontSize = 14.sp,
            color = colorScheme.onPrimaryContainer,
        )
        Text(
            text = stringResource(R.string.wallet_amount, balance),
            fontSize = 36.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onRecharge,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    color = colorScheme.onPrimaryContainer,
                    contentColor = colorScheme.primaryContainer,
                ),
            ) {
                Text(text = stringResource(R.string.wallet_recharge))
            }
            Button(
                onClick = onCashOut,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    color = colorScheme.onPrimaryContainer,
                    contentColor = colorScheme.primaryContainer,
                ),
            ) {
                Text(text = stringResource(R.string.wallet_cash_out))
            }
        }
    }
}

@Composable
private fun WalletServiceGridMiuix(
    services: List<WalletService>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (services.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        services.chunked(3).forEach { rowServices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowServices.forEach { service ->
                    Card(
                        modifier = Modifier.weight(1f),
                        onClick = onClick,
                        showIndication = true,
                        insideMargin = PaddingValues(horizontal = 2.dp, vertical = 12.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = walletServiceIcon(service.id),
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = service.name,
                                fontSize = 12.sp,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                }
                repeat(3 - rowServices.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WalletSecurityFooterMiuix(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.wallet_security),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.wallet_security_summary),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
