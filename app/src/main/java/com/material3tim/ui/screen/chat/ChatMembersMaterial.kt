package com.material3tim.ui.screen.chat

import com.material3tim.ui.component.material.ExpressiveDurationDialog
import androidx.compose.material.icons.filled.Timer
import kotlin.math.roundToInt
import com.material3tim.ui.component.material.OffsetAnchoredExpressiveMenu
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.component.material.SegmentedColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.ChatMember
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SearchAppBar
import com.material3tim.ui.component.material.ExpressiveConfirmDialog
import com.material3tim.ui.component.material.ExpressiveInputDialog
import com.material3tim.ui.component.material.SegmentedItem
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/**
 * 群成员列表页（Material 3 Expressive）：首字母分组 + SegmentedListItem，
 * 顶部使用与消息页一致的 [SearchAppBar]（点击展开搜索、实时过滤成员）。
 */
@Composable
internal fun ChatMembersMaterial(
    groupName: String,
    members: List<ChatMember>,
    loading: Boolean,
    chatId: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenMemberProfile: (ChatMember) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val role = remember(members) { currentMemberRole(members) }
    var menuMember by remember { mutableStateOf<ChatMember?>(null) }
    var kickMember by remember { mutableStateOf<ChatMember?>(null) }
    var cardMember by remember { mutableStateOf<ChatMember?>(null) }
    // 自定义禁言时长弹窗的目标成员（点击菜单项后设置）。
    var durationMember by remember { mutableStateOf<ChatMember?>(null) }
    val noPermission = stringResource(R.string.group_manage_no_permission)
    val successMessage = stringResource(R.string.group_manage_success)
    val failedMessage = stringResource(R.string.group_manage_failed)
    // 点击任何成员卡片（普通成员/管理员/群主）→ 用户资料页。
    val onMemberClick: (ChatMember) -> Unit = { member -> onOpenMemberProfile(member) }
    // 长按 → 气泡同款 popup 管理菜单（仅对可操作成员）。
    val onMemberLongPress: (ChatMember) -> Unit = { member ->
        if (canOperateMember(role, member)) menuMember = member
    }
    // 菜单动作：与原底部 sheet 完全一致，执行后关闭 popup；踢人/名片仍走原弹窗。
    val onMemberMenuAction: (ChatMember, MemberMenuAction) -> Unit = { member, action ->
        menuMember = null
        when (action) {
            MemberMenuAction.MUTE_10M -> scope.performGroupManageAction(
                context = context,
                noPermissionMessage = noPermission,
                successMessage = successMessage,
                failedMessage = failedMessage,
                onSuccess = onRefresh,
                call = {
                    AppDataSources.groupManageSource
                        ?.setGroupMemberMuted(chatId, member.uid, true, 10 * 60) ?: false
                },
            )
            MemberMenuAction.MUTE_1H -> scope.performGroupManageAction(
                context = context,
                noPermissionMessage = noPermission,
                successMessage = successMessage,
                failedMessage = failedMessage,
                onSuccess = onRefresh,
                call = {
                    AppDataSources.groupManageSource
                        ?.setGroupMemberMuted(chatId, member.uid, true, 60 * 60) ?: false
                },
            )
            MemberMenuAction.MUTE_1D -> scope.performGroupManageAction(
                context = context,
                noPermissionMessage = noPermission,
                successMessage = successMessage,
                failedMessage = failedMessage,
                onSuccess = onRefresh,
                call = {
                    AppDataSources.groupManageSource
                        ?.setGroupMemberMuted(chatId, member.uid, true, 24 * 60 * 60) ?: false
                },
            )
            MemberMenuAction.UNMUTE -> scope.performGroupManageAction(
                context = context,
                noPermissionMessage = noPermission,
                successMessage = successMessage,
                failedMessage = failedMessage,
                onSuccess = onRefresh,
                call = {
                    AppDataSources.groupManageSource
                        ?.setGroupMemberMuted(chatId, member.uid, false) ?: false
                },
            )
            MemberMenuAction.TOGGLE_ADMIN -> scope.performGroupManageAction(
                context = context,
                noPermissionMessage = noPermission,
                successMessage = successMessage,
                failedMessage = failedMessage,
                onSuccess = onRefresh,
                call = {
                    AppDataSources.groupManageSource
                        ?.setGroupMemberAdmin(chatId, member.uid, !member.isAdmin) ?: false
                },
            )
            MemberMenuAction.CUSTOM_MUTE -> durationMember = member
            MemberMenuAction.EDIT_CARD -> cardMember = member
            MemberMenuAction.KICK -> kickMember = member
        }
    }
    var searchText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val filtered = remember(members, searchText) { filterMembers(members, searchText) }
    val groups = groupMembersByLetter(filtered)
    // 群主 & 管理（群主最顶、管理员其后）；下方字母分组仍会包含这些人（重复出现）。
    val privilegedMembers = remember(members) {
        members.filter { it.isOwner } + members.filter { it.isAdmin && !it.isOwner }
    }

    ExpressiveScaffold(
        topBar = {
            // 与联系人页一致的搜索栏顶栏：胶囊搜索栏 + 展开层（搜索时展示过滤后的成员列表）。
            SearchAppBar(
                snackbarHostState = snackbarHostState,
                title = { Text(stringResource(R.string.chat_info_members)) },
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onClearClick = { searchText = "" },
                navigationIcon = {
                    TopBarBackButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.chat_detail_back),
                    )
                },
                scrollBehavior = scrollBehavior,
                defaultContent = { _, _ -> },
                searchContent = { bottomPadding, closeSearch ->
                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp + bottomPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.chat_members_search_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        MemberListMaterial(
                            groups = groups,
                            bottomPadding = bottomPadding,
                            loading = loading,
                            memberCount = 0,
                            privilegedMembers = emptyList(),
                            role = role,
                            menuMemberKey = menuMember?.let { memberKey(it) },
                            onMemberLongPress = onMemberLongPress,
                            onMemberMenuAction = onMemberMenuAction,
                            onDismissMenu = { menuMember = null },
                            onMemberClick = { member ->
                                closeSearch()
                                onMemberClick(member)
                            },
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { innerPadding ->
        MemberListMaterial(
            groups = groups,
            bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            loading = loading,
            memberCount = members.size,
            privilegedMembers = privilegedMembers,
            role = role,
            menuMemberKey = menuMember?.let { memberKey(it) },
            onMemberLongPress = onMemberLongPress,
            onMemberMenuAction = onMemberMenuAction,
            onDismissMenu = { menuMember = null },
            onMemberClick = onMemberClick,
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        )
    }
    kickMember?.let { member ->
        // 踢人确认：Expressive 危险确认弹窗。
        ExpressiveConfirmDialog(
            show = true,
            title = stringResource(R.string.group_manage_kick),
            message = stringResource(R.string.group_manage_kick_confirm),
            confirmText = stringResource(R.string.group_manage_kick),
            danger = true,
            onConfirm = {
                kickMember = null
                scope.performGroupManageAction(
                    context = context,
                    noPermissionMessage = noPermission,
                    successMessage = successMessage,
                    failedMessage = failedMessage,
                    onSuccess = onRefresh,
                    call = {
                        AppDataSources.groupManageSource
                            ?.kickGroupMembers(chatId, listOf(member.uid)) ?: false
                    },
                )
            },
            onDismissRequest = { kickMember = null },
        )
    }
    cardMember?.let { member ->
        MemberInputDialogMaterial(
            title = stringResource(R.string.group_manage_edit_card),
            placeholder = stringResource(R.string.group_manage_card_hint),
            initial = member.name,
            confirmText = stringResource(R.string.confirm),
            onConfirm = { newCard ->
                cardMember = null
                scope.performGroupManageAction(
                    context = context,
                    noPermissionMessage = noPermission,
                    successMessage = successMessage,
                    failedMessage = failedMessage,
                    onSuccess = onRefresh,
                    call = {
                        AppDataSources.groupManageSource
                            ?.setGroupMemberCard(chatId, member.uid, newCard.trim()) ?: false
                    },
                )
            },
            onDismiss = { cardMember = null },
        )
    }
    durationMember?.let { member ->
        ExpressiveDurationDialog(
            show = true,
            title = stringResource(R.string.group_manage_custom_mute),
            onConfirm = { days, hours, minutes ->
                durationMember = null
                val seconds = customMuteSeconds(days, hours, minutes)
                if (seconds > 0) {
                    scope.performGroupManageAction(
                        context = context,
                        noPermissionMessage = noPermission,
                        successMessage = successMessage,
                        failedMessage = failedMessage,
                        onSuccess = onRefresh,
                        call = {
                            AppDataSources.groupManageSource
                                ?.setGroupMemberMuted(chatId, member.uid, true, seconds) ?: false
                        },
                    )
                }
            },
            onDismissRequest = { durationMember = null },
        )
    }
}

@Composable
private fun MemberListMaterial(
    groups: List<Pair<Char, List<ChatMember>>>,
    bottomPadding: Dp,
    loading: Boolean,
    onMemberClick: (ChatMember) -> Unit,
    role: GroupMemberRole,
    menuMemberKey: String?,
    onMemberLongPress: (ChatMember) -> Unit,
    onMemberMenuAction: (ChatMember, MemberMenuAction) -> Unit,
    onDismissMenu: () -> Unit,
    modifier: Modifier = Modifier,
    memberCount: Int = 0,
    privilegedMembers: List<ChatMember> = emptyList(),
) {
    if (loading && groups.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 0.dp,
            bottom = 16.dp + bottomPadding,
        ),
    ) {
        if (privilegedMembers.isNotEmpty()) {
            item(key = "privileged_group") {
                SegmentedColumn(title = stringResource(R.string.chat_members_owner_admin)) {
                    privilegedMembers.forEach { member ->
                        item(key = "privileged_${memberKey(member)}") {
                            ChatMemberRowMaterial(
                                member = member,
                                onClick = { onMemberClick(member) },
                                showRoleBadge = true,
                                canManage = canOperateMember(role, member),
                                canSetAdmin = canSetMemberAdmin(role, member),
                                menuVisible = menuMemberKey == memberKey(member),
                                onLongPress = { onMemberLongPress(member) },
                                onDismissMenu = onDismissMenu,
                                onMenuAction = { action -> onMemberMenuAction(member, action) },
                            )
                        }
                    }
                }
            }
        }
        if (memberCount > 0) {
            item(key = "member_count") {
                Text(
                    text = stringResource(R.string.chat_members_total, memberCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 2.dp),
                )
            }
        }
        groups.forEach { (letter, groupMembers) ->
            item(key = "letter_$letter") {
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            itemsIndexed(groupMembers, key = { _, member -> memberKey(member) }) { index, member ->
                SegmentedItem(index = index, count = groupMembers.size) {
                    ChatMemberRowMaterial(
                        member = member,
                        onClick = { onMemberClick(member) },
                        canManage = canOperateMember(role, member),
                        canSetAdmin = canSetMemberAdmin(role, member),
                        menuVisible = menuMemberKey == memberKey(member),
                        onLongPress = { onMemberLongPress(member) },
                        onDismissMenu = onDismissMenu,
                        onMenuAction = { action -> onMemberMenuAction(member, action) },
                    )
                }
            }
        }
    }
}

internal fun memberKey(member: ChatMember): String =
    "${member.uin.ifBlank { member.name }}_${member.avatarSeed}"

internal fun memberSubtitle(member: ChatMember): String {
    val parts = mutableListOf<String>()
    if (member.uin.isNotBlank()) parts += "QQ：${member.uin}"
    when {
        member.isOwner -> parts += "群主"
        member.isAdmin -> parts += "管理员"
        member.isMe -> parts += "我"
    }
    return parts.joinToString(" · ")
}

@Composable
private fun ChatMemberRowMaterial(
    member: ChatMember,
    onClick: () -> Unit,
    showRoleBadge: Boolean = false,
    canManage: Boolean = false,
    canSetAdmin: Boolean = false,
    menuVisible: Boolean = false,
    onLongPress: () -> Unit = {},
    onDismissMenu: () -> Unit = {},
    onMenuAction: (MemberMenuAction) -> Unit = {},
) {
    val subtitle = memberSubtitle(member)
    // 长按点相对行内的坐标：作为 M3E popup 的锚点（与长按消息气泡的 pressOffset 一致）。
    var pressOffset by remember { mutableStateOf(IntOffset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .memberLongPress { offset ->
                if (canManage) {
                    pressOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                    onLongPress()
                }
            },
    ) {
        SegmentedListItem(
            onClick = onClick,
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = if (subtitle.isNotBlank()) {
                {
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                null
            },
            trailingContent = if (showRoleBadge && (member.isOwner || member.isAdmin)) {
                {
                    Text(
                        text = stringResource(
                            if (member.isOwner) R.string.chat_info_owner else R.string.chat_info_admin,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                null
            },
        )
        if (menuVisible) {
            MemberActionsMenuMaterial(
                canSetAdmin = canSetAdmin,
                isAdmin = member.isAdmin,
                anchorOffset = pressOffset,
                onDismiss = onDismissMenu,
                onAction = onMenuAction,
            )
        }
    }
}

/** 成员管理 popup 菜单（M3E）：复用长按消息气泡同款 Expressive 下拉，锚点=成员卡片。 */
@Composable
private fun MemberActionsMenuMaterial(
    canSetAdmin: Boolean,
    isAdmin: Boolean,
    anchorOffset: IntOffset,
    onDismiss: () -> Unit,
    onAction: (MemberMenuAction) -> Unit,
) {
    data class Entry(
        val label: String,
        val icon: ImageVector,
        val action: MemberMenuAction,
        val danger: Boolean = false,
    )

    val entries = buildList {
        add(Entry(stringResource(R.string.group_manage_mute_10m), Icons.Filled.NotificationsOff, MemberMenuAction.MUTE_10M))
        add(Entry(stringResource(R.string.group_manage_mute_1h), Icons.Filled.NotificationsOff, MemberMenuAction.MUTE_1H))
        add(Entry(stringResource(R.string.group_manage_mute_1d), Icons.Filled.NotificationsOff, MemberMenuAction.MUTE_1D))
        add(Entry(stringResource(R.string.group_manage_custom_mute), Icons.Filled.Timer, MemberMenuAction.CUSTOM_MUTE))
        add(Entry(stringResource(R.string.group_manage_unmute), Icons.Filled.NotificationsActive, MemberMenuAction.UNMUTE))
        if (canSetAdmin) {
            add(
                Entry(
                    stringResource(
                        if (isAdmin) R.string.group_manage_unset_admin else R.string.group_manage_set_admin,
                    ),
                    Icons.Filled.ManageAccounts,
                    MemberMenuAction.TOGGLE_ADMIN,
                ),
            )
        }
        add(Entry(stringResource(R.string.group_manage_edit_card), Icons.Filled.Edit, MemberMenuAction.EDIT_CARD))
        add(Entry(stringResource(R.string.group_manage_kick), Icons.Filled.Delete, MemberMenuAction.KICK, danger = true))
    }
    OffsetAnchoredExpressiveMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        anchorOffset = anchorOffset,
    ) {
        entries.forEachIndexed { index, entry ->
            SelectableDropdownMenuItem(
                text = { Text(entry.label) },
                selected = false,
                leadingIcon = {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        tint = if (entry.danger) {
                            MaterialTheme.colorScheme.error
                        } else {
                            LocalContentColor.current
                        },
                        modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                    )
                },
                shapes = MenuDefaults.itemShape(index = index, count = entries.size),
                onClick = { onAction(entry.action) },
            )
        }
    }
}

/** 成员操作菜单（M3E）：ModalBottomSheet + SegmentedList 动作行（含预测性返回跟手动画）。 */
@Suppress("DEPRECATION") // rememberModalBottomSheetState 在 material3 alpha28 仍是稳定用法
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemberActionsSheetMaterial(
    member: ChatMember,
    role: GroupMemberRole,
    chatId: String,
    noPermission: String,
    successMessage: String,
    failedMessage: String,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    onKick: (ChatMember) -> Unit,
    onEditCard: (ChatMember) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val source = AppDataSources.groupManageSource
    val perform: (() -> Boolean) -> Unit = { call ->
        scope.performGroupManageAction(
            context = context,
            noPermissionMessage = noPermission,
            successMessage = successMessage,
            failedMessage = failedMessage,
            onSuccess = onRefresh,
            call = call,
        )
    }
    fun mute(seconds: Int) {
        onDismiss()
        perform { source?.setGroupMemberMuted(chatId, member.uid, true, seconds) ?: false }
    }
    // 预测性返回：material3 ModalBottomSheet 的 Dialog 窗口默认拿不到跟手返回动画。
    // 与 @ 面板完全同款：背景透明 + 自绘 Surface，跟手进度驱动「整个 Surface」translationY
    // （不能只平移内容，否则看起来像列表在收缩）。MiuiX 侧 OverlayBottomSheet 自带该手势。
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val backProgress = remember { Animatable(0f) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true,
            shouldDismissOnClickOutside = true,
        ),
        // 让 sheet 背景一直画到屏幕底部（含导航栏区域），内容再自行避让 nav inset。
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
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
                .graphicsLayer { translationY = backProgress.value * size.height }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            // 顶部拖拽条：material3 官方 DragHandle（与 @ 面板一致）
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BottomSheetDefaults.DragHandle()
            }
            // 内容左右 16dp（与普通列表一致）；导航栏 inset 只作用于内容，背景仍铺到屏幕底部。
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            ) {
            SegmentedColumn {
                item {
                    SegmentedListItem(
                        onClick = { mute(10 * 60) },
                        headlineContent = { Text(stringResource(R.string.group_manage_mute_10m)) },
                    )
                }
                item {
                    SegmentedListItem(
                        onClick = { mute(60 * 60) },
                        headlineContent = { Text(stringResource(R.string.group_manage_mute_1h)) },
                    )
                }
                item {
                    SegmentedListItem(
                        onClick = { mute(24 * 60 * 60) },
                        headlineContent = { Text(stringResource(R.string.group_manage_mute_1d)) },
                    )
                }
                item {
                    SegmentedListItem(
                        onClick = {
                            onDismiss()
                            perform { source?.setGroupMemberMuted(chatId, member.uid, false) ?: false }
                        },
                        headlineContent = { Text(stringResource(R.string.group_manage_unmute)) },
                    )
                }
                if (canSetMemberAdmin(role, member)) {
                    item {
                        SegmentedListItem(
                            onClick = {
                                onDismiss()
                                perform {
                                    source?.setGroupMemberAdmin(chatId, member.uid, !member.isAdmin)
                                        ?: false
                                }
                            },
                            headlineContent = {
                                Text(
                                    stringResource(
                                        if (member.isAdmin) {
                                            R.string.group_manage_unset_admin
                                        } else {
                                            R.string.group_manage_set_admin
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
                item {
                    SegmentedListItem(
                        onClick = {
                            onDismiss()
                            onEditCard(member)
                        },
                        headlineContent = { Text(stringResource(R.string.group_manage_edit_card)) },
                    )
                }
                item {
                    SegmentedListItem(
                        onClick = {
                            onDismiss()
                            onKick(member)
                        },
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.group_manage_kick),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                }
            }
            }
        }
    }
}

@Composable
internal fun MemberInputDialogMaterial(
    title: String,
    placeholder: String,
    initial: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // 群昵称输入：Expressive 输入型弹窗（复用共享组件）。
    ExpressiveInputDialog(
        show = true,
        title = title,
        initialText = initial,
        placeholder = placeholder,
        confirmText = confirmText,
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
    )
}
