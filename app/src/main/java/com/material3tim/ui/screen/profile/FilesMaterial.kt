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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.FileItem
import com.material3tim.data.model.FileKind
import com.material3tim.data.model.StorageInfo
import com.material3tim.data.repository.ContentRepository
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SegmentedColumn
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/**
 * 「我的文件」页（Material 3 Expressive 风格）。
 * 顶部为存储空间概览卡片，文件按「今天 / 昨天 / 更早」分组展示。
 */
@Composable
internal fun FilesMaterial(onBack: () -> Unit) {
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val storage = ContentRepository.storage
    val files = ContentRepository.files
    val todayLabel = stringResource(R.string.files_today)
    val yesterdayLabel = stringResource(R.string.files_yesterday)
    val sections = groupFiles(files, todayLabel, yesterdayLabel)
    val showComingSoon: () -> Unit = {
        Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.files_title)) },
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
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                        24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item(key = "storage") {
                StorageCardMaterial(storage = storage)
            }
            sections.forEach { (section, sectionFiles) ->
                item(key = "section_${section.name}") {
                    Column {
                        Text(
                            text = stringResource(section.labelRes),
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                        )
                        SegmentedColumn {
                            sectionFiles.forEach { file ->
                                item(key = file.id) {
                                    FileRowMaterial(
                                        file = file,
                                        onClick = showComingSoon,
                                        onMoreClick = showComingSoon,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (files.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.files_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 96.dp),
                    )
                }
            }
        }
    }
}

/** 存储空间卡片：标题 + 进度条 + 「已用 X，共 Y」 + 四类分类图例。 */
@Composable
private fun StorageCardMaterial(storage: StorageInfo) {
    val colorScheme = MaterialTheme.colorScheme
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.files_storage),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.files_storage_used,
                        storage.usedText,
                        storage.totalText,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { storage.usedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StorageLegendMaterial(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.files_images),
                    size = storage.imageText,
                    dotColor = colorScheme.primary,
                )
                StorageLegendMaterial(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.files_videos),
                    size = storage.videoText,
                    dotColor = colorScheme.tertiary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StorageLegendMaterial(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.files_docs),
                    size = storage.docText,
                    dotColor = colorScheme.secondary,
                )
                StorageLegendMaterial(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.files_others),
                    size = storage.otherText,
                    dotColor = colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun StorageLegendMaterial(
    label: String,
    size: String,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = size,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 单条文件行：类型图标 + 文件名 + 大小/时间/来源 + 更多按钮。 */
@Composable
private fun FileRowMaterial(
    file: FileItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        headlineContent = {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = fileMeta(file),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = { FileKindBadgeMaterial(kind = file.kind) },
        trailingContent = {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun FileKindBadgeMaterial(kind: FileKind) {
    val colorScheme = MaterialTheme.colorScheme
    val container = when (kind) {
        FileKind.DOC -> colorScheme.primaryContainer
        FileKind.PDF -> colorScheme.errorContainer
        FileKind.IMAGE -> colorScheme.tertiaryContainer
        FileKind.VIDEO -> colorScheme.secondaryContainer
        FileKind.ZIP -> colorScheme.surfaceContainerHighest
        FileKind.APK -> colorScheme.primaryContainer
        FileKind.MUSIC -> colorScheme.secondaryContainer
        FileKind.OTHER -> colorScheme.surfaceContainerHighest
    }
    val content = when (kind) {
        FileKind.DOC -> colorScheme.onPrimaryContainer
        FileKind.PDF -> colorScheme.onErrorContainer
        FileKind.IMAGE -> colorScheme.onTertiaryContainer
        FileKind.VIDEO -> colorScheme.onSecondaryContainer
        FileKind.ZIP -> colorScheme.onSurfaceVariant
        FileKind.APK -> colorScheme.onPrimaryContainer
        FileKind.MUSIC -> colorScheme.onSecondaryContainer
        FileKind.OTHER -> colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = fileKindIcon(kind),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
    }
}
