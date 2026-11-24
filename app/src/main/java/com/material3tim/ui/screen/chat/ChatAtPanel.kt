package com.material3tim.ui.screen.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.ChatMember
import com.material3tim.data.source.AtTarget
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.material.SegmentedItem
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.screen.contacts.ContactsLetterHeaderMaterial
import com.material3tim.ui.screen.contacts.ContactsLetterHeaderMiuix
import android.os.Build
import android.view.View
import android.window.OnBackAnimationCallback
import android.window.BackEvent
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import com.material3tim.ui.component.miuix.glass.GlassSheet

/**
 * 群聊 @ 成员选择底部 Sheet（双风格，视觉复用联系人页）。
 *
 * 内容：标题 + 联系人页同款搜索框 + 仿联系人页的首字母分组成员列表（多选勾选）
 * + 「全体成员」第一项（与成员互斥） + 底部取消/确定(n)。
 * 确认时按成员列表顺序生成多个 [AtTarget]，由 ChatComposerState.selectAts 一次性插入输入框。
 */

/** 「全体成员」特殊选中 key（与成员勾选互斥）。 */
private const val AllMembersKey = "__at_all__"

/** 成员的唯一 key：优先内核 uid，其次 QQ 号，最后昵称。 */
private fun memberSelectKey(member: ChatMember): String =
    member.uid.ifBlank { member.uin.ifBlank { member.name } }

/** 选中集合 → @ 目标：全体优先；否则按成员列表原顺序。 */
private fun buildAtTargets(
    members: List<ChatMember>,
    selectedKeys: Set<String>,
    allLabel: String,
): List<AtTarget> {
    if (AllMembersKey in selectedKeys) {
        return listOf(AtTarget(display = "@$allLabel", uid = "", isAll = true))
    }
    return members
        .filter { memberSelectKey(it) in selectedKeys }
        .map { member ->
            AtTarget(
                display = "@${member.name}",
                uid = member.uid.ifBlank { member.uin },
                isAll = false,
            )
        }
}

/** 成员徽标（群主 / 管理员 / 我，多个用「 · 」连接）。 */
@Composable
private fun memberBadge(member: ChatMember): String {
    val resources = mutableListOf<String>()
    if (member.isOwner) resources += stringResource(R.string.chat_at_owner)
    if (member.isAdmin && !member.isOwner) resources += stringResource(R.string.chat_at_admin)
    if (member.isMe) resources += stringResource(R.string.chat_at_me)
    return resources.joinToString(" · ")
}

/** 搜索过滤 + 首字母分组（复用联系人页的 filterMembers / groupMembersByLetter）。 */
private fun filteredGroups(
    members: List<ChatMember>,
    query: String,
): List<Pair<Char, List<ChatMember>>> = groupMembersByLetter(filterMembers(members, query))

