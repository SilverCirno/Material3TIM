package com.material3tim.ui.screen.chat

import android.app.DownloadManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.material3tim.R
import com.material3tim.data.model.ChatMessage
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors
import com.material3tim.ui.navigation3.LocalNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * 视频播放页（M3E 样式，与图片详情页一致）：
 * - M3E 顶栏：返回 + 「视频」+ 右上角下载按钮（正确处理状态栏安全区）
 * - 顶栏与播放控制条（进度/时间/全屏）显示状态完全一致：点一下显示，再点隐藏，3 秒自动隐藏
 * - 全屏时顶栏始终隐藏；控制条仍可点击显隐
 * - 进度条吸附在屏幕最底部，使用莫奈主题色
 */
@Composable
fun ChatVideoScreen(
    videoPath: String,
    thumbnailPath: String,
    chatId: String,
    msgId: Long,
    elementId: Long,
    videoWidth: Int = 0,
    videoHeight: Int = 0,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember(videoPath) {
        mutableStateOf(videoPath.takeIf { it.isNotBlank() && File(it).isFile }.orEmpty())
    }
    var loading by remember(videoPath, chatId, msgId, elementId) { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(videoPath, chatId, msgId, elementId) {
        if (source.isNotBlank()) return@LaunchedEffect
        if (chatId.isBlank() || elementId == 0L) return@LaunchedEffect
        loading = true
        val url = withContext(Dispatchers.IO) {
            AppDataSources.messageSource?.videoPlayUrl(chatId, msgId, elementId)
        }
        if (!url.isNullOrBlank()) source = url
        loading = false
    }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }
    BackHandler(enabled = fullscreen) { fullscreen = false }

    val ratio = remember(videoWidth, videoHeight) {
        if (videoWidth > 0 && videoHeight > 0) {
            videoWidth.toFloat() / videoHeight
        } else {
            16f / 9f
        }
    }

    // 整页全屏播放；顶栏/底栏都是悬浮层，显隐不会改变视频区域大小（避免画面上下跳）。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            source.isNotBlank() -> VideoStage(
                source = source,
                ratio = ratio,
                accent = MaterialTheme.colorScheme.primary,
                fullscreen = fullscreen,
                controlsVisible = controlsVisible,
                onToggleFullscreen = {
                    fullscreen = !fullscreen
                    controlsVisible = true
                },
                onToggleControls = { controlsVisible = !controlsVisible },
            )
            loading -> CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                VideoPlaceholder(thumbnailPath = thumbnailPath)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && !fullscreen,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.66f), Color.Transparent),
                        ),
                    )
                    .statusBarsPadding(),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.chat_video_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.chat_detail_back),
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val message = withContext(Dispatchers.IO) {
                                        saveVideo(context = context, source = source)
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = stringResource(R.string.chat_video_save),
                                tint = Color.White,
                            )
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    windowInsets = WindowInsets(0),
                )
            }
        }
    }
}

@Composable
private fun VideoStage(
    source: String,
    ratio: Float,
    accent: Color,
    fullscreen: Boolean,
    controlsVisible: Boolean,
    onToggleFullscreen: () -> Unit,
    onToggleControls: () -> Unit,
) {
    val context = LocalContext.current
    var videoView by remember { mutableStateOf<android.widget.VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var buffering by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }

    DisposableEffect(source) {
        onDispose { videoView?.stopPlayback() }
    }
    LaunchedEffect(videoView) {
        while (true) {
            val view = videoView ?: run {
                delay(300)
                return@LaunchedEffect
            }
            if (!dragging) {
                position = view.currentPosition.toLong()
                if (view.duration > 0) duration = view.duration.toLong()
            }
            isPlaying = view.isPlaying
            buffering = duration <= 0L && !view.isPlaying
            delay(250)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onToggleControls() },
    ) {
        val videoModifier = if (ratio >= 1f) {
            Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
        } else {
            Modifier
                .fillMaxHeight()
                .aspectRatio(ratio)
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .then(videoModifier)
                .clip(RoundedCornerShape(if (fullscreen) 0.dp else 14.dp)),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    android.widget.VideoView(ctx).apply {
                        setVideoPath(source)
                        setOnPreparedListener { mp ->
                            duration = mp.duration.toLong()
                            buffering = false
                            start()
                        }
                        setOnCompletionListener { isPlaying = false }
                        setOnErrorListener { _, _, _ ->
                            buffering = false
                            true
                        }
                    }.also { videoView = it }
                },
            )
        }

        if (buffering) {
            CircularProgressIndicator(
                color = accent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(38.dp),
            )
        } else if (controlsVisible) {
            IconButton(
                onClick = {
                    val view = videoView
                    if (view != null && view.isPlaying) {
                        view.pause()
                        isPlaying = false
                    } else {
                        view?.start()
                        isPlaying = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f)),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.chat_video_title),
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
        }

        // 底部控制条：固定在屏幕最底部，莫奈主题色；与顶栏共用 controlsVisible 状态
        if (controlsVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        ),
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                val max = duration.coerceAtLeast(1L)
                Slider(
                    value = position.coerceIn(0L, max).toFloat(),
                    onValueChange = {
                        dragging = true
                        position = it.toLong()
                    },
                    onValueChangeFinished = {
                        videoView?.seekTo(position.toInt())
                        dragging = false
                    },
                    valueRange = 0f..max.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = accent.copy(alpha = 0.28f),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatVideoDuration((position / 1000L).toInt()),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "/",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatVideoDuration((duration / 1000L).toInt()),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = if (fullscreen) {
                                Icons.Filled.FullscreenExit
                            } else {
                                Icons.Filled.Fullscreen
                            },
                            contentDescription = stringResource(R.string.chat_video_fullscreen),
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPlaceholder(thumbnailPath: String) {
    val bitmap = com.material3tim.ui.component.rememberLocalImage(thumbnailPath, maxDimension = 1024)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.chat_video_title),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
        }
        Text(
            text = stringResource(R.string.chat_video_missing),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(24.dp),
        )
    }
}

