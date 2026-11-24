package com.material3tim.ui.screen.chat

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.material3tim.R
import com.material3tim.data.model.ContactItem
import com.material3tim.data.repository.ChatRepository
import com.material3tim.data.repository.LiveChats
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 创建群聊选人页入口（消息页加号菜单「创建群聊」→ `Route.CreateGroup`）。
 *
 * 联系人数据：宿主读 [AppDataSources.contactSource]（IO 线程）；独立 App 回退
 * [ChatRepository.contacts]（示例联系人），保证两种形态都能进页面操作。
 * 交互：点击整行切换选中（右侧 Checkbox 只是展示）；顶部右侧「确认(N)」在选中数 > 0 时可用，
 * 确认后 IO 调 [AppDataSources.groupCreateSource]?.createGroup，成功替换当前页进入新群聊天。
 */
@Composable
fun CreateGroupScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // null = 联系人还在加载（用于展示 loading 态）。
    var contacts by remember { mutableStateOf<List<ContactItem>?>(null) }
    var selectedIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val live = withContext(Dispatchers.IO) {
            runCatching { AppDataSources.contactSource?.loadContacts() }.getOrNull()
        }
        // 宿主拉取失败或独立 App（contactSource == null）都回退示例联系人。
        contacts = live ?: ChatRepository.contacts
    }

    val successToast = stringResource(R.string.create_group_success)
    val failedToast = stringResource(R.string.create_group_failed)
    val hostOnlyToast = stringResource(R.string.create_group_host_only)

    val contactList = contacts.orEmpty()
    val onToggle: (ContactItem) -> Unit = { contact ->
        selectedIds = if (contact.id in selectedIds) {
            selectedIds - contact.id
        } else {
            selectedIds + contact.id
        }
    }
    val onConfirm: () -> Unit = {
        val selectedContacts = contactList.filter { it.id in selectedIds }
        if (!creating && selectedContacts.isNotEmpty()) {
            val source = AppDataSources.groupCreateSource
            if (source == null) {
                // 独立 App：没有创建群聊能力，明确提示而不是静默失败。
                Toast.makeText(context, hostOnlyToast, Toast.LENGTH_SHORT).show()
            } else {
                creating = true
                scope.launch {
                    val chatId = withContext(Dispatchers.IO) {
                        runCatching { source.createGroup(selectedContacts) }.getOrNull()
                    }
                    if (chatId.isNullOrBlank()) {
                        creating = false
                        Toast.makeText(context, failedToast, Toast.LENGTH_SHORT).show()
                    } else {
                        // 新群刚创建时最近会话缓存可能还没跟上：刷新并轮询等待 chatId 可查，
                        // 否则 ChatDetailScreen 会走 MissingPage。等待 6s 仍查不到就退回会话列表。
                        withContext(Dispatchers.IO) {
                            runCatching { LiveChats.refresh() }
                        }
                        var chat = ChatRepository.chatById(chatId)
                        var waited = 0
                        while (chat == null && waited < 20) {
                            delay(300)
                            chat = ChatRepository.chatById(chatId)
                            waited += 1
                        }
                        Toast.makeText(context, successToast, Toast.LENGTH_SHORT).show()
                        if (chat != null) {
                            // 用 replace 把「创建群聊」页替换成新群聊天页：返回时直接回会话列表。
                            navigator.replace(Route.ChatDetail(chatId))
                        } else {
                            navigator.pop()
                        }
                    }
                }
            }
        }
    }
    val onBack: () -> Unit = { if (!creating) navigator.pop() }

    when (LocalUiMode.current) {
        UiMode.Miuix -> CreateGroupMiuix(
            contacts = contactList,
            loading = contacts == null,
            selectedIds = selectedIds,
            creating = creating,
            onToggle = onToggle,
            onBack = onBack,
            onConfirm = onConfirm,
        )

        UiMode.Material -> CreateGroupMaterial(
            contacts = contactList,
            loading = contacts == null,
            selectedIds = selectedIds,
            creating = creating,
            onToggle = onToggle,
            onBack = onBack,
            onConfirm = onConfirm,
        )
    }
}
