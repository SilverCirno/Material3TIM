package com.material3tim.ui.screen.chat

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.material3tim.ui.component.miuix.glass.GlassMenuEntry
import com.material3tim.ui.component.miuix.glass.GlassConfirmDialog
import com.material3tim.ui.component.miuix.glass.GlassInputDialog
import com.material3tim.ui.component.miuix.glass.GlassSheet
import com.material3tim.ui.component.miuix.glass.GlassMenuHost
import com.material3tim.ui.component.miuix.glass.GlassMenuState
import com.material3tim.ui.component.miuix.glass.LocalGlassMenu
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.glass.GlassTextFieldDefaults
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarIconButton
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.material3tim.data.source.AppDataSources
import top.yukonga.miuix.kmp.basic.TextField
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.ChatMember
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.SearchStatus
import com.material3tim.ui.component.miuix.SearchBarFake
import com.material3tim.ui.component.miuix.SearchPager
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * 群成员列表页（Miuix）：顶部搜索栏（与消息页展开态一致）+ 首字母分组 Card 列表。
 */
@Composable
internal fun ChatMembersMiuix(
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
    // 群名片输入内容：用于「非空才可确认」（由 GlassInputDialog.onTextChange 同步）。
    var cardEditText by remember { mutableStateOf("") }
    // 自定义禁言时长输入（常驻对话框，打开时重置）。
    var durationDays by remember { mutableStateOf("") }
    var durationHours by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("") }
    val durationTotalSeconds = customMuteSeconds(durationDays, durationHours, durationMinutes)
    // 每次换目标成员/重新打开时清空时长输入。
    LaunchedEffect(durationMember) {
        if (durationMember != null) {
            durationDays = ""
            durationHours = ""
            durationMinutes = ""
        }
    }
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
    var searchStatus by remember {
        mutableStateOf(
            SearchStatus(
                label = "",
                current = SearchStatus.Status.COLLAPSED,
            ),
        )
    }
    val query = searchStatus.searchText
    val filtered = remember(members, query) { filterMembers(members, query) }
    val groups = groupMembersByLetter(filtered)
    // 群主 & 管理（群主最顶、管理员其后）；下方字母分组仍会包含这些人（重复出现）。
    val privilegedMembers = remember(members) {
        members.filter { it.isOwner } + members.filter { it.isAdmin && !it.isOwner }
    }
    val enableBlur = LocalEnableBlur.current
    val density = LocalDensity.current
    val scrollBehavior = MiuixScrollBehavior()
    val dynamicTopPadding by remember {
        derivedStateOf { 12.dp * (1f - scrollBehavior.state.collapsedFraction) }
    }
    // Phase 2b：页面级 Glass 采样 backdrop（关闭模糊/无 runtime shader 时为 null → 纯色回退）。
    val backdrop = rememberPageBackdrop(enableBlur)
    // 页面内容层长按菜单宿主状态：host 在 NavDisplay 子树内，返回手势优先关菜单。
    val glassMenu = remember { GlassMenuState() }
    val layoutDirection = LocalLayoutDirection.current
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val onSearchStatusChange: (SearchStatus) -> Unit = { updated -> searchStatus = updated }

    CompositionLocalProvider(
        LocalPageBackdrop provides backdrop,
        LocalGlassMenu provides glassMenu,
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            // 与联系人页 Miuix 同款：玻璃顶栏 + 底部胶囊搜索栏（点展开全屏搜索层）。
            HyperTopAppBar(
                title = stringResource(R.string.chat_info_members),
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                navigationIcon = {
                    // 圆形 Glass 按钮（玻璃模式）/ 普通 IconButton（纯色回退）。
                    HyperTopBarIconButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.chat_detail_back),
                        icon = MiuixIcons.Back,
                    )
                },
                bottomContent = {
                            Box(
                                modifier = Modifier
                                    .alpha(if (searchStatus.isCollapsed()) 1f else 0f)
                                    .onGloballyPositioned { coordinates ->
                                        with(density) {
                                            val newOffsetY = coordinates.positionInWindow().y.toDp()
                                            if (searchStatus.offsetY != newOffsetY) {
                                                onSearchStatusChange(
                                                    searchStatus.copy(offsetY = newOffsetY),
                                                )
                                            }
                                        }
                                    }
                                    .then(
                                        if (searchStatus.isCollapsed()) {
                                            Modifier.pointerInput(Unit) {
                                                detectTapGestures {
                                                    onSearchStatusChange(
                                                        searchStatus.copy(
                                                            current = SearchStatus.Status.EXPANDING,
                                                        ),
                                                    )
                                                }
                                            }
                                        } else {
                                            Modifier
                                        },
                                    ),
                            ) {
                                SearchBarFake(
                                    label = stringResource(R.string.chat_members_search_hint),
                                    searchBarTopPadding = dynamicTopPadding,
                                )
                            }
                },
            )
        },
        popupHost = {
            searchStatus.SearchPager(
                onSearchStatusChange = onSearchStatusChange,
                defaultResult = {},
                searchBarTopPadding = dynamicTopPadding,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .overScrollVertical(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = navigationBarPadding + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    overscrollEffect = null,
                ) {
                    item {
                        Spacer(Modifier.height(6.dp))
                    }
                    if (filtered.isEmpty()) {
                        item(key = "search_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.chat_members_search_empty),
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    } else {
                        items(filtered, key = { memberKey(it) }) { member ->
                            ChatMemberCardMiuix(
                                member = member,
                                onClick = {
                                    onSearchStatusChange(
                                        searchStatus.copy(
                                            searchText = "",
                                            current = SearchStatus.Status.COLLAPSING,
                                        ),
                                    )
                                    onMemberClick(member)
                                },
                                canManage = canOperateMember(role, member),
                                canSetAdmin = canSetMemberAdmin(role, member),
                                menuVisible = menuMember?.let { memberKey(it) } == memberKey(member),
                                onLongPress = { onMemberLongPress(member) },
                                onDismissMenu = { menuMember = null },
                                onMenuAction = { action -> onMemberMenuAction(member, action) },
                            )
                        }
                    }
                }
            }
        },
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
            if (loading && groups.isEmpty() && privilegedMembers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.loading),
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
            } else if (groups.isEmpty() && privilegedMembers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.chat_members_search_empty),
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
                        end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp,
                        top = innerPadding.calculateTopPadding() + 6.dp,
                        bottom = navigationBarPadding + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    overscrollEffect = null,
                ) {
                    // 群主 & 管理分组（置顶，群主在前、管理员其后）。
                    if (privilegedMembers.isNotEmpty()) {
                        item(key = "privileged_title") {
                            Text(
                                text = stringResource(R.string.chat_members_owner_admin),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 4.dp),
                            )
                        }
                        items(privilegedMembers, key = { "privileged_${memberKey(it)}" }) { member ->
                            ChatMemberCardMiuix(
                                member = member,
                                onClick = { onMemberClick(member) },
                                showRoleBadge = true,
                                canManage = canOperateMember(role, member),
                                canSetAdmin = canSetMemberAdmin(role, member),
                                menuVisible = menuMember?.let { memberKey(it) } == memberKey(member),
                                onLongPress = { onMemberLongPress(member) },
                                onDismissMenu = { menuMember = null },
                                onMenuAction = { action -> onMemberMenuAction(member, action) },
                            )
                        }
                    }
                    // 人数小字（不放进标题）。
                    item(key = "member_count") {
                        Text(
                            text = stringResource(R.string.chat_members_total, members.size),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 2.dp),
                        )
                    }
                    // 字母分组仍包含群主/管理员（重复出现）。
                    groups.forEach { (letter, groupMembers) ->
                        item(key = "letter_$letter") {
                            Text(
                                text = letter.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 4.dp),
                            )
                        }
                        items(groupMembers, key = { memberKey(it) }) { member ->
                            ChatMemberCardMiuix(
                                member = member,
                                onClick = { onMemberClick(member) },
                                canManage = canOperateMember(role, member),
                                canSetAdmin = canSetMemberAdmin(role, member),
                                menuVisible = menuMember?.let { memberKey(it) } == memberKey(member),
                                onLongPress = { onMemberLongPress(member) },
                                onDismissMenu = { menuMember = null },
                                onMenuAction = { action -> onMemberMenuAction(member, action) },
                            )
                        }
                    }
                }
            }
        }
    }
    // ===== 玻璃对话框（常驻组合，用 show 参数控制显隐）=====
    // 踢出群成员确认
    GlassConfirmDialog(
        show = kickMember != null,
        onDismissRequest = { kickMember = null },
        title = stringResource(R.string.group_manage_kick),
        summary = stringResource(R.string.group_manage_kick_confirm),
        confirmText = stringResource(R.string.group_manage_kick),
        cancelText = stringResource(R.string.cancel),
        destructive = true,
        onConfirm = {
            val member = kickMember
            kickMember = null
            if (member != null) {
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
            }
        },
    )
    // 修改群名片（打开时重置为成员昵称；空白不可确认）
    GlassInputDialog(
        show = cardMember != null,
        onDismissRequest = { cardMember = null },
        title = stringResource(R.string.group_manage_edit_card),
        summary = stringResource(R.string.group_manage_card_hint),
        initialText = cardMember?.name.orEmpty(),
        singleLine = true,
        confirmText = stringResource(R.string.confirm),
        cancelText = stringResource(R.string.cancel),
        confirmEnabled = cardEditText.isNotBlank(),
        onTextChange = { cardEditText = it },
        onConfirm = { text ->
            val member = cardMember
            cardMember = null
            if (member != null) {
                scope.performGroupManageAction(
                    context = context,
                    noPermissionMessage = noPermission,
                    successMessage = successMessage,
                    failedMessage = failedMessage,
                    onSuccess = onRefresh,
                    call = {
                        AppDataSources.groupManageSource
                            ?.setGroupMemberCard(chatId, member.uid, text.trim()) ?: false
                    },
                )
            }
        },
    )
    // 自定义禁言时长（天/时/分，自定义内容 → 直接用 GlassDialog 承载）
    GlassDialog(
        show = durationMember != null,
        onDismissRequest = { durationMember = null },
        backdrop = LocalPageBackdrop.current,
        title = stringResource(R.string.group_manage_custom_mute),
        summary = stringResource(R.string.group_manage_custom_mute_hint),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MiuixDurationField(
                label = stringResource(R.string.group_manage_mute_days),
                value = durationDays,
                onValueChange = { durationDays = sanitizeDurationInput(it, 3) },
                modifier = Modifier.weight(1f),
            )
            MiuixDurationField(
                label = stringResource(R.string.group_manage_mute_hours),
                value = durationHours,
                onValueChange = { durationHours = sanitizeDurationInput(it, 2) },
                modifier = Modifier.weight(1f),
            )
            MiuixDurationField(
                label = stringResource(R.string.group_manage_mute_minutes),
                value = durationMinutes,
                onValueChange = { durationMinutes = sanitizeDurationInput(it, 2) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassDialogButton(
                onClick = { durationMember = null },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MiuixTheme.textStyles.button,
                )
            }
            GlassDialogButton(
                onClick = {
                    val member = durationMember
                    val total = customMuteSeconds(durationDays, durationHours, durationMinutes)
                    durationMember = null
                    if (member != null && total > 0) {
                        scope.performGroupManageAction(
                            context = context,
                            noPermissionMessage = noPermission,
                            successMessage = successMessage,
                            failedMessage = failedMessage,
                            onSuccess = onRefresh,
                            call = {
                                AppDataSources.groupManageSource
                                    ?.setGroupMemberMuted(chatId, member.uid, true, total) ?: false
                            },
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = durationTotalSeconds > 0,
                role = GlassDialogButtonRole.Primary,
            ) {
                Text(
                    text = stringResource(R.string.confirm),
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
    }
    // 长按菜单宿主：页面内容层最后渲染（不被列表/顶栏裁剪；返回优先级在页面处理器之后）。
    GlassMenuHost(glassMenu)
    }
    }
}

/** 禁言时长输入框（Miuix）：数字键盘 + label 文案。 */
@Composable
private fun MiuixDurationField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            // 玻璃对话框表面使用高对比度输入框配色。
            colors = GlassTextFieldDefaults.textFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
    }
}


/**
 * 成员操作底部 sheet（Miuix）：与 @ 面板 [ChatAtSheetMiuix] 同款 OverlayBottomSheet
 * （同组件/同交互/自带预测性返回），动作用 Miuix Card 呈现，卡片之间 12dp 间距、左右 12dp 内边距，
 * 底部额外留系统导航栏安全区。群成员页与用户资料页三点共用。
 */
@Composable
internal fun MemberActionsDialogMiuix(
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
    GlassSheet(
        show = true,
        onDismissRequest = onDismiss,
        // 保持迁移前 OverlayBottomSheet 的内容内边距（横向 24dp），动作卡片位置不变。
        insideMargin = DpSize(24.dp, 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 标题行：群成员管理 / 成员名 + 群名片
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.group_manage_menu_title, member.name),
                    fontSize = 18.sp,
                    fontWeight = FontWeight(600),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = memberSubtitle(member)
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            MemberActionCardMiuix(
                text = stringResource(R.string.group_manage_mute_10m),
                onClick = { mute(10 * 60) },
            )
            MemberActionCardMiuix(
                text = stringResource(R.string.group_manage_mute_1h),
                onClick = { mute(60 * 60) },
            )
            MemberActionCardMiuix(
                text = stringResource(R.string.group_manage_mute_1d),
                onClick = { mute(24 * 60 * 60) },
            )
            MemberActionCardMiuix(
                text = stringResource(R.string.group_manage_unmute),
                onClick = {
                    onDismiss()
                    perform {
                        source?.setGroupMemberMuted(chatId, member.uid, false) ?: false
                    }
                },
            )
            if (canSetMemberAdmin(role, member)) {
                MemberActionCardMiuix(
                    text = stringResource(
                        if (member.isAdmin) {
                            R.string.group_manage_unset_admin
                        } else {
                            R.string.group_manage_set_admin
                        },
                    ),
                    onClick = {
                        onDismiss()
                        perform {
                            source?.setGroupMemberAdmin(chatId, member.uid, !member.isAdmin)
                                ?: false
                        }
                    },
                )
            }
            MemberActionCardMiuix(
                text = stringResource(R.string.group_manage_edit_card),
                onClick = {
                    onDismiss()
                    onEditCard(member)
                },
            )
            MemberActionCardMiuix(
                text = stringResource(R.string.group_manage_kick),
                danger = true,
                onClick = {
                    onDismiss()
                    onKick(member)
                },
            )
            // 底部导航栏安全区（键盘弹出时由 imePadding 负责，避免重复留白）。
            Spacer(Modifier.height(memberSheetBottomInset()))
        }
    }
}