/** 保存视频：本地文件复制到相册；在线地址交给系统 DownloadManager 下载。 */
private fun saveVideo(context: android.content.Context, source: String): String {
    if (source.isBlank()) return context.getString(R.string.chat_video_save_failed)
    return runCatching {
        val file = File(source)
        if (file.isFile) {
            val name = "M3Tim_${System.currentTimeMillis()}.${file.extension.ifBlank { "mp4" }}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_MOVIES + "/Material3Tim",
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return context.getString(R.string.chat_video_save_failed)
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                } ?: return context.getString(R.string.chat_video_save_failed)
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                context.getString(R.string.chat_video_saved)
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "Material3Tim",
                ).apply { mkdirs() }
                file.copyTo(File(dir, name), overwrite = true)
                context.getString(R.string.chat_video_saved)
            }
        } else {
            val manager = context.getSystemService(DownloadManager::class.java)
            val request = DownloadManager.Request(Uri.parse(source)).apply {
                setTitle("Material3Tim 视频")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_MOVIES,
                    "Material3Tim/M3Tim_${System.currentTimeMillis()}.mp4",
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            manager.enqueue(request)
            context.getString(R.string.chat_video_downloading)
        }
    }.getOrDefault(context.getString(R.string.chat_video_save_failed))
}

/**
 * 视频气泡缩略图兜底：优先用消息自带的 [ChatMessage.videoThumbPath]；
 * 缺失/文件不存在但 [ChatMessage.videoPath] 是本地可读文件时，用 MediaMetadataRetriever
 * 抽首帧写入 cache（videoFrame_<msgId>_<hash>.jpg），返回可用缩略图路径。
 *
 * 抽帧是阻塞 IO：放在 LaunchedEffect + Dispatchers.IO 异步执行，不在组合期同步做。
 * 无可用来源时返回 null（气泡显示黑底播放按钮占位）。
 */
@Composable
internal fun rememberVideoThumbnailPath(message: ChatMessage): String? {
    val context = LocalContext.current
    val direct = message.videoThumbPath?.takeIf { it.isNotBlank() && File(it).isFile }
    var extracted by remember(message.id, message.videoPath, direct) {
        mutableStateOf<String?>(null)
    }
    LaunchedEffect(message.id, message.videoPath, direct) {
        if (direct != null || extracted != null) return@LaunchedEffect
        val localVideo = message.videoPath
            ?.takeIf { it.isNotBlank() && File(it).isFile }
            ?: return@LaunchedEffect
        extracted = withContext(Dispatchers.IO) {
            probePickedVideo(
                context = context,
                videoPath = localVideo,
                thumbFileName = "videoFrame_${message.chatRecordMsgId}_${localVideo.hashCode()}.jpg",
            )?.thumbnailPath
        }
    }
    return direct ?: extracted
}

/** 与 bridge 日志一致的 tag，便于真机过滤视频探测链路。 */
private const val VIDEO_PROBE_TAG = "Material3Tim"

/** 视频文件探测结果：时长 / 显示宽高（已处理 90°/270° 旋转）/ 首帧缩略图。 */
internal data class PickedVideoProbe(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val thumbnailPath: String?,
)

