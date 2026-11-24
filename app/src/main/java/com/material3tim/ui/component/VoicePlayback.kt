package com.material3tim.ui.component

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.material3tim.data.model.ChatMessage
import com.material3tim.data.source.AppDataSources
import com.material3tim.data.source.VoicePlaybackSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 语音消息播放控制器（全局单例，同一时间只播一条）。
 *
 * 播放后端：
 * - 宿主里优先用 [AppDataSources.voicePlaybackSource]（TIM CommonAudioPlayer，支持 silk/amr）；
 * - 独立 App（没有 voicePlaybackSource）回退到 [MediaPlayer] 播本地文件。
 *
 * 用法：
 * - 点击语音条 → [toggle]：同一条在播则停止；否则停止旧的并播放新的；
 * - 宿主播放期间每 [POLL_INTERVAL_MS] 轮询一次 `isVoicePlaying()`，播完自动清空 [playingMessageId]；
 * - 聊天页离开/切换会话时调用 [stop]（或 [release]）停止播放并取消所有协程。
 *
 * 状态 [playingMessageId] 是 Compose State，UI 直接据此切换播放/暂停图标与波形高亮；
 * 失败时返回 false（参数/文件问题）或回调 [toggle] 的 onPlayError（宿主异步播放失败），UI 负责 Toast。
 */
object VoicePlaybackController {

    /** 宿主播放中的轮询间隔：350ms 查一次内核是否仍在播。 */
    private const val POLL_INTERVAL_MS = 350L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var pollJob: Job? = null
    private var hostPlayJob: Job? = null

    /** 宿主播放请求代号：stop/切换/轮询完成时自增，用于丢弃过期协程的结果。 */
    private var hostGeneration: Int = 0

    /** 当前正在播放的消息 id；null 表示没有在播。 */
    var playingMessageId: String? by mutableStateOf(null)
        private set

    /** 最近一次播放失败的原因（便于排查；UI Toast 文案由调用方决定）。 */
    var lastError: String? by mutableStateOf(null)
        private set

    /**
     * 当前播放进度 0..1（null = 没有在播/未知）。
     * 宿主播放中由轮询 `playbackPositionMs()/playbackDurationMs()` 计算；MediaPlayer 回退路径不提供进度。
     */
    var playbackProgress: Float? by mutableStateOf(null)
        private set

    /** 暂停中的消息 id；null 表示没有暂停中的语音。 */
    var pausedMessageId: String? by mutableStateOf(null)
        private set

    /** 暂停位置 0..1（暂停后进度条停在这里，再次点击继续播放）。 */
    var pausedProgress: Float? by mutableStateOf(null)
        private set

    /** 指定消息是否正在播放（给 UI 判断图标/波形高亮）。 */
    fun isPlaying(messageId: String?): Boolean =
        messageId != null && playingMessageId == messageId

    /** 进度条显示用：播放中→实时进度，暂停中→暂停位置，其它→0（点开前 0%）。 */
    fun progressFor(messageId: String): Float = when (messageId) {
        playingMessageId -> playbackProgress ?: 0f
        pausedMessageId -> pausedProgress ?: 0f
        else -> 0f
    }

    /**
     * 点击语音条（播放/暂停/继续）：
     * 1) 正在播本消息 → 暂停并停在当前进度；
     * 2) 本消息暂停中 → 从暂停位置继续；
     * 3) 其它情况 → 清掉播放/暂停状态后从头播放这一条。
     *
     * @param onPlayError 宿主异步播放/继续失败的兜底回调（在 Main 线程调用），UI 用来 Toast；
     *   文件缺失等同步失败直接返回 false。
     * @return 是否已开始处理（暂停/继续/开始播放）；false 表示文件缺失。
     */
    fun toggle(message: ChatMessage, onPlayError: () -> Unit = {}): Boolean {
        val hostSource = AppDataSources.voicePlaybackSource
        if (isPlaying(message.id)) {
            pauseCurrent(message, hostSource)
            return true
        }
        if (pausedMessageId == message.id) {
            resumePaused(message, hostSource, onPlayError)
            return true
        }
        stop()
        val path = message.mediaPath
        if (path.isNullOrBlank() || !File(path).isFile) {
            lastError = "voice file missing: $path"
            return false
        }
        if (hostSource != null) {
            playViaHost(hostSource, message, path, onPlayError)
            return true
        }
        return playWithMediaPlayer(message, path)
    }

