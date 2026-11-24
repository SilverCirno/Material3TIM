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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.AddFriendGroup
import com.material3tim.data.model.AddFriendUser
import com.material3tim.ui.component.ChatAvatar
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
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 加好友/群搜索页（Miuix 风格）：
 * 顶栏返回 + 搜索框（自动聚焦、可清空）+ 结果列表。
 * 结果按「个人」「群聊」分块：每块用联系人页字母分组同款 header + Card 行；
 * 某类无结果时不显示该块。
 */
@Composable
internal fun AddFriendMiuix(
    keyword: String,
    state: AddFriendUiState,
    onKeywordChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenVerify: (AddFriendUser) -> Unit,
    onJoinGroup: (AddFriendGroup) -> Unit,
) {
    val enableBlur = LocalEnableBlur.current
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/设备不支持 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)
    val layoutDirection = LocalLayoutDirection.current
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.home_plus_add_friend),
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                        .imePadding(),
                ) {
                    AddFriendSearchFieldMiuix(
                        keyword = keyword,
                        onKeywordChange = onKeywordChange,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        when (state) {
                            AddFriendUiState.Idle -> {
                                AddFriendHintMiuix(stringResource(R.string.add_friend_idle))
                            }

                            AddFriendUiState.Loading -> {
                                AddFriendHintMiuix(stringResource(R.string.loading))
                            }

                            AddFriendUiState.Empty -> {
                                AddFriendHintMiuix(stringResource(R.string.add_friend_empty))
                            }

                            AddFriendUiState.Failed -> {
                                AddFriendHintMiuix(stringResource(R.string.add_friend_failed))
                            }

                            AddFriendUiState.HostOnly -> {
                                AddFriendHintMiuix(stringResource(R.string.add_friend_host_only))
                            }

                            is AddFriendUiState.Results -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scrollEndHaptic()
                                        .overScrollVertical()
                                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                                    contentPadding = PaddingValues(
                                        // 横向 12dp 由 CardSegment 的 outerHorizontalPadding 提供，这里只留 inset。
                                        start = innerPadding.calculateStartPadding(layoutDirection),
                                        end = innerPadding.calculateEndPadding(layoutDirection),
                                        top = 4.dp,
                                        bottom = navigationBarPadding + 24.dp,
                                    ),
                                    // 分段卡片由 CardSegment 自带首尾圆角与外边距，不能再加逐项间距（否则黑缝被拆开）。
                                    overscrollEffect = null,
                                ) {
                                    if (state.result.users.isNotEmpty()) {
                                        item(key = "users_header") {
                                            AddFriendSectionHeaderMiuix(
                                                text = stringResource(R.string.add_friend_section_users),
                                            )
                                        }
                                        // HyperOS4 分组卡片：个人结果合并为 squircle 分段卡片。
                                        groupedCardItems(
                                            keyPrefix = "addFriendUsers",
                                            outerTopPadding = 0.dp,
                                            outerBottomPadding = 6.dp,
                                            items = state.result.users.map { user ->
                                                CardItem("user_${user.uid}") {
                                                    AddFriendUserRowMiuix(
                                                        user = user,
                                                        onClick = { onOpenVerify(user) },
                                                    )
                                                }
                                            },
                                        )
                                    }
                                    if (state.result.groups.isNotEmpty()) {
                                        item(key = "groups_header") {
                                            AddFriendSectionHeaderMiuix(
                                                text = stringResource(R.string.add_friend_section_groups),
                                            )
                                        }
                                        // HyperOS4 分组卡片：群聊结果合并为 squircle 分段卡片。
                                        groupedCardItems(
                                            keyPrefix = "addFriendGroups",
                                            outerTopPadding = 0.dp,
                                            outerBottomPadding = 6.dp,
                                            items = state.result.groups.map { group ->
                                                CardItem("group_${group.groupCode}") {
                                                    AddFriendGroupRowMiuix(
                                                        group = group,
                                                        onClick = { onJoinGroup(group) },
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
        }
    }
}

@Composable
private fun AddFriendSearchFieldMiuix(
    keyword: String,
    onKeywordChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    // 进页面自动聚焦并弹出键盘，省一次点击。
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MiuixIcons.Basic.Search,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(fontSize = 15.sp, color = colorScheme.onSurface),
                singleLine = true,
                cursorBrush = SolidColor(colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (keyword.isEmpty()) {
                            Text(
                                text = stringResource(R.string.add_friend_search_hint),
                                fontSize = 15.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (keyword.isNotEmpty()) {
                IconButton(
                    onClick = { onKeywordChange("") },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Basic.SearchCleanup,
                        contentDescription = stringResource(R.string.search_clear),
                        tint = colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** 分组标题：联系人页 A B C 字母分组同款样式（这里写「个人」/「群聊」）。 */
@Composable
private fun AddFriendSectionHeaderMiuix(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 6.dp),
    )
}

/** 个人账号行：消息列表那种「头像 + 名称/详情 + 箭头」Card。 */
@Composable
private fun AddFriendUserRowMiuix(user: AddFriendUser, onClick: () -> Unit) {
    // 背景/圆角由 CardSegment 提供，这里只保留可点击内容行。
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
            ChatAvatar(
                name = user.name.ifBlank { user.uid },
                seed = user.uin.hashCode(),
                isGroup = false,
                avatarPath = user.avatarPath,
                modifier = Modifier.padding(end = 10.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = user.detail.ifBlank {
                        stringResource(R.string.chat_info_qq_number, user.uin.toString())
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 8.dp),
            )
    }
}

/** 群聊行：群头像 + 群名称/群号，点击申请加群。 */
@Composable
private fun AddFriendGroupRowMiuix(group: AddFriendGroup, onClick: () -> Unit) {
    // 背景/圆角由 CardSegment 提供，这里只保留可点击内容行。
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
            ChatAvatar(
                name = group.name,
                seed = group.groupCode.hashCode(),
                isGroup = true,
                avatarPath = group.avatarPath,
                modifier = Modifier.padding(end = 10.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = group.detail.ifBlank {
                        stringResource(R.string.chat_info_group_number, group.groupCode.toString())
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 8.dp),
            )
    }
}

@Composable
private fun AddFriendHintMiuix(text: String) {
    Box(
        modifier = Modifier
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