/** Material 3 Expressive 底部 Sheet：标准 material3 [ModalBottomSheet]（大屏限宽/inset/scrim/拖拽由组件处理）。 */
@Suppress("DEPRECATION") // rememberModalBottomSheetState 在 material3 alpha28 仍是稳定用法
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatAtSheetMaterial(
    visible: Boolean,
    members: List<ChatMember>,
    onDismiss: () -> Unit,
    onConfirm: (List<AtTarget>) -> Unit,
    loading: Boolean = false,
    failed: Boolean = false,
    onRetry: () -> Unit = {},
) {
    if (!visible) return
    val allLabel = stringResource(R.string.chat_at_all)
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    val groups = remember(members, query) { filteredGroups(members, query) }
    val selectedCount = if (AllMembersKey in selectedKeys) 1 else selectedKeys.size
    val allSelected = AllMembersKey in selectedKeys
    val toggleAll: () -> Unit = {
        selectedKeys = if (AllMembersKey in selectedKeys) emptySet() else setOf(AllMembersKey)
    }
    val toggleMember: (String) -> Unit = { key ->
        selectedKeys = if (key in selectedKeys) {
            selectedKeys - key
        } else {
            (selectedKeys - AllMembersKey) + key
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 预测性返回跟手进度：0 = 正常位置，1 = 完全拖出屏幕；驱动内容根 translationY。
    val backProgress = remember { Animatable(0f) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // 背景透明 + 去掉默认拖拽条：内容根自己画 Surface（顶部圆角 + 自绘拖拽条），
        // scrim / 大屏限宽 / insets 仍由 ModalBottomSheet 处理。
        containerColor = Color.Transparent,
        dragHandle = null,
        // 显式开启返回键关闭（兜底：若 Dialog 窗口返回回调注册失败，仍能按返回关闭）。
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true,
            shouldDismissOnClickOutside = true,
        ),
        // 去掉组件默认窗口 inset：Sheet 背景延伸到导航栏（沉浸化），
        // 内容底部按钮用 atSheetBottomInset() 自己避开安全区。
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        // 在 Dialog 窗口注册返回回调，把预测性返回进度映射到内容根位移。
        DialogBackDispatchEffect(
            sheetState = sheetState,
            onBackProgress = { progress ->
                scope.launch { backProgress.snapTo(progress.coerceIn(0f, 1f)) }
            },
            onBackCancelled = {
                scope.launch { backProgress.animateTo(0f, tween(durationMillis = 200)) }
            },
            onDismiss = onDismiss,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .graphicsLayer { translationY = backProgress.value * size.height }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            // 自绘顶部拖拽条
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // 使用 material3 官方 DragHandle（32×4dp、官方间距/颜色），不再自绘。
                BottomSheetDefaults.DragHandle()
            }
            // 与消息列表顶部一致的胶囊搜索框：点击即聚焦并弹出输入法，输入即时过滤。
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 8.dp)
                    .height(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (query.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.chat_at_search_hint),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            },
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            // 列表：与联系人页相同的 SegmentedList 行 + 字母分组头（选中态靠 checked 的圆角/背景色）
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                if (members.isEmpty() && loading) {
                    item(key = "at_loading") {
                        Text(
                            text = stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, top = 16.dp),
                        )
                    }
                } else if (members.isEmpty() && failed) {
                    item(key = "at_failed") {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text(
                                text = stringResource(R.string.group_files_load_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                            TextButton(onClick = onRetry) {
                                Text(text = stringResource(R.string.group_files_retry))
                            }
                        }
                    }
                } else {
                item(key = "__all_members__") {
                    SegmentedItem(index = 0, count = 1) {
                        SegmentedListItem(
                            checked = allSelected,
                            onCheckedChange = { toggleAll() },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(24.dp),
                                )
                            },
                            headlineContent = {
                                Text(
                                    text = allLabel,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
                groups.forEach { (letter, groupMembers) ->
                    item(key = "at_letter_$letter") {
                        ContactsLetterHeaderMaterial(letter)
                    }
                    itemsIndexed(
                        items = groupMembers,
                        key = { _, member -> "at_${memberSelectKey(member)}" },
                    ) { index, member ->
                        val key = memberSelectKey(member)
                        val badge = memberBadge(member)
                        SegmentedItem(index = index, count = groupMembers.size) {
                            SegmentedListItem(
                                checked = key in selectedKeys,
                                onCheckedChange = { toggleMember(key) },
                                leadingContent = {
                                    ChatAvatar(
                                        name = member.name,
                                        seed = member.avatarSeed,
                                        size = 44.dp,
                                        avatarPath = member.avatarPath,
                                    )
                                },
                                headlineContent = {
                                    Text(
                                        text = member.name,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                supportingContent = if (badge.isEmpty()) {
                                    null
                                } else {
                                    { Text(text = badge, maxLines = 1) }
                                },
                            )
                        }
                    }
                }
                if (groups.isEmpty()) {
                    item(key = "at_empty") {
                        Text(
                            text = stringResource(R.string.chat_at_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, top = 16.dp),
                        )
                    }
                }
                }
            }
            // 底部取消 / 确定(n)：背景延伸到导航栏，但按钮自身避开安全区。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp + atSheetBottomInset(),
                    ),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(buildAtTargets(members, selectedKeys, allLabel)) },
                    enabled = selectedCount > 0,
                ) {
                    Text(text = stringResource(R.string.chat_at_confirm_count, selectedCount))
                }
            }
        }
    }
}

