package com.material3tim.ui.screen.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.FavoriteItem
import com.material3tim.data.model.FavoriteType
import com.material3tim.data.repository.ContentRepository
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.groupedCardItems
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 「我的收藏」页（Miuix 风格）。
 * 顶部筛选 Tab 切换只在本地生效，示例数据来自 [ContentRepository.favorites]。
 *
 * Tab 放在顶栏 bottomContent 而不是列表里：GlassSegmentedTabRow 必须采样页面 backdrop，
 * 若放在 `Modifier.layerBackdrop` 包裹的列表内会「采样自身录制层」（miuix 示例也把列表内 Tab
 * 改采样壁纸来规避）；放在玻璃顶栏内由 GlassTopAppBar 的 backdrop 提供采样，安全且保持同一交互。
 * 关闭模糊/无 runtime shader（backdrop == null）时回退普通 TabRow。
 */
@Composable
internal fun FavoritesMiuix(onBack: () -> Unit) {
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val favorites = ContentRepository.favorites
    val filters = FavoriteFilter.entries
    val filterLabels = filters.map { stringResource(it.labelRes) }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filtered = remember(selectedFilterIndex) {
        val type = filters[selectedFilterIndex].type
        if (type == null) favorites else favorites.filter { it.type == type }
    }
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
                    title = stringResource(R.string.favorites_title),
                    isContentScrolled = listState.canScrollBackward,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { HyperBackIcon(onClick = onBack) },
                    bottomContent = {
                        val tabModifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                        // backdrop 非空说明已走 GlassTopAppBar 分支：用玻璃分段 TabRow（内部由顶栏 backdrop 采样）。
                        if (backdrop != null) {
                            GlassSegmentedTabRow(
                                tabs = filterLabels,
                                selectedIndex = selectedFilterIndex,
                                onSelect = { selectedFilterIndex = it },
                                backdrop = backdrop,
                                modifier = tabModifier,
                            )
                        } else {
                            TabRow(
                                tabs = filterLabels,
                                selectedTabIndex = selectedFilterIndex,
                                onTabSelected = { selectedFilterIndex = it },
                                modifier = tabModifier,
                            )
                        }
                    },
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
                item(key = "top_spacer") {
                    Spacer(Modifier.height(6.dp))
                }
                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 96.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.StarBorder,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariantSummary,
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.favorites_empty),
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                } else {
                    // HyperOS4 分组卡片：收藏条目合并为 squircle 分段卡片。
                    groupedCardItems(
                        keyPrefix = "favorites",
                        outerTopPadding = 0.dp,
                        outerBottomPadding = 6.dp,
                        items = filtered.map { item ->
                            CardItem(item.id) {
                                FavoriteSegmentMiuix(item = item, onClick = showComingSoon)
                            }
                        },
                    )
                }
                item(key = "bottom_padding") {
                    Spacer(
                        Modifier.height(
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                    24.dp
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

/** 单条收藏分段内容：类型缩略图 + 标题 + 摘要 + 来源/时间等信息。 */
@Composable
private fun FavoriteSegmentMiuix(
    item: FavoriteItem,
    onClick: () -> Unit,
) {
    val voiceLabel = if (item.type == FavoriteType.VOICE && item.durationSeconds > 0) {
        stringResource(R.string.voice_duration, item.durationSeconds)
    } else {
        null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FavoriteThumbMiuix(item = item)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
                maxLines = 1,
            )
            if (item.snippet.isNotBlank()) {
                Text(
                    text = item.snippet,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = favoriteMeta(item, voiceLabel),
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** 收藏类型缩略图：图片类型用渐变占位图，其它类型用主题色底的图标。 */
@Composable
private fun FavoriteThumbMiuix(item: FavoriteItem) {
    val isImage = item.type == FavoriteType.IMAGE
    val container = when (item.type) {
        FavoriteType.TEXT -> colorScheme.secondaryContainer
        FavoriteType.LINK -> colorScheme.primaryContainer
        FavoriteType.IMAGE -> Color.Transparent
        FavoriteType.FILE -> colorScheme.surfaceContainerHigh
        FavoriteType.VOICE -> colorScheme.secondaryContainer
    }
    val content = when (item.type) {
        FavoriteType.TEXT -> colorScheme.onSecondaryContainer
        FavoriteType.LINK -> colorScheme.onPrimaryContainer
        FavoriteType.IMAGE -> Color.White
        FavoriteType.FILE -> colorScheme.onSurfaceContainerHigh
        FavoriteType.VOICE -> colorScheme.onSecondaryContainer
    }
    val gradient = remember(item.id) { avatarGradient(item.id.hashCode()) }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isImage) {
                    Modifier.background(Brush.linearGradient(gradient))
                } else {
                    Modifier.background(container)
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = favoriteTypeIcon(item.type),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(22.dp),
        )
    }
}