/** 动作卡片（Miuix Card 行）：常规动作 vs 红色危险动作。 */
@Composable
private fun MemberActionCardMiuix(
    text: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        showIndication = true,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                color = if (danger) colorScheme.error else colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 底部动作区安全区（与 @ 面板 atSheetBottomInset 一致）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun memberSheetBottomInset(): Dp =
    if (WindowInsets.isImeVisible) {
        0.dp
    } else {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    }

@Composable
private fun ChatMemberCardMiuix(
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
    // 1x1 morph 锚点：GlassTransformPopup 从手指长按点展开、预测性返回跟手收缩回该点。
    val rowAnchor = rememberGlassPopupAnchor()
    // 长按点（相对成员行左上角）：1dp 透明锚点 Box 放在这里，作为 morph 展开/收缩的中心。
    var rowPressPosition by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .memberLongPress { offset ->
                if (canManage) {
                    rowPressPosition = offset
                    onLongPress()
                }
            },
    ) {
        // 1dp 透明锚点：放在手指长按位置（窗口矩形≈1x1），菜单从该点展开/收缩；
        // cornerRadius 与展开面板一致（24dp，start==end）→ 不会出现负圆角。
        Box(
            modifier = Modifier
                .offset { IntOffset(rowPressPosition.x.roundToInt(), rowPressPosition.y.roundToInt()) }
                .size(1.dp)
                .glassPopupAnchor(anchor = rowAnchor, cornerRadius = 24.dp, floating = true),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            showIndication = true,
            insideMargin = PaddingValues(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChatAvatar(
                    name = member.name,
                    seed = member.avatarSeed,
                    size = 44.dp,
                    avatarPath = member.avatarPath,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        fontSize = 15.sp,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (showRoleBadge && (member.isOwner || member.isAdmin)) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (member.isOwner) R.string.chat_info_owner else R.string.chat_info_admin,
                        ),
                        fontSize = 12.sp,
                        color = colorScheme.primary,
                    )
                }
            }
        }
        if (menuVisible) {
            MemberActionsMenuMiuix(
                canSetAdmin = canSetAdmin,
                isAdmin = member.isAdmin,
                anchor = rowAnchor,
                onDismiss = onDismissMenu,
                onAction = onMenuAction,
            )
        }
    }
}