/** Miuix 底部 Sheet：搜索框与 Card 列表视觉与联系人页一致。 */
@Composable
internal fun ChatAtSheetMiuix(
    visible: Boolean,
    members: List<ChatMember>,
    onDismiss: () -> Unit,
    onConfirm: (List<AtTarget>) -> Unit,
    loading: Boolean = false,
    failed: Boolean = false,
    onRetry: () -> Unit = {},
) {
    val allLabel = stringResource(R.string.chat_at_all)
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    val groups = remember(members, query) { filteredGroups(members, query) }
    val selectedCount = if (AllMembersKey in selectedKeys) 1 else selectedKeys.size
    val allSelected = AllMembersKey in selectedKeys
    val toggleAll: () -> Unit = {
        selectedKeys = if (AllMembersKey in selectedKeys) emptySet() else setOf(AllMembersKey)
    }
    val toggleMember: (String) -> Unit = { key ->
        selectedKeys = if (key in selectedKeys) {
            selectedKeys - key
        } else {
            (selectedKeys - AllMembersKey) + key
        }
    }
    GlassSheet(
        show = visible,
        onDismissRequest = onDismiss,
        // 保持迁移前 OverlayBottomSheet 的内容内边距（横向 24dp），列表/搜索框位置不变。
        insideMargin = DpSize(24.dp, 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
        ) {
            // 联系人页同款 Miuix InputField 搜索框
            InputField(
                query = query,
                onQueryChange = { query = it },
                label = stringResource(R.string.chat_at_search_hint),
                leadingIcon = {
                    MiuixIcon(
                        imageVector = MiuixIcons.Basic.Search,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .padding(start = 16.dp, end = 8.dp),
                        tint = colorScheme.onSecondaryContainer,
                    )
                },
                // 搜索框背景与下方成员卡片保持一致（secondaryContainer）。
                color = colorScheme.secondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp, bottom = 6.dp),
                onSearch = { focusManager.clearFocus() },
                expanded = false,
                onExpandedChange = { },
            )
            // 列表：与联系人页相同的 Card 行 + 字母分组头
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 6.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                if (members.isEmpty() && loading) {
                    item(key = "at_loading") {
                        MiuixText(
                            text = stringResource(R.string.loading),
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp),
                        )
                    }
                } else if (members.isEmpty() && failed) {
                    item(key = "at_failed") {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            MiuixText(
                                text = stringResource(R.string.group_files_load_failed),
                                fontSize = 13.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(start = 20.dp),
                            )
                            MiuixTextButton(
                                text = stringResource(R.string.group_files_retry),
                                onClick = onRetry,
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                            )
                        }
                    }
                } else {
                item(key = "__all_members__") {
                    MiuixAtCard(
                        selected = allSelected,
                        title = allLabel,
                        badge = "",
                        onToggle = toggleAll,
                        leading = {
                            MiuixIcon(
                                imageVector = Icons.Filled.Groups,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .size(28.dp),
                            )
                        },
                    )
                }
                groups.forEach { (letter, groupMembers) ->
                    item(key = "at_letter_$letter") {
                        ContactsLetterHeaderMiuix(letter)
                    }
                    itemsIndexed(
                        items = groupMembers,
                        key = { _, member -> "at_${memberSelectKey(member)}" },
                    ) { _, member ->
                        val key = memberSelectKey(member)
                        MiuixAtCard(
                            selected = key in selectedKeys,
                            title = member.name,
                            badge = memberBadge(member),
                            onToggle = { toggleMember(key) },
                            leading = {
                                ChatAvatar(
                                    name = member.name,
                                    seed = member.avatarSeed,
                                    avatarPath = member.avatarPath,
                                    modifier = Modifier.padding(end = 10.dp),
                                )
                            },
                        )
                    }
                }
                if (groups.isEmpty()) {
                    item(key = "at_empty") {
                        MiuixText(
                            text = stringResource(R.string.chat_at_empty),
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp),
                        )
                    }
                }
                }
            }
            // 底部取消 / 确定(n)：玻璃 Sheet 上使用 GlassDialogButton（Normal / Primary）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp + atSheetBottomInset(),
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassDialogButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    MiuixText(
                        text = stringResource(R.string.cancel),
                        style = MiuixTheme.textStyles.button,
                    )
                }
                GlassDialogButton(
                    onClick = { onConfirm(buildAtTargets(members, selectedKeys, allLabel)) },
                    modifier = Modifier.weight(1f),
                    enabled = selectedCount > 0,
                    role = GlassDialogButtonRole.Primary,
                ) {
                    MiuixText(
                        text = stringResource(R.string.chat_at_confirm_count, selectedCount),
                        style = MiuixTheme.textStyles.button,
                    )
                }
            }
        }
    }
}