/** MediaMetadataRetriever 会话：同时持有可选的 fd 输入流，close 时一起释放。 */
private class RetrieverSession(
    val retriever: MediaMetadataRetriever,
    private val source: java.io.Closeable? = null,
) : java.io.Closeable {
    override fun close() {
        runCatching { retriever.release() }
        runCatching { source?.close() }
    }
}

/**
 * 打开 MediaMetadataRetriever：先直接路径，失败再试 context+Uri，再试 FileDescriptor
 * （部分机型上相册 provider 拷出的缓存 mp4 只有 fd 方式能读）。每次尝试/失败都打日志。
 */
private fun openVideoRetriever(
    context: android.content.Context,
    file: File,
): RetrieverSession? {
    val byPath = MediaMetadataRetriever()
    val pathOk = runCatching { byPath.setDataSource(file.absolutePath) }
        .onFailure { Log.w(VIDEO_PROBE_TAG, "probe: setDataSource(path) failed: ${file.absolutePath}", it) }
        .isSuccess
    Log.i(VIDEO_PROBE_TAG, "probe: setDataSource(path) ok=$pathOk path=${file.absolutePath}")
    if (pathOk) return RetrieverSession(byPath)
    runCatching { byPath.release() }

    val viaUri = MediaMetadataRetriever()
    val uri = Uri.fromFile(file)
    val uriOk = runCatching { viaUri.setDataSource(context, uri) }
        .onFailure { Log.w(VIDEO_PROBE_TAG, "probe: setDataSource(uri) failed: $uri", it) }
        .isSuccess
    Log.i(VIDEO_PROBE_TAG, "probe: setDataSource(uri) ok=$uriOk uri=$uri")
    if (uriOk) return RetrieverSession(viaUri)
    runCatching { viaUri.release() }

    return runCatching {
        val stream = FileInputStream(file)
        val byFd = MediaMetadataRetriever()
        try {
            byFd.setDataSource(stream.fd)
        } catch (t: Throwable) {
            // setDataSource 失败时释放已创建的资源，避免 fd/流泄漏。
            runCatching { byFd.release() }
            runCatching { stream.close() }
            throw t
        }
        Log.i(VIDEO_PROBE_TAG, "probe: setDataSource(fd) ok=true")
        RetrieverSession(byFd, stream)
    }.onFailure {
        Log.w(VIDEO_PROBE_TAG, "probe: setDataSource(fd) failed: ${file.absolutePath}", it)
    }.getOrNull()
}

private fun MediaMetadataRetriever.metaLong(key: Int): Long =
    extractMetadata(key)?.toLongOrNull() ?: 0L

private fun MediaMetadataRetriever.metaInt(key: Int): Int =
    extractMetadata(key)?.toIntOrNull() ?: 0

/**
 * MediaExtractor 兜底：读视频轨 MediaFormat 的宽高；duration 先取 KEY_DURATION，
 * 缺失时用 sampleTime 逐帧估算（有 20000 帧上限保护）。失败返回 null。
 */
private fun probeByMediaExtractor(file: File): PickedVideoProbe? = runCatching {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(file.absolutePath)
        var width = 0
        var height = 0
        var rotation = 0
        var durationUs = 0L
        for (index in 0 until extractor.trackCount) {
            val format = runCatching { extractor.getTrackFormat(index) }.getOrNull() ?: continue
            val mime = runCatching { format.getString(MediaFormat.KEY_MIME) }.getOrNull().orEmpty()
            if (!mime.startsWith("video/")) continue
            width = runCatching { format.getInteger(MediaFormat.KEY_WIDTH) }.getOrNull() ?: 0
            height = runCatching { format.getInteger(MediaFormat.KEY_HEIGHT) }.getOrNull() ?: 0
            rotation = runCatching { format.getInteger(MediaFormat.KEY_ROTATION) }.getOrNull() ?: 0
            durationUs = runCatching { format.getLong(MediaFormat.KEY_DURATION) }.getOrNull() ?: 0L
            if (durationUs <= 0L) {
                // 部分容器轨道 format 不带 duration：逐帧 sampleTime 估算（有上限保护）。
                runCatching { extractor.selectTrack(index) }
                var lastTimeUs = 0L
                var samples = 0
                while (samples < 20000) {
                    val timeUs = extractor.getSampleTime()
                    if (timeUs < 0L) break
                    lastTimeUs = timeUs
                    if (!extractor.advance()) break
                    samples++
                }
                runCatching { extractor.unselectTrack(index) }
                durationUs = lastTimeUs
            }
            Log.i(
                VIDEO_PROBE_TAG,
                "probe extractor: mime=$mime w=$width h=$height rotation=$rotation durUs=$durationUs",
            )
            break
        }
        if (rotation == 90 || rotation == 270) {
            val swap = width
            width = height
            height = swap
        }
        PickedVideoProbe(
            durationMs = (durationUs / 1000L).coerceAtLeast(0L),
            width = width.coerceAtLeast(0),
            height = height.coerceAtLeast(0),
            thumbnailPath = null,
        )
    } finally {
        runCatching { extractor.release() }
    }
}.onFailure {
    Log.w(VIDEO_PROBE_TAG, "probe extractor failed: ${file.absolutePath}", it)
}.getOrNull()

