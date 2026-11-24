package com.material3tim.ui.screen.chat

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import kotlinx.coroutines.withTimeoutOrNull

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.material3tim.R
import com.material3tim.data.model.ChatMember
import com.material3tim.data.repository.ChatRepository
import com.material3tim.data.repository.MessageRepository
import com.material3tim.data.source.AppDataSources
import com.material3tim.data.source.DataUpdate
import com.material3tim.data.source.LiveEvents
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 群成员列表页（独立页面，样式对齐联系人页：A/B/C… 首字母分组 + 分段列表）。
 *
 * 从聊天信息页「查看全部 N 位成员」进入，数据复用 [com.material3tim.xposed.TimBridge.loadChatInfo]
 * 的成员列表（60 秒内走缓存，打开很快）。
 */
@Composable
fun ChatMembersScreen(chatId: String) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val isHost = AppDataSources.hasLiveDataSource
    var members by remember(chatId) { mutableStateOf<List<ChatMember>>(emptyList()) }
    var groupName by remember(chatId) { mutableStateOf("") }
    var loading by remember(chatId) { mutableStateOf(isHost) }
    var refreshTick by remember(chatId) { mutableStateOf(0) }

    // 首屏快路径：loadChatInfoFast（毫秒级缓存）先渲染成员，随后 loadChatInfo 覆盖；
    // 有 fast 数据时不显示 loading，也不再走长重试。
    LaunchedEffect(chatId, refreshTick) {
        if (isHost) {
            if (refreshTick > 0) {
                // 管理操作（踢人/管理员/群昵称）后绕过缓存，短补偿 150ms 等内核写入后再拉（原 300ms）。
                AppDataSources.chatSource?.invalidateChatInfoCache(chatId)
                kotlinx.coroutines.delay(150L)
            }
            if (members.isEmpty()) loading = true
            val fast = withContext(Dispatchers.IO) {
                runCatching { AppDataSources.chatSource?.loadChatInfoFast(chatId) }.getOrNull()
            }
            if (fast != null) {
                members = fast.members
                groupName = fast.chat.name
                loading = false
            }
            val live = if (fast != null) {
                withContext(Dispatchers.IO) {
                    runCatching { AppDataSources.chatSource?.loadChatInfo(chatId) }.getOrNull()
                }
            } else {
                // 无 fast 缓存（冷启动）：短重试等内核成员列表就绪。
                loadChatInfoWithRetry(chatId, maxAttempts = 3, delayMillis = 600L)
            }
            if (live != null) {
                members = live.members
                groupName = live.chat.name
            } else if (members.isEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.group_files_load_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            }
            loading = false
        } else {
            ChatRepository.chatById(chatId)?.let { chat ->
                val sample = MessageRepository.chatInfoFor(chat)
                members = sample.members
                groupName = chat.name
            }
            loading = false
        }
    }
    // 后台慢查询完成（成员/资料刷新）：直接用已更新的缓存数据局部覆盖成员列表，不闪 loading、不再全量重试。
    LaunchedEffect(chatId) {
        if (!isHost) return@LaunchedEffect
        LiveEvents.dataUpdates.collect { update ->
            if (update.chatId == chatId &&
                (update.kind == DataUpdate.Kind.MEMBERS || update.kind == DataUpdate.Kind.CHAT_INFO)
            ) {
                val fresh = withContext(Dispatchers.IO) {
                    runCatching { AppDataSources.chatSource?.loadChatInfo(chatId) }.getOrNull()
                }
                if (fresh != null && fresh.members.isNotEmpty()) {
                    members = fresh.members
                    groupName = fresh.chat.name
                    loading = false
                }
            }
        }
    }

    val onBack: () -> Unit = { navigator.pop() }
    // 点击任意成员卡片（群主/管理员/普通成员、含搜索结果）→ 用户资料页（与聊天页点头像同款）。
    val onOpenMemberProfile: (ChatMember) -> Unit = { member ->
        if (member.uid.isBlank() && member.uin.isBlank()) {
            Toast.makeText(
                context,
                context.getString(R.string.member_open_profile_failed),
                Toast.LENGTH_SHORT,
            ).show()
        } else {
            navigator.push(
                Route.ContactDetail(
                    contactId = "",
                    chatId = chatId,
                    uid = member.uid,
                    uin = member.uin,
                ),
            )
        }
    }
    when (LocalUiMode.current) {
        UiMode.Miuix -> ChatMembersMiuix(
            groupName = groupName,
            members = members,
            loading = loading,
            chatId = chatId,
            onBack = onBack,
            onRefresh = { refreshTick += 1 },
            onOpenMemberProfile = onOpenMemberProfile,
        )
        UiMode.Material -> ChatMembersMaterial(
            groupName = groupName,
            members = members,
            loading = loading,
            chatId = chatId,
            onBack = onBack,
            onRefresh = { refreshTick += 1 },
            onOpenMemberProfile = onOpenMemberProfile,
        )
    }
}

