package com.material3tim.ui.screen.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.FileItem
import com.material3tim.data.model.FileKind
import com.material3tim.data.model.StorageInfo
import com.material3tim.data.repository.ContentRepository
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.groupedCardItems
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 「我的文件」页（Miuix 风格）。
 * 顶部为存储空间概览卡片，文件按「今天 / 昨天 / 更早」分组展示为 HyperOS4 分段卡片。
 */
@Composable
internal fun FilesMiuix(onBack: () -> Unit) {
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

    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val enableBlur = LocalEnableBlur.current
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/无 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.files_title),
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
                item(key = "storage") {
                    StorageCardMiuix(
                        storage = storage,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp, bottom = 6.dp),
                    )
                }
                sections.forEach { (section, sectionFiles) ->
                    item(key = "header_${section.name}") {
                        Text(
                            text = stringResource(section.labelRes),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 6.dp),
                        )
                    }
                    // HyperOS4 分组卡片：同一时间分组的文件合并为 squircle 分段卡片。
                    groupedCardItems(
                        keyPrefix = "files_${section.name}",
                        outerTopPadding = 0.dp,
                        outerBottomPadding = 6.dp,
                        items = sectionFiles.map { file ->
                            CardItem(file.id) {
                                FileRowMiuix(
                                    file = file,
                                    onClick = showComingSoon,
                                    onMoreClick = showComingSoon,
                                )
                            }
                        },
                    )
                }
                if (files.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.files_empty),
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 96.dp),
                        )
                    }
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

/** 存储空间卡片：标题 + 进度条 + 「已用 X，共 Y」 + 四类分类图例。 */
@Composable
private fun StorageCardMiuix(
    storage: StorageInfo,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.files_storage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.files_storage_used,
                        storage.usedText,
                        storage.totalText,
                    ),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorScheme.secondaryContainer),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(storage.usedFraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(colorScheme.primary),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StorageLegendMiuix(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.files_images),
                    size = storage.imageText,
                    dotColor = colorScheme.primary,
                )
                StorageLegendMiuix(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.files_videos),
                    size = storage.videoText,
                    dotColor = colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StorageLegendMiuix(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.files_docs),
                    size = storage.docText,
                    dotColor = colorScheme.primaryVariant,
                )
                StorageLegendMiuix(
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
private fun StorageLegendMiuix(
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
            fontSize = 13.sp,
            color = colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = size,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
        )
    }
}

/** 单条文件行：类型图标 + 文件名 + 大小/时间/来源 + 更多按钮。 */
@Composable
private fun FileRowMiuix(
    file: FileItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileKindBadgeMiuix(kind = file.kind)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontSize = 15.sp,
                color = colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = fileMeta(file),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun FileKindBadgeMiuix(kind: FileKind) {
    val container = when (kind) {
        FileKind.DOC -> colorScheme.primaryContainer
        FileKind.PDF -> colorScheme.errorContainer
        FileKind.IMAGE -> colorScheme.secondaryContainer
        FileKind.VIDEO -> colorScheme.primaryContainer
        FileKind.ZIP -> colorScheme.surfaceContainerHigh
        FileKind.APK -> colorScheme.secondaryContainer
        FileKind.MUSIC -> colorScheme.primaryContainer
        FileKind.OTHER -> colorScheme.surfaceContainerHigh
    }
    val content = when (kind) {
        FileKind.DOC -> colorScheme.onPrimaryContainer
        FileKind.PDF -> colorScheme.onErrorContainer
        FileKind.IMAGE -> colorScheme.onSecondaryContainer
        FileKind.VIDEO -> colorScheme.onPrimaryContainer
        FileKind.ZIP -> colorScheme.onSurfaceContainerHigh
        FileKind.APK -> colorScheme.onSecondaryContainer
        FileKind.MUSIC -> colorScheme.onPrimaryContainer
        FileKind.OTHER -> colorScheme.onSurfaceContainerHigh
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
