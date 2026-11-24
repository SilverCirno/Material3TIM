package com.material3tim.ui.screen.search

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
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.ChatItem
import com.material3tim.data.model.ContactItem
import com.material3tim.data.model.GlobalSearchMessage
import com.material3tim.data.model.GlobalSearchResult
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 全局搜索页（Miuix）：顶栏返回 + 自动聚焦搜索框 + 消息/联系人/群聊分组结果，
 * 以及空闲/加载/无结果/失败/仅宿主可用状态。搜索逻辑与状态由 [GlobalSearchScreen] 统一维护。
 */
@Composable
internal fun GlobalSearchMiuix(
    keyword: String,
    state: GlobalSearchUiState,
    onKeywordChange: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenMessage: (GlobalSearchMessage) -> Unit,
    onOpenContact: (ContactItem) -> Unit,
    onOpenGroup: (ChatItem) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    val enableBlur = LocalEnableBlur.current
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/设备不支持 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.global_search_title),
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
                        if (keyword.isNotEmpty()) {
                            val onClear: () -> Unit = { onKeywordChange("") }
                            HyperTopBarGlassButton(onClick = onClear) {
                                Icon(
                                    imageVector = MiuixIcons.Close,
                                    contentDescription = stringResource(R.string.search_clear),
                                    tint = colorScheme.onSurface,
                                )
                            }
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
                    InputField(
                        query = keyword,
                        onQueryChange = onKeywordChange,
                        label = stringResource(R.string.global_search_hint),
                        leadingIcon = {
                            Icon(
                                imageVector = MiuixIcons.Basic.Search,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .padding(start = 16.dp, end = 8.dp),
                                tint = colorScheme.onSurfaceVariantSummary,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .focusRequester(focusRequester),
                        onSearch = { focusManager.clearFocus() },
                        expanded = false,
                        onExpandedChange = { },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        when (state) {
                            GlobalSearchUiState.Idle -> GlobalSearchHintMiuix(
                                stringResource(R.string.global_search_idle),
                            )
                            GlobalSearchUiState.HostOnly -> GlobalSearchHintMiuix(
                                stringResource(R.string.global_search_host_only),
                            )
                            GlobalSearchUiState.Loading -> GlobalSearchLoadingMiuix()
                            GlobalSearchUiState.Failed -> GlobalSearchFailedMiuix(onRetry = onRetry)
                            GlobalSearchUiState.Empty -> GlobalSearchHintMiuix(
                                stringResource(R.string.global_search_empty),
                            )
                            is GlobalSearchUiState.Results -> GlobalSearchResultsMiuix(
                                result = state.result,
                                listState = listState,
                                scrollBehavior = scrollBehavior,
                                onOpenMessage = onOpenMessage,
                                onOpenContact = onOpenContact,
                                onOpenGroup = onOpenGroup,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalSearchResultsMiuix(
    result: GlobalSearchResult,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    onOpenMessage: (GlobalSearchMessage) -> Unit,
    onOpenContact: (ContactItem) -> Unit,
    onOpenGroup: (ChatItem) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (result.messages.isNotEmpty()) {
            item(key = "section_messages") {
                GlobalSearchSectionHeaderMiuix(
                    text = stringResource(R.string.global_search_section_messages),
                    count = result.messages.size,
                )
            }
            items(
                result.messages,
                key = { "msg_${it.chatId}_${it.msgId}_${it.msgSeq}_${it.timestampMs}" },
            ) { message ->
                GlobalSearchMessageRowMiuix(message = message, onClick = { onOpenMessage(message) })
            }
        }
        if (result.contacts.isNotEmpty()) {
            item(key = "section_contacts") {
                GlobalSearchSectionHeaderMiuix(
                    text = stringResource(R.string.global_search_section_contacts),
                    count = result.contacts.size,
                )
            }
            items(result.contacts, key = { "contact_${it.id}" }) { contact ->
                GlobalSearchContactRowMiuix(contact = contact, onClick = { onOpenContact(contact) })
            }
        }
        if (result.groups.isNotEmpty()) {
            item(key = "section_groups") {
                GlobalSearchSectionHeaderMiuix(
                    text = stringResource(R.string.global_search_section_groups),
                    count = result.groups.size,
                )
            }
            items(result.groups, key = { "group_${it.id}" }) { group ->
                GlobalSearchGroupRowMiuix(group = group, onClick = { onOpenGroup(group) })
            }
        }
    }
}

@Composable
private fun GlobalSearchSectionHeaderMiuix(text: String, count: Int) {
    Text(
        text = "$text ($count)",
        fontSize = 13.sp,
        color = colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun GlobalSearchMessageRowMiuix(
    message: GlobalSearchMessage,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        showIndication = true,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChatAvatar(
                name = message.chatName,
                seed = message.chatName.hashCode(),
                size = 48.dp,
                isGroup = message.isGroup,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.chatName,
                        fontSize = 15.sp,
                        color = colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (message.isGroup) {
                        Text(
                            text = stringResource(R.string.global_search_tag_group),
                            fontSize = 10.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                if (message.senderName.isNotBlank()) {
                    Text(
                        text = message.senderName,
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = message.preview,
                    fontSize = 14.sp,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (message.timeLabel.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message.timeLabel,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun GlobalSearchContactRowMiuix(
    contact: ContactItem,
    onClick: () -> Unit,
) {
    val secondary = contactSearchSecondary(contact)
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        showIndication = true,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChatAvatar(
                name = contact.name,
                seed = contact.avatarSeed,
                size = 44.dp,
                isOnline = contact.isOnline,
                avatarPath = contact.avatarPath,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontSize = 15.sp,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (secondary.isNotBlank()) {
                    Text(
                        text = secondary,
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalSearchGroupRowMiuix(
    group: ChatItem,
    onClick: () -> Unit,
) {
    val groupCode = groupCodeOfChatIdForSearch(group.id)
    val secondary = buildString {
        append(stringResource(R.string.chat_member_count, group.memberCount))
        if (groupCode.isNotBlank()) {
            append(" · ")
            append(groupCode)
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        showIndication = true,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 与消息列表一致的群头像：avatarPath（真实群头像）优先，avatarSeed 渐变回退。
            GlobalSearchChatAvatar(chat = group)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontSize = 15.sp,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = secondary,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun GlobalSearchHintMiuix(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun GlobalSearchLoadingMiuix() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        InfiniteProgressIndicator()
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.loading),
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun GlobalSearchFailedMiuix(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.global_search_failed),
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(
            text = stringResource(R.string.global_search_retry),
            onClick = onRetry,
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}
