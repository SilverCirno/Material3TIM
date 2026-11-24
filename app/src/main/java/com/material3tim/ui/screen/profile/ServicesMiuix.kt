package com.material3tim.ui.screen.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.ServiceEntry
import com.material3tim.data.model.ServiceGroup
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 服务页 Miuix 实现：
 * TextField 搜索框本地过滤 + 常用/生活/效率分组 + 每组四列 Card 网格。
 */
@Composable
fun ServicesMiuix(onBack: () -> Unit) {
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val showComingSoon: () -> Unit = {
        Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
    }
    var query by rememberSaveable { mutableStateOf("") }
    val groups = remember(query) { filterServiceGroups(query) }

    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val enableBlur = LocalEnableBlur.current
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/无 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.services_title),
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
                item(key = "search") {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        label = stringResource(R.string.services_search),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp, bottom = 6.dp),
                    )
                }
                if (groups.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.services_empty),
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                } else {
                    groups.forEach { group ->
                        item(key = "group_${group.title}") {
                            ServicesGroupMiuix(
                                group = group,
                                onClick = showComingSoon,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .padding(vertical = 6.dp),
                            )
                        }
                    }
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
private fun ServicesGroupMiuix(
    group: ServiceGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = group.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(start = 20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.entries.chunked(4).forEach { rowEntries ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowEntries.forEach { entry ->
                        ServiceCellMiuix(
                            entry = entry,
                            onClick = onClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - rowEntries.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCellMiuix(
    entry: ServiceEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        showIndication = true,
        insideMargin = PaddingValues(horizontal = 2.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = serviceIcon(entry.icon),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.name,
                fontSize = 11.sp,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
