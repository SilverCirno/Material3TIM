package com.material3tim.ui.screen.contacts

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.OfficialAccount
import com.material3tim.data.repository.ContentRepository
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SegmentedColumn
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/**
 * 公众号页 Material 3 Expressive 实现：
 * 分段列表，每行头像 + 名称/未读 Badge + 描述 + 最新文章标题 + 时间。
 */
@Composable
fun OfficialAccountsMaterial(onBack: () -> Unit) {
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val showComingSoon: () -> Unit = {
        Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
    }
    val accounts = ContentRepository.officialAccounts
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.official_accounts_title)) },
                navigationIcon = {
                    TopBarBackButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.back),
                    )
                },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.official_accounts_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                SegmentedColumn {
                    accounts.forEach { account ->
                        item(key = account.id) {
                            OfficialAccountRowMaterial(
                                account = account,
                                onClick = showComingSoon,
                            )
                        }
                    }
                }
            }
            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
                )
            )
        }
    }
}

@Composable
private fun OfficialAccountRowMaterial(
    account: OfficialAccount,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    SegmentedListItem(
        onClick = onClick,
        leadingContent = {
            ChatAvatar(
                name = account.name,
                seed = account.avatarSeed,
                size = 46.dp,
            )
        },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = account.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (account.unreadCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Badge {
                        Text(account.unreadCount.toString())
                    }
                }
            }
        },
        supportingContent = {
            Column {
                Text(
                    text = account.description,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = account.lastArticle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = {
            Text(
                text = account.timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )
        },
    )
}
