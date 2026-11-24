package com.material3tim.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.ContactItem
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.groupedCardItems
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 创建群聊选人页（Miuix 风格）：
 * 顶栏返回 + 标题 + 右侧「确认(N)」（未选中时禁用）；
 * 联系人列表为 HyperOS4 分段卡片（头像 + 昵称/签名 + 右侧 Checkbox），点整行切换选中。
 *
 * Phase 2e：玻璃/纯色自适应顶栏 + 页面级 backdrop + CardSegment 分段卡片。
 */
@Composable
internal fun CreateGroupMiuix(
    contacts: List<ContactItem>,
    loading: Boolean,
    selectedIds: Set<String>,
    creating: Boolean,
    onToggle: (ContactItem) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    val enableBlur = LocalEnableBlur.current
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/设备不支持 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)
    val layoutDirection = LocalLayoutDirection.current
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val canConfirm = selectedIds.isNotEmpty() && !creating

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.home_plus_create_group),
                    scrollBehavior = scrollBehavior,
                    backdrop = backdrop,
                    isContentScrolled = listState.canScrollBackward,
                    navigationIcon = {
                        // 顶栏按钮跟随顶栏风格：HyperTopBarGlassButton 自动选择玻璃圆钮 / 普通 IconButton。
                        HyperTopBarGlassButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.chat_detail_back),
                                tint = colorScheme.onSurface,
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            text = stringResource(R.string.create_group_confirm_count, selectedIds.size),
                            onClick = onConfirm,
                            enabled = canConfirm,
                        )
                    },
                )
            },
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 采样层要早于 inset/padding：记录不透明底 + 页面内容（不含玻璃顶栏自身）。
                    .then(
                        if (backdrop != null) {
                            Modifier
                                .layerBackdrop(backdrop)
                                .background(colorScheme.surface)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                when {
                    loading -> {
                        CreateGroupHintMiuix(
                            text = stringResource(R.string.loading),
                            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                        )
                    }

                    contacts.isEmpty() -> {
                        CreateGroupHintMiuix(
                            text = stringResource(R.string.create_group_empty),
                            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                        )
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .scrollEndHaptic()
                                .overScrollVertical()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = PaddingValues(
                                start = innerPadding.calculateStartPadding(layoutDirection),
                                end = innerPadding.calculateEndPadding(layoutDirection),
                                top = innerPadding.calculateTopPadding() + 6.dp,
                                bottom = navigationBarPadding + 24.dp,
                            ),
                            overscrollEffect = null,
                        ) {
                            // HyperOS4 分组卡片：选人行动画统一为 squircle 分段卡片。
                            groupedCardItems(
                                keyPrefix = "createGroup",
                                outerTopPadding = 0.dp,
                                outerBottomPadding = 6.dp,
                                items = contacts.map { contact ->
                                    CardItem(contact.id) {
                                        CreateGroupContactRowMiuix(
                                            contact = contact,
                                            checked = contact.id in selectedIds,
                                            onToggle = { onToggle(contact) },
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 联系人选择行：头像 + 昵称/签名 + 右侧 Checkbox，整行点击切换选中（背景由 CardSegment 提供）。 */
@Composable
private fun CreateGroupContactRowMiuix(
    contact: ContactItem,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatAvatar(
            name = contact.name,
            seed = contact.avatarSeed,
            isOnline = contact.isOnline,
            avatarPath = contact.avatarPath,
            modifier = Modifier.padding(end = 10.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                fontWeight = FontWeight(550),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (contact.signature.isNotBlank()) {
                Text(
                    text = contact.signature,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Checkbox(
            state = if (checked) ToggleableState.On else ToggleableState.Off,
            onClick = onToggle,
        )
    }
}

@Composable
private fun CreateGroupHintMiuix(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
    }
}
