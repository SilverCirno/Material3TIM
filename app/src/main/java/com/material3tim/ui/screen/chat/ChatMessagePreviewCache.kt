package com.material3tim.ui.screen.chat

import com.material3tim.data.model.ChatMessage
import com.material3tim.data.source.AppDataSources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 会话消息内存预览缓存：进入聊天时先用缓存渲染首帧，避免导航转场期间聊天列表空白。
 *
 * - 纯内存、不持久化（会话删除/退出登录时忽略即可）；
 * - LRU 上限 [MAX_CHATS] 个会话，每会话保留最近 [MAX_MESSAGES] 条消息；
 * - 可从主线程（进入聊天时同步读）与 IO 线程（loadMessages 预取后写）安全访问；
 * - [prefetch]/[awaitOrLoad] 共享同一个 in-flight 请求：点击会话时立即开始加载，
 *   聊天页进入时直接 await 该请求，避免转场结束后再发一次重复的 loadMessages。
 */
internal object ChatMessagePreviewCache {

    private const val MAX_CHATS = 20
    private const val MAX_MESSAGES = 40

    private val cache = object : LinkedHashMap<String, List<ChatMessage>>(16, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<ChatMessage>>,
        ): Boolean = size > MAX_CHATS
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inflight = ConcurrentHashMap<String, Deferred<List<ChatMessage>?>>()

    /** 取该会话的预览消息（升序，最近 [MAX_MESSAGES] 条）；无缓存返回空列表。 */
    @Synchronized
    fun get(chatId: String): List<ChatMessage> = cache[chatId].orEmpty()

    /** 写入/更新该会话的预览消息；只保留最近 [MAX_MESSAGES] 条。 */
    @Synchronized
    fun save(chatId: String, messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        cache[chatId] = if (messages.size > MAX_MESSAGES) {
            messages.takeLast(MAX_MESSAGES)
        } else {
            messages
        }
    }

    @Synchronized
    fun clear() {
        cache.clear()
        inflight.clear()
    }

    /**
     * 点击会话时调用：立即在 IO 线程开始加载该会话消息（结果写入缓存）。
     * 已有缓存或已有 in-flight 请求时直接复用，不重复请求。
     */
    @Synchronized
    fun prefetch(chatId: String) {
        if (chatId.isBlank() || cache.containsKey(chatId) || inflight.containsKey(chatId)) return
        val deferred = scope.async {
            runCatching { AppDataSources.messageSource?.loadMessages(chatId) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.also { save(chatId, it) }
        }
        inflight[chatId] = deferred
        deferred.invokeOnCompletion {
            // 完成后从 in-flight 表移除；缓存已写入，后续进入直接命中缓存。
            inflight.remove(chatId, deferred)
        }
    }

    /**
     * 聊天页进入时调用：若点击时已启动预取则直接 await 同一请求（转场结束即可显示），
     * 否则现拉一次；结果写缓存但不重复覆盖相同内容（由调用方判断）。
     */
    suspend fun awaitOrLoad(chatId: String): List<ChatMessage>? {
        inflight[chatId]?.let { deferred ->
            return runCatching { deferred.await() }.getOrNull()
        }
        val loaded = withContext(Dispatchers.IO) {
            runCatching { AppDataSources.messageSource?.loadMessages(chatId) }.getOrNull()
        }
        if (!loaded.isNullOrEmpty()) save(chatId, loaded)
        return loaded
    }
}