    /**
     * 暂停当前播放：先记下当前位置、停轮询，再 IO 调 `pauseVoice()`；
     * bridge 不支持暂停时退化为 `stopVoice()`（进度条仍停在 pausedProgress 展示）。
     */
    private fun pauseCurrent(message: ChatMessage, source: VoicePlaybackSource?) {
        val position = playbackProgress ?: 0f
        // 先自增代号并取消轮询，避免迟到的 poll 被当成"自然播完"清掉暂停位置。
        hostGeneration++
        pollJob?.cancel()
        pollJob = null
        hostPlayJob?.cancel()
        hostPlayJob = null
        playingMessageId = null
        playbackProgress = null
        pausedMessageId = message.id
        pausedProgress = position
        lastError = null
        if (source != null) {
            scope.launch {
                val paused = withContext(Dispatchers.IO) {
                    runCatching { source.pauseVoice() }.getOrDefault(false)
                }
                // 不支持 pause 的桥实现：退化为停止（仍保留 pausedProgress 供 UI 显示）。
                if (!paused) {
                    withContext(Dispatchers.IO) { runCatching { source.stopVoice() } }
                }
                // 用内核实际暂停位置校准（暂停动作发生在最后一次轮询之后，最多差 350ms）。
                val snapshot = withContext(Dispatchers.IO) {
                    runCatching {
                        source.playbackPositionMs().toFloat() to
                            source.playbackDurationMs().toFloat()
                    }.getOrNull()
                }
                val (positionMs, durationMs) = snapshot ?: return@launch
                if (pausedMessageId == message.id && positionMs > 0f && durationMs > 0f) {
                    pausedProgress = (positionMs / durationMs).coerceIn(0f, 1f)
                }
            }
        } else {
            // MediaPlayer 回退没有暂停续播语义：停止并释放（下次点击从头播）。
            runCatching { mediaPlayer?.stop() }
            runCatching { mediaPlayer?.release() }
            mediaPlayer = null
        }
    }

