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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.FavoriteItem
import com.material3tim.data.model.FavoriteType
import com.material3tim.data.repository.ContentRepository
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/**
 * 「我的收藏」页（Material 3 Expressive 风格）。
 * 顶部筛选条只在本地生效，示例数据来自 [ContentRepository.favorites]。
 */
@Composable
internal fun FavoritesMaterial(onBack: () -> Unit) {
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val favorites = ContentRepository.favorites
    val filters = FavoriteFilter.entries
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filtered = remember(selectedFilterIndex) {
        val type = filters[selectedFilterIndex].type
        if (type == null) favorites else favorites.filter { it.type == type }
    }
    val showComingSoon: () -> Unit = {
        Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.favorites_title)) },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                        24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "filters") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(filters) { index, filter ->
                        FilterChip(
                            selected = index == selectedFilterIndex,
                            onClick = { selectedFilterIndex = index },
                            label = { Text(stringResource(filter.labelRes)) },
                        )
                    }
                }
            }
            if (filtered.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 96.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.StarBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.favorites_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    FavoriteCardMaterial(item = item, onClick = showComingSoon)
                }
            }
        }
    }
}

/** 单条收藏卡片：类型缩略图 + 标题 + 摘要 + 来源/时间等信息。 */
@Composable
private fun FavoriteCardMaterial(
    item: FavoriteItem,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val voiceLabel = if (item.type == FavoriteType.VOICE && item.durationSeconds > 0) {
        stringResource(R.string.voice_duration, item.durationSeconds)
    } else {
        null
    }
    TonalCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            FavoriteThumbMaterial(item = item)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.snippet.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = favoriteMeta(item, voiceLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 收藏类型缩略图：图片类型用渐变占位图，其它类型用主题色底的图标。 */
@Composable
private fun FavoriteThumbMaterial(item: FavoriteItem) {
    val colorScheme = MaterialTheme.colorScheme
    val isImage = item.type == FavoriteType.IMAGE
    val container = when (item.type) {
        FavoriteType.TEXT -> colorScheme.secondaryContainer
        FavoriteType.LINK -> colorScheme.tertiaryContainer
        FavoriteType.IMAGE -> Color.Transparent
        FavoriteType.FILE -> colorScheme.surfaceContainerHighest
        FavoriteType.VOICE -> colorScheme.primaryContainer
    }
    val content = when (item.type) {
        FavoriteType.TEXT -> colorScheme.onSecondaryContainer
        FavoriteType.LINK -> colorScheme.onTertiaryContainer
        FavoriteType.IMAGE -> Color.White
        FavoriteType.FILE -> colorScheme.onSurfaceVariant
        FavoriteType.VOICE -> colorScheme.onPrimaryContainer
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