/** Miuix 联系人页同款 Card 行（多选勾选）。 */
@Composable
private fun MiuixAtCard(
    selected: Boolean,
    title: String,
    badge: String,
    onToggle: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 6.dp),
        onClick = onToggle,
        showIndication = true,
        // 对齐 Miuix 官方示例（example/component/BottomSheetSection.kt）：BottomSheet 内的
        // Card 只覆盖 color = secondaryContainer（contentColor 与文字颜色沿用 Miuix 默认）。
        colors = CardDefaults.defaultColors(
            color = colorScheme.secondaryContainer,
        ),
        insideMargin = PaddingValues(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading()
            Column(modifier = Modifier.weight(1f)) {
                MiuixText(
                    text = title,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                )
                if (badge.isNotEmpty()) {
                    MiuixText(
                        text = badge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight(550),
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            MiuixCheckbox(
                state = if (selected) ToggleableState.On else ToggleableState.Off,
                onClick = onToggle,
            )
        }
    }
}

/** 底部动作行安全区：键盘收起时避开系统导航栏/小白条；键盘弹出时由 imePadding 负责，避免重复留白。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun atSheetBottomInset(): Dp =
    if (WindowInsets.isImeVisible) {
        0.dp
    } else {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    }

/**
 * 在 Dialog 窗口注册返回回调，让标准 ModalBottomSheet 也能跟手预测性返回：
 * - API34+：OnBackAnimationCallback（onBackProgressed → 跟手，cancel → 回弹，invoked → hide + dismiss）；
 * - API33：普通 OnBackInvokedCallback（直接 hide + dismiss）；
 * - 低版本 / 拿不到 dispatcher：静默不注册（退回 ModalBottomSheet 默认行为）。
 *
 * Dialog 窗口在首帧可能还没 attach，用 view.postDelayed 重试若干次；onDispose 必须 unregister。
 */
@Composable
internal fun DialogBackDispatchEffect(
    sheetState: SheetState,
    onBackProgress: (Float) -> Unit,
    onBackCancelled: () -> Unit,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val latestOnDismiss = rememberUpdatedState(onDismiss)
    DisposableEffect(view, sheetState) {
        var disposed = false
        var unregister: (() -> Unit)? = null
        fun dismissByBack() {
            scope.launch {
                runCatching { sheetState.hide() }
                if (!sheetState.isVisible) latestOnDismiss.value()
            }
        }
        fun attempt(remaining: Int) {
            if (disposed || unregister != null) return
            val cleanup = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerBackDispatch(
                    view = view,
                    onProgress = onBackProgress,
                    onCancelled = onBackCancelled,
                    onInvoked = { dismissByBack() },
                )
            } else {
                null
            }
            if (cleanup != null) {
                unregister = cleanup
            } else if (remaining > 0) {
                // Dialog 视图可能尚未 attach：延迟重试拿 dispatcher。
                view.postDelayed({ attempt(remaining - 1) }, 50L)
            }
        }
        attempt(5)
        onDispose {
            disposed = true
            unregister?.invoke()
            unregister = null
        }
    }
}

/** API33+：拿到 View 所在窗口的 OnBackInvokedDispatcher 并注册返回回调，返回 unregister 闭包。 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun registerBackDispatch(
    view: View,
    onProgress: (Float) -> Unit,
    onCancelled: () -> Unit,
    onInvoked: () -> Unit,
): (() -> Unit)? {
    val dispatcher = runCatching { view.findOnBackInvokedDispatcher() }.getOrNull() ?: return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        registerPredictiveBackDispatch(dispatcher, onProgress, onCancelled, onInvoked)
    } else {
        val callback = OnBackInvokedCallback { onInvoked() }
        val registered = runCatching {
            dispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                callback,
            )
        }.isSuccess
        if (registered) {
            { runCatching { dispatcher.unregisterOnBackInvokedCallback(callback) } }
        } else {
            null
        }
    }
}

/** API34+：预测性返回动画回调（跟手进度 / 取消回弹 / 完成后关闭）。 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun registerPredictiveBackDispatch(
    dispatcher: OnBackInvokedDispatcher,
    onProgress: (Float) -> Unit,
    onCancelled: () -> Unit,
    onInvoked: () -> Unit,
): (() -> Unit)? {
    val callback = object : OnBackAnimationCallback {
        override fun onBackStarted(backEvent: BackEvent) = Unit

        override fun onBackProgressed(backEvent: BackEvent) {
            onProgress(backEvent.progress)
        }

        override fun onBackCancelled() {
            onCancelled()
        }

        override fun onBackInvoked() {
            onInvoked()
        }
    }
    runCatching {
        dispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            callback,
        )
    }.getOrElse { return null }
    return { runCatching { dispatcher.unregisterOnBackInvokedCallback(callback) }.getOrNull() }
}