/** 按名称 / QQ 号过滤成员（忽略大小写）。 */
internal fun filterMembers(members: List<ChatMember>, query: String): List<ChatMember> {
    val q = query.trim()
    if (q.isEmpty()) return members
    return members.filter { member ->
        member.name.contains(q, ignoreCase = true) || member.uin.contains(q)
    }
}

/** 按显示名首字母分组：A-Z，其它归入 #，组内按名称排序。 */
internal fun groupMembersByLetter(members: List<ChatMember>): List<Pair<Char, List<ChatMember>>> {
    if (members.isEmpty()) return emptyList()
    val grouped = members.groupBy { memberLetter(it.name) }
    return grouped.keys
        .sortedWith(compareBy({ it == '#' }, { it }))
        .map { letter -> letter to grouped.getValue(letter).sortedBy { it.name.lowercase() } }
}

internal fun memberLetter(name: String): Char {
    val first = name.trim().firstOrNull() ?: return '#'
    return when (first) {
        in 'A'..'Z' -> first
        in 'a'..'z' -> first.uppercaseChar()
        else -> '#'
    }
}

/** 当前账号在该群的角色（群主/管理员）。 */
internal data class GroupMemberRole(
    val isOwner: Boolean = false,
    val isAdmin: Boolean = false,
) {
    val canManage: Boolean get() = isOwner || isAdmin
}

internal fun currentMemberRole(members: List<ChatMember>): GroupMemberRole {
    val me = members.firstOrNull { it.isMe } ?: return GroupMemberRole()
    return GroupMemberRole(isOwner = me.isOwner, isAdmin = me.isAdmin)
}

/** 权限约束：不能操作自己/群主；管理员不能操作其他管理员，群主可以。 */
internal fun canOperateMember(role: GroupMemberRole, target: ChatMember): Boolean {
    if (!role.canManage || target.isMe || target.isOwner) return false
    return role.isOwner || !target.isAdmin
}

/** 设置/取消管理员：仅群主，且不能操作自己/群主。 */
internal fun canSetMemberAdmin(role: GroupMemberRole, target: ChatMember): Boolean =
    role.isOwner && !target.isMe && !target.isOwner

/**
 * 执行一次群管理操作：独立 App（无 groupManageSource）提示 noPermission；
 * 其余在 IO 线程调用，成功/失败 Toast，成功后回调 [onSuccess]（用于刷新成员列表）。
 */
internal fun CoroutineScope.performGroupManageAction(
    context: Context,
    noPermissionMessage: String,
    successMessage: String,
    failedMessage: String,
    onSuccess: () -> Unit = {},
    call: () -> Boolean,
) {
    if (AppDataSources.groupManageSource == null) {
        Toast.makeText(context, noPermissionMessage, Toast.LENGTH_SHORT).show()
        return
    }
    launch {
        val ok = withContext(Dispatchers.IO) { runCatching(call).getOrDefault(false) }
        Toast.makeText(
            context,
            if (ok) successMessage else failedMessage,
            Toast.LENGTH_SHORT,
        ).show()
        if (ok) onSuccess()
    }
}

/** 群成员长按菜单动作（M3E / Miuix 共用）。 */
internal enum class MemberMenuAction {
    MUTE_10M,
    MUTE_1H,
    MUTE_1D,
    CUSTOM_MUTE,
    UNMUTE,
    TOGGLE_ADMIN,
    EDIT_CARD,
    KICK,
}

/**
 * 成员行长按检测（与长按消息气泡同款）：用 Initial pass 自己检测长按，
 * 触发后消费后续事件，避免松手时触发行点击进入资料页；长按带触感。
 */
@Composable
internal fun Modifier.memberLongPress(onLongPress: (Offset) -> Unit): Modifier {
    val haptic = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    return pointerInput(onLongPress) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val releasedBeforeLongPress = withTimeoutOrNull(
                viewConfiguration.longPressTimeoutMillis,
            ) {
                var finished = false
                while (!finished) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed) {
                        finished = true
                    } else if (
                        (change.position - down.position).getDistance() >
                        viewConfiguration.touchSlop
                    ) {
                        finished = true
                    }
                }
                true
            } != null
            if (!releasedBeforeLongPress) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                // 回调长按点位置（相对行内坐标），供 M3E 菜单锚点使用，与消息气泡菜单一致。
                onLongPress(down.position)
                // 长按已触发：消费后续事件，避免松手触发子项点击。
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach { it.consume() }
                    if (event.changes.none { it.pressed }) break
                }
            }
        }
    }
}

/** 自定义禁言时长（天/时/分）→ 总秒数：空按 0，天钳制 0..365、时 0..23、分 0..59。 */
internal fun customMuteSeconds(days: String, hours: String, minutes: String): Int {
    val d = (days.toIntOrNull() ?: 0).coerceIn(0, 365)
    val h = (hours.toIntOrNull() ?: 0).coerceIn(0, 23)
    val m = (minutes.toIntOrNull() ?: 0).coerceIn(0, 59)
    return d * 86400 + h * 3600 + m * 60
}

/** 数字输入过滤：仅保留数字并限制最大位数。 */
internal fun sanitizeDurationInput(value: String, maxLength: Int): String =
    value.filter { it.isDigit() }.take(maxLength)