    /** 从暂停位置继续播放：乐观设置进度并重启轮询；失败回到暂停态并回调 Toast。 */
    private fun resumePaused(
        message: ChatMessage,
        source: VoicePlaybackSource?,
        onPlayError: () -> Unit,
    ) {
        val resumeAt = (pausedProgress ?: 0f).coerceIn(0f, 1f)
        pausedMessageId = null
        pausedProgress = null
        if (source == null) {
            // MediaPlayer 回退：没有 resume 语义，重新从头播放。
            val path = message.mediaPath
            if (path.isNullOrBlank() || !File(path).isFile) {
                lastError = "voice file missing: $path"
                onPlayError()
                return
            }
            if (!playWithMediaPlayer(message, path)) onPlayError()
            return
        }
        val generation = ++hostGeneration
        playingMessageId = message.id
        playbackProgress = resumeAt
        lastError = null
        hostPlayJob = scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { source.resumeVoice() }.getOrDefault(false)
            }
            if (generation != hostGeneration) return@launch
            if (!ok) {
                // 继续失败：回到暂停态（保留位置），UI Toast 后用户可再点一次重试。
                playingMessageId = null
                playbackProgress = null
                pausedMessageId = message.id
                pausedProgress = resumeAt
                lastError = "voice resume failed"
                onPlayError()
                return@launch
            }
            startHostPolling(source, generation)
        }
    }

    /** 宿主播放：先乐观设置 playingMessageId，IO 线程调 playVoice，成功后开始轮询。 */
    private fun playViaHost(
        source: VoicePlaybackSource,
        message: ChatMessage,
        path: String,
        onPlayError: () -> Unit,
    ) {
        val generation = ++hostGeneration
        playingMessageId = message.id
        playbackProgress = 0f
        lastError = null
        hostPlayJob = scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { source.playVoice(path) }.getOrDefault(false)
            }
            // 已被 stop/切换成别的消息：丢弃本次结果。
            if (generation != hostGeneration) return@launch
            if (!ok) {
                playingMessageId = null
                lastError = "voice play failed: $path"
                onPlayError()
                return@launch
            }
            startHostPolling(source, generation)
        }
    }

    /** 宿主播放中轮询：内核播完返回 false 时清空状态；播放中同时更新 [playbackProgress]。 */
    private fun startHostPolling(source: VoicePlaybackSource, generation: Int) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                if (generation != hostGeneration) return@launch
                val snapshot = withContext(Dispatchers.IO) {
                    runCatching {
                        val playing = source.isVoicePlaying()
                        val position = source.playbackPositionMs().toFloat()
                        val duration = source.playbackDurationMs().toFloat()
                        Triple(playing, position, duration)
                    }.getOrNull()
                }
                if (generation != hostGeneration) return@launch
                if (snapshot == null) {
                    // 内核查询异常：按播放结束处理，清状态并停止轮询，避免无限轮询。
                    if (generation == hostGeneration) {
                        playingMessageId = null
                        playbackProgress = null
                        hostGeneration++
                    }
                    pollJob = null
                    return@launch
                }
                val (stillPlaying, positionMs, durationMs) = snapshot
                if (!stillPlaying) {
                    if (generation == hostGeneration) {
                        playingMessageId = null
                        playbackProgress = null
                        // 播完自增代号，让后续迟到的 play 请求失效。
                        hostGeneration++
                    }
                    pollJob = null
                    return@launch
                }
                playbackProgress = if (durationMs > 0f) {
                    (positionMs / durationMs).coerceIn(0f, 1f)
                } else {
                    null
                }
            }
        }
    }

    /**
     * 拖动进度条 seek：仅对正在播放的语音生效（未播放静默忽略，不报错）。
     * duration 优先用宿主 [VoicePlaybackSource.playbackDurationMs]，否则用消息时长 [ChatMessage.voiceSeconds]；
     * 乐观更新 [playbackProgress]，seek 失败恢复原进度。
     */
    fun seekTo(message: ChatMessage, fraction: Float) {
        val playing = isPlaying(message.id)
        val paused = pausedMessageId == message.id
        if (!playing && !paused) return
        val source = AppDataSources.voicePlaybackSource ?: return
        val target = fraction.coerceIn(0f, 1f)
        val previous = if (playing) playbackProgress else pausedProgress
        if (playing) playbackProgress = target else pausedProgress = target
        scope.launch {
            val durationMs = withContext(Dispatchers.IO) {
                runCatching {
                    val fromSource = source.playbackDurationMs()
                    if (fromSource > 0) fromSource else message.voiceSeconds * 1000
                }.getOrDefault(message.voiceSeconds * 1000)
            }
            if (durationMs <= 0) return@launch
            val ok = withContext(Dispatchers.IO) {
                runCatching { source.seekVoice((target * durationMs).toInt()) }.getOrDefault(false)
            }
            if (!ok) {
                // 失败静默：恢复原进度（暂停态不启动播放，只回退显示位置）。
                if (playing) playbackProgress = previous else pausedProgress = previous
            }
        }
    }

    /** 独立 App 回退：MediaPlayer 播本地文件（silk 可能不支持，失败返回 false）。 */
    private fun playWithMediaPlayer(message: ChatMessage, path: String): Boolean {
        return runCatching {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build(),
                )
                setDataSource(path)
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
            playingMessageId = message.id
            playbackProgress = null
            lastError = null
            true
        }.getOrElse { throwable ->
            lastError = throwable.message
            stop()
            false
        }
    }

    /** 停止播放（宿主播放器 + MediaPlayer 回退都清理），并取消轮询/在途请求。 */
    fun stop() {
        hostGeneration++
        pollJob?.cancel()
        pollJob = null
        hostPlayJob?.cancel()
        hostPlayJob = null
        AppDataSources.voicePlaybackSource?.let { source ->
            runCatching { source.stopVoice() }
        }
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        playingMessageId = null
        playbackProgress = null
        pausedMessageId = null
        pausedProgress = null
    }

    /** 页面销毁时释放资源（当前实现等价于 [stop]）。 */
    fun release() = stop()
}
