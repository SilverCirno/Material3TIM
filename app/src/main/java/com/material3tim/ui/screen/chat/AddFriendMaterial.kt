package com.material3tim.ui.screen.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.AddFriendGroup
import com.material3tim.data.model.AddFriendUser
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SegmentedColumn
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/**
 * 加好友/群搜索页（Material 3 Expressive 风格）：
 * 顶栏返回 + 搜索框（自动聚焦、可清空）+ 结果列表。
 * 结果按「个人」「群聊」分成两块：每块是 section header（联系人页字母分组同款样式）
 * 加一张 SegmentedList 卡片；某类无结果时不显示该卡片。
 */
@Composable
internal fun AddFriendMaterial(
    keyword: String,
    state: AddFriendUiState,
    onKeywordChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenVerify: (AddFriendUser) -> Unit,
    onJoinGroup: (AddFriendGroup) -> Unit,
) {
    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_plus_add_friend),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    TopBarBackButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.chat_detail_back),
                    )
                },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            AddFriendSearchFieldMaterial(
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
                        AddFriendHintMaterial(stringResource(R.string.add_friend_idle))
                    }

                    AddFriendUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    AddFriendUiState.Empty -> {
                        AddFriendHintMaterial(stringResource(R.string.add_friend_empty))
                    }

                    AddFriendUiState.Failed -> {
                        AddFriendHintMaterial(stringResource(R.string.add_friend_failed))
                    }

                    AddFriendUiState.HostOnly -> {
                        AddFriendHintMaterial(stringResource(R.string.add_friend_host_only))
                    }

                    is AddFriendUiState.Results -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 24.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (state.result.users.isNotEmpty()) {
                                item(key = "users") {
                                    SegmentedColumn(
                                        title = stringResource(R.string.add_friend_section_users),
                                        content = state.result.users.map { user ->
                                            @Composable {
                                                AddFriendUserRowMaterial(
                                                    user = user,
                                                    onClick = { onOpenVerify(user) },
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                            if (state.result.groups.isNotEmpty()) {
                                item(key = "groups") {
                                    SegmentedColumn(
                                        title = stringResource(R.string.add_friend_section_groups),
                                        content = state.result.groups.map { group ->
                                            @Composable {
                                                AddFriendGroupRowMaterial(
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

@Composable
private fun AddFriendSearchFieldMaterial(
    keyword: String,
    onKeywordChange: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    // 进页面自动聚焦并弹出键盘，省一次点击。
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(44.dp),
        shape = RoundedCornerShape(50),
        color = colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
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
                    if (keyword.isEmpty()) {
                        Text(
                            text = stringResource(R.string.add_friend_search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                },
            )
            if (keyword.isNotEmpty()) {
                IconButton(
                    onClick = { onKeywordChange("") },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.search_clear),
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** 个人账号行：消息列表那种「头像 + 名称（weight 撑满）+ 详情」+ 右侧箭头。 */
@Composable
private fun AddFriendUserRowMaterial(user: AddFriendUser, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    SegmentedListItem(
        onClick = onClick,
        leadingContent = {
            ChatAvatar(
                name = user.name.ifBlank { user.uid },
                seed = user.uin.hashCode(),
                size = 44.dp,
                avatarPath = user.avatarPath,
            )
        },
        headlineContent = {
            Text(
                text = user.name,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = user.detail.ifBlank {
                    stringResource(R.string.chat_info_qq_number, user.uin.toString())
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
            )
        },
    )
}

/** 群聊行：同个人账号行，群头像 + 名称 + 群号/简介。 */
@Composable
private fun AddFriendGroupRowMaterial(group: AddFriendGroup, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    SegmentedListItem(
        onClick = onClick,
        leadingContent = {
            ChatAvatar(
                name = group.name,
                seed = group.groupCode.hashCode(),
                size = 44.dp,
                isGroup = true,
                avatarPath = group.avatarPath,
            )
        },
        headlineContent = {
            Text(
                text = group.name,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = group.detail.ifBlank {
                    stringResource(R.string.chat_info_group_number, group.groupCode.toString())
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun AddFriendHintMaterial(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
