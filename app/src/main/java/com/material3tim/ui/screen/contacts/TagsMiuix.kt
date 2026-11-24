package com.material3tim.ui.screen.contacts

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.TagGroup
import com.material3tim.data.repository.ContentRepository
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.groupedCardItems
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Icon
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
 * 标签页 Miuix 实现：
 * 新建标签入口 + 标签 HyperOS4 分段卡片（名称、人数、FlowRow 换行成员 chip）。
 */
@Composable
fun TagsMiuix(onBack: () -> Unit) {
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val showComingSoon: () -> Unit = {
        Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
    }
    val tags = ContentRepository.tags

    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberPageBackdrop(enableBlur)

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.tags_title),
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
                // HyperOS4 分组卡片：新建标签入口单独一张 squircle 卡片。
                groupedCardItems(
                    keyPrefix = "newTag",
                    outerTopPadding = 12.dp,
                    outerBottomPadding = 6.dp,
                    items = listOf(
                        CardItem("new") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = showComingSoon)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.tags_new),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface,
                                )
                            }
                        },
                    ),
                )
                if (tags.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.tags_empty),
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                } else {
                    groupedCardItems(
                        keyPrefix = "tags",
                        outerTopPadding = 0.dp,
                        outerBottomPadding = 6.dp,
                        items = tags.map { tag ->
                            CardItem(tag.id) {
                                TagSegmentMiuix(tag = tag, onClick = showComingSoon)
                            }
                        },
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

/** 单个标签分段内容：名称 + 人数 + 成员 chip 自动换行。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSegmentMiuix(
    tag: TagGroup,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tag.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.tags_summary, tag.memberNames.size),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
        }
        if (tag.memberNames.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tag.memberNames.forEach { name ->
                    TagMemberChipMiuix(name = name)
                }
            }
        }
    }
}

@Composable
private fun TagMemberChipMiuix(name: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colorScheme.surfaceContainerHigh)
            .padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatAvatar(
            name = name,
            seed = tagMemberAvatarSeed(name),
            size = 20.dp,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            color = colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
