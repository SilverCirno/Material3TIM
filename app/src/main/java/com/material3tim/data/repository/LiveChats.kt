package com.material3tim.data.repository

import com.material3tim.data.model.ChatItem
import com.material3tim.data.source.AppDataSources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 会话列表的“实时数据”缓存。
 *
 * - 独立 App：始终为 null，UI 回落到 [ChatRepository.sampleChats]。
 * - Xposed 宿主：由 [AppDataSources.chatSource] 从 TIM Kernel 拉取真实会话，写进这里；
 *   UI 优先使用这份数据。
 */
object LiveChats {

    @Volatile
    private var live: List<ChatItem>? = null

    /** 从联系人详情进入聊天时补出来的会话（不在 TIM 最近会话里）。 */
    private val extra = java.util.concurrent.ConcurrentHashMap<String, ChatItem>()

    private val _liveFlow = MutableStateFlow<List<ChatItem>?>(null)

    /** UI 可以订阅真实会话变化（发送消息后刷新列表用）。 */
    val liveFlow: StateFlow<List<ChatItem>?> = _liveFlow.asStateFlow()

    /** 当前应展示的会话（真实数据优先）。 */
    val current: List<ChatItem>
        get() = live ?: ChatRepository.sampleChats

    /** 仅真实数据（没有则 null）。 */
    fun peek(): List<ChatItem>? = live

    fun chatById(id: String): ChatItem? =
        live?.firstOrNull { it.id == id } ?: extra[id]

    fun upsert(chat: ChatItem) {
        extra[chat.id] = chat
        _liveFlow.value = live
    }

    val isLive: Boolean
        get() = live != null

    /** 从已注册的数据源重新加载；返回是否加载成功。 */
    fun refresh(): List<ChatItem>? {
        val source = AppDataSources.chatSource ?: return null
        val chats = runCatching { source.loadChats() }.getOrNull() ?: return null
        update(chats)
        return chats
    }

    fun update(chats: List<ChatItem>?) {
        val previous = live
        live = chats
        // 新旧内容完全相等时不推 StateFlow，避免首页列表无意义重组（滑动掉帧）。
        // @Volatile 读取 + 比较存在竞态：最坏多一次刷新，可接受。
        if (previous == chats) return
        _liveFlow.value = chats
    }

    fun unreadCount(): Int = current.sumOf { it.unreadCount }
}

/**
 * 会话时间显示：今天 HH:mm / 昨天 / 星期X / M月d日 / yyyy/M/d。
 * 传入值同时兼容秒和毫秒时间戳（TIM 的最近会话用秒）。
 */
fun formatChatTimeLabel(raw: Long): String {
    if (raw <= 0L) return ""
    val millis = if (raw < 10_000_000_000L) raw * 1000L else raw
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val sameDay = sameYear && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (sameDay) {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    }
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    if (sameYear && yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
        return "昨天"
    }
    // 一周内显示星期几
    val diffDays = (now.timeInMillis - millis) / (24L * 60L * 60L * 1000L)
    if (diffDays in 0..6) {
        val weekNames = arrayOf("日", "一", "二", "三", "四", "五", "六")
        return "星期" + weekNames[then.get(Calendar.DAY_OF_WEEK) - 1]
    }
    return if (sameYear) {
        "${then.get(Calendar.MONTH) + 1}月${then.get(Calendar.DAY_OF_MONTH)}日"
    } else {
        "${then.get(Calendar.YEAR)}/${then.get(Calendar.MONTH) + 1}/${then.get(Calendar.DAY_OF_MONTH)}"
    }
}
