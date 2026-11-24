package com.material3tim.ui.component.bottombar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.repository.SettingsRepositoryImpl
import com.material3tim.ui.LocalMainPagerState
import kotlinx.coroutines.launch

@Composable
fun NavigationRailMaterial(
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    val mainPagerState = LocalMainPagerState.current

    val items = listOf(
        Triple(R.string.tab_chats, Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
        Triple(R.string.tab_contacts, Icons.Filled.Contacts, Icons.Outlined.Contacts),
        Triple(R.string.tab_profile, Icons.Filled.Person, Icons.Outlined.Person),
    )

    val settingsRepo = remember { SettingsRepositoryImpl() }
    val state = rememberWideNavigationRailState(
        initialValue = if (settingsRepo.navigationRailExpanded) {
            WideNavigationRailValue.Expanded
        } else {
            WideNavigationRailValue.Collapsed
        },
    )
    val scope = rememberCoroutineScope()
    val expanded = state.targetValue == WideNavigationRailValue.Expanded
    LaunchedEffect(state.targetValue) {
        settingsRepo.navigationRailExpanded =
            state.targetValue == WideNavigationRailValue.Expanded
    }

    WideNavigationRail(
        modifier = modifier.fillMaxHeight(),
        state = state,
        colors = WideNavigationRailDefaults.colors().copy(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            WindowInsetsSides.Start + WindowInsetsSides.Vertical
        ),
        contentPadding = PaddingValues(vertical = 20.dp),
        header = {
            IconButton(
                modifier = Modifier.padding(start = 24.dp),
                onClick = {
                    scope.launch {
                        if (expanded) state.collapse() else state.expand()
                    }
                },
            ) {
                Icon(
                    if (expanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Filled.Menu,
                    contentDescription = stringResource(
                        if (expanded) R.string.nav_rail_collapse else R.string.nav_rail_expand
                    )
                )
            }
        },
    ) {
        items.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
            val selected = mainPagerState.selectedPage == index
            WideNavigationRailItem(
                railExpanded = expanded,
                selected = selected,
                onClick = {
                    // 已在目标页时也回调（animateToPage 内部处理：消息页重复点击 → 跳下一个未读会话）
                    mainPagerState.animateToPage(index)
                },
                icon = {
                    NavigationIconWithBadge(
                        icon = if (selected) selectedIcon else unselectedIcon,
                        contentDescription = stringResource(label),
                        badge = badgeFor(index, navigationBadge),
                    )
                },
                label = { Text(stringResource(label)) }
            )
        }
    }
}