/** 抽首帧并压 JPEG 到 cacheDir/video_thumbs，失败返回 null。 */
private fun extractFirstFrame(
    context: android.content.Context,
    retriever: MediaMetadataRetriever,
    thumbFileName: String,
): String? {
    val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        ?: return null
    val dir = File(context.cacheDir, "video_thumbs").apply { mkdirs() }
    val target = File(dir, thumbFileName)
    if (!target.isFile || target.length() <= 0L) {
        target.outputStream().use { output ->
            frame.compress(Bitmap.CompressFormat.JPEG, 80, output)
        }
    }
    frame.recycle()
    return target.takeIf { it.isFile && it.length() > 0L }?.absolutePath
}

/**
 * 视频文件探测（best-effort，供相册发送与气泡缩略图兜底复用）：
 * - 先 MediaMetadataRetriever，数据源依次尝试 path → context+Uri → FileDescriptor；
 * - 时长/宽高 key 读不到时用 MediaExtractor 视频轨 MediaFormat 兜底（含 sampleTime 估时长）；
 * - 90°/270° 旋转交换宽高；首帧失败 thumbnailPath=null；
 * - 全程打 [VIDEO_PROBE_TAG] 日志；只要文件可读就返回结果（可能全 0），null 仅表示文件不可读。
 * 阻塞 IO，调用方需在 Dispatchers.IO 执行。
 */
internal fun probePickedVideo(
    context: android.content.Context,
    videoPath: String,
    thumbFileName: String,
): PickedVideoProbe? {
    val file = File(videoPath)
    Log.i(
        VIDEO_PROBE_TAG,
        "probe start: path=$videoPath exists=${file.isFile} size=${file.length()}",
    )
    if (!file.isFile || file.length() <= 0L) {
        Log.w(VIDEO_PROBE_TAG, "probe: file missing/empty path=$videoPath")
        return null
    }
    var durationMs = 0L
    var width = 0
    var height = 0
    var rotation = 0
    var thumbnailPath: String? = null

    val session = openVideoRetriever(context, file)
    if (session != null) {
        try {
            val retriever = session.retriever
            durationMs = retriever.metaLong(MediaMetadataRetriever.METADATA_KEY_DURATION)
            width = retriever.metaInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            height = retriever.metaInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            rotation = retriever.metaInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            Log.i(
                VIDEO_PROBE_TAG,
                "probe retriever: durMs=$durationMs w=$width h=$height rotation=$rotation",
            )
            thumbnailPath = runCatching { extractFirstFrame(context, retriever, thumbFileName) }
                .onFailure { Log.w(VIDEO_PROBE_TAG, "probe: extract frame failed", it) }
                .getOrNull()
            Log.i(VIDEO_PROBE_TAG, "probe retriever: thumb=$thumbnailPath")
        } finally {
            runCatching { session.close() }
        }
    }

    if (durationMs <= 0L || width <= 0 || height <= 0) {
        val fallback = probeByMediaExtractor(file)
        if (fallback != null) {
            if (durationMs <= 0L) durationMs = fallback.durationMs
            if (width <= 0 || height <= 0) {
                width = fallback.width
                height = fallback.height
            }
            Log.i(
                VIDEO_PROBE_TAG,
                "probe fallback(extractor): durMs=$durationMs w=$width h=$height",
            )
        }
    }
    if (rotation == 90 || rotation == 270) {
        val swap = width
        width = height
        height = swap
    }
    Log.i(
        VIDEO_PROBE_TAG,
        "probe done: durMs=$durationMs w=$width h=$height thumb=$thumbnailPath",
    )
    return PickedVideoProbe(
        durationMs = durationMs.coerceAtLeast(0L),
        width = width.coerceAtLeast(0),
        height = height.coerceAtLeast(0),
        thumbnailPath = thumbnailPath,
    )
}