private data class MemberMenuEntry(
    val label: String,
    val icon: ImageVector,
    val action: MemberMenuAction,
    val danger: Boolean = false,
)

/**
 * 成员管理菜单（Miuix）：锚点 = 成员行内 1dp 透明 Box（跟随手指长按点），
 * `GlassTransformPopup` 从手指点展开/预测性返回跟手收缩；危险项（踢出群聊）统一危险红。
 */
@Composable
private fun MemberActionsMenuMiuix(
    canSetAdmin: Boolean,
    isAdmin: Boolean,
    anchor: GlassPopupAnchor,
    onDismiss: () -> Unit,
    onAction: (MemberMenuAction) -> Unit,
) {
    val glassMenu = LocalGlassMenu.current
    // 页面采样只能在页面 CompositionLocal 作用域内读，这里读到后交给根级状态保存。
    val pageBackdrop = LocalPageBackdrop.current
    // 菜单发出方 token：成员行被移出列表时关闭残留菜单。
    val menuOwner = remember { Any() }
    DisposableEffect(menuOwner) {
        onDispose { glassMenu?.hideIfOwner(menuOwner) }
    }
    val entries = buildList {
        add(
            MemberMenuEntry(
                stringResource(R.string.group_manage_mute_10m),
                Icons.Filled.NotificationsOff,
                MemberMenuAction.MUTE_10M,
            ),
        )
        add(
            MemberMenuEntry(
                stringResource(R.string.group_manage_mute_1h),
                Icons.Filled.NotificationsOff,
                MemberMenuAction.MUTE_1H,
            ),
        )
        add(
            MemberMenuEntry(
                stringResource(R.string.group_manage_mute_1d),
                Icons.Filled.NotificationsOff,
                MemberMenuAction.MUTE_1D,
            ),
        )
        add(
            MemberMenuEntry(
                stringResource(R.string.group_manage_custom_mute),
                Icons.Filled.Timer,
                MemberMenuAction.CUSTOM_MUTE,
            ),
        )
        add(
            MemberMenuEntry(
                stringResource(R.string.group_manage_unmute),
                Icons.Filled.NotificationsActive,
                MemberMenuAction.UNMUTE,
            ),
        )
        if (canSetAdmin) {
            add(
                MemberMenuEntry(
                    stringResource(
                        if (isAdmin) R.string.group_manage_unset_admin else R.string.group_manage_set_admin,
                    ),
                    Icons.Filled.ManageAccounts,
                    MemberMenuAction.TOGGLE_ADMIN,
                ),
            )
        }
        add(
            MemberMenuEntry(
                stringResource(R.string.group_manage_edit_card),
                Icons.Filled.Edit,
                MemberMenuAction.EDIT_CARD,
            ),
        )
        add(
            MemberMenuEntry(
                stringResource(R.string.group_manage_kick),
                Icons.Filled.Delete,
                MemberMenuAction.KICK,
                danger = true,
            ),
        )
    }
    // 本 composable 只在 menuVisible 时进入组合：先等一帧让 1dp 锚点 Box 移动到本次长按点，
    // 再交给页面级宿主用 GlassTransformPopup 从手指点展开（预测性返回跟手收缩回手指点）。
    LaunchedEffect(Unit) {
        withFrameNanos { }
        glassMenu?.show(
            owner = menuOwner,
            anchor = anchor,
            entries = entries.map { entry ->
                GlassMenuEntry(
                    label = entry.label,
                    icon = entry.icon,
                    isDanger = entry.danger,
                    onClick = { onAction(entry.action) },
                )
            },
            backdrop = pageBackdrop,
            onDismiss = onDismiss,
        )
    }
}
