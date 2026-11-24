package com.material3tim.data.source

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI 进程内的轻量运行时状态（宿主模式）：
 * - 供通知/后台提醒判断 App 是否在前台、当前打开的是哪个会话（后台桥接读取）；
 * - 前台/会话状态由 Compose 层（M3TimRoot / ChatDetailScreen）写入。
 */
object AppUiState {

    /** App 是否有处于 RESUMED 的 Activity。 */
    @Volatile
    var isForeground: Boolean = false

    /** 当前正在查看的会话 id（不在聊天页为 null）。 */
    @Volatile
    var currentChatId: String? = null
}

/**
 * 通知点击后的页面跳转请求。
 *
 * 通知的 PendingIntent 给 TIM 主 Activity 带上 [EXTRA_CHAT_ID] extra；TimUiHook 在
 * Activity.onResume/onNewIntent 时取出并转成一次 [requests]，M3TimRoot 收集后 push 聊天页。
 */
object NavigationRequests {

    /** 通知 → 打开会话的 Intent extra key（与 TimBridge 里创建 PendingIntent 时保持一致）。 */
    const val EXTRA_CHAT_ID: String = "m3tim_chat_id"

    private val _requests = MutableStateFlow<String?>(null)
    val requests: StateFlow<String?> = _requests.asStateFlow()

    /** 记录一次「请打开该会话」的请求（覆盖旧请求）。 */
    fun request(chatId: String?) {
        val id = chatId?.takeIf { it.isNotBlank() } ?: return
        _requests.value = id
    }

    /** 从 Intent 里读 extra 并发起跳转；返回是否读到了会话 id。 */
    fun requestFromIntent(intent: Intent?): Boolean {
        val id = intent?.getStringExtra(EXTRA_CHAT_ID)?.takeIf { it.isNotBlank() } ?: return false
        intent.removeExtra(EXTRA_CHAT_ID)
        request(id)
        return true
    }

    /** UI 层处理完一次跳转后调用（清空，避免重复 push）。 */
    fun consume(chatId: String) {
        if (_requests.value == chatId) _requests.value = null
    }
}
