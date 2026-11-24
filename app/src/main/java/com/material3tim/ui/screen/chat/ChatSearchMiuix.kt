package com.material3tim.ui.screen.chat

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
import androidx.compose.foundation.lazy.items
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
import com.material3tim.data.model.ChatMessage
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
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
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 查找聊天记录页（Miuix 风格）：
 * 顶栏返回 + 搜索输入框（自动聚焦、可清空）+ 结果列表 / 加载 / 空 / 失败状态。
 * 搜索逻辑与状态由 [ChatSearchScreen] 统一维护，这里只做展示与回调。
 */
@Composable
internal fun ChatSearchMiuix(
    keyword: String,
    state: ChatSearchUiState,
    onKeywordChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenMessage: (ChatMessage) -> Unit,
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
                    title = stringResource(R.string.chat_search_title),
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
                    ChatSearchFieldMiuix(
                        keyword = keyword,
                        onKeywordChange = onKeywordChange,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        when (state) {
                            ChatSearchUiState.Idle -> {
                                ChatSearchHintMiuix(stringResource(R.string.chat_search_idle))
                            }

                            ChatSearchUiState.Loading -> {
                                ChatSearchHintMiuix(stringResource(R.string.loading))
                            }

                            ChatSearchUiState.Empty -> {
                                ChatSearchHintMiuix(stringResource(R.string.chat_search_record_empty))
                            }

                            ChatSearchUiState.Failed -> {
                                ChatSearchHintMiuix(stringResource(R.string.chat_search_failed))
                            }

                            ChatSearchUiState.HostOnly -> {
                                ChatSearchHintMiuix(stringResource(R.string.chat_search_host_only))
                            }

                            is ChatSearchUiState.Results -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scrollEndHaptic()
                                        .overScrollVertical()
                                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                                    contentPadding = PaddingValues(
                                        start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
                                        end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp,
                                        top = 4.dp,
                                        bottom = navigationBarPadding + 24.dp,
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    overscrollEffect = null,
                                ) {
                                    items(state.messages, key = { it.id }) { message ->
                                        ChatSearchResultRowMiuix(
                                            message = message,
                                            onClick = { onOpenMessage(message) },
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
private fun ChatSearchFieldMiuix(
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
                                text = stringResource(R.string.chat_search_record_hint),
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

@Composable
private fun ChatSearchResultRowMiuix(
    message: ChatMessage,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        showIndication = true,
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chatSearchSenderName(message),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
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
            Spacer(Modifier.height(4.dp))
            Text(
                text = chatSearchMessagePreview(message),
                fontSize = 14.sp,
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ChatSearchHintMiuix(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
    }
}
