package com.material3tim.ui.screen.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.material3tim.R
import com.material3tim.ui.component.material.ExpressiveAlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private const val MaxImageBytes = 10 * 1024 * 1024
private val imageWorkers = Dispatchers.IO.limitedParallelism(3)

/** 只请求 QQ 官方媒体域名。不发送 WebView Cookie，不落盘或记录临时媒体 URL。 */
internal class QZonePhotoLoader {
    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(url: String, avatar: Boolean = false): Bitmap? = withContext(imageWorkers) {
        val key = if (avatar) "avatar:$url" else url
        cache.get(key) ?: runCatching { download(url, avatar) }.getOrNull()?.also { cache.put(key, it) }
    }

    private fun download(url: String, avatar: Boolean): Bitmap? {
        var candidate = url
        repeat(3) {
            val safeUrl = (if (avatar) qZoneAvatarUrl(candidate) else qZoneMediaUrl(candidate, video = false))
                ?: return null
            val request = URL(safeUrl).openConnection() as HttpsURLConnection
            try {
                request.instanceFollowRedirects = false
                request.connectTimeout = 5000
                request.readTimeout = 7000
                request.setRequestProperty("Referer", "https://user.qzone.qq.com/")
                when (request.responseCode) {
                    in 300..399 -> {
                        val next = request.getHeaderField("Location") ?: return null
                        candidate = URL(URL(safeUrl), next).toString()
                    }
                    200 -> {
                        if (request.contentLengthLong > MaxImageBytes) return null
                        val bytes = request.inputStream.use { input ->
                            val buffer = ByteArray(8192)
                            val output = ByteArrayOutputStream()
                            while (true) {
                                val count = input.read(buffer)
                                if (count == -1) break
                                if (output.size() + count > MaxImageBytes) return null
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        if (options.outWidth <= 0 || options.outHeight <= 0) return null
                        var sample = 1
                        val maxSize = if (avatar) 192 else 1600
                        while (maxOf(options.outWidth, options.outHeight) / sample > maxSize) sample *= 2
                        options.inSampleSize = sample
                        options.inJustDecodeBounds = false
                        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    }
                    else -> return null
                }
            } finally { request.disconnect() }
        }
        return null
    }
}

@Composable
internal fun QZoneAvatar(url: String?, loader: QZonePhotoLoader, modifier: Modifier) {
    var photo by remember(url, loader) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url, loader) {
        photo = url?.let { loader.load(it, avatar = true) }
    }
    Surface(modifier, shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val bitmap = photo
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Filled.Person, null, Modifier.fillMaxSize(0.55f), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun QZonePhoto(url: String, loader: QZonePhotoLoader, modifier: Modifier, fit: Boolean = false) {
    var photo by remember(url) { mutableStateOf<Bitmap?>(null) }
    var loaded by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url, loader) {
        photo = loader.load(url)
        loaded = true
    }
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val bitmap = photo
            when {
                bitmap != null -> Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.qzone_photo),
                    contentScale = if (fit) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                !loaded -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else -> Text(stringResource(R.string.qzone_photo_failed), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
internal fun QZonePhotos(urls: List<String>, loader: QZonePhotoLoader, onOpenWeb: () -> Unit, small: Boolean = false) {
    if (urls.isEmpty()) return
    var selected by remember(urls) { mutableStateOf<Int?>(null) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(urls) { index, url ->
            QZonePhoto(
                url, loader, Modifier.size(if (small) 72.dp else 112.dp)
                    .clip(RoundedCornerShape(12.dp)).clickable { selected = index },
            )
        }
    }
    selected?.let { initial ->
        ExpressiveAlertDialog(show = true, onDismissRequest = { selected = null }) {
            val pager = rememberPagerState(initialPage = initial) { urls.size }
            HorizontalPager(pager, modifier = Modifier.fillMaxWidth().height(300.dp)) { index ->
                QZonePhoto(urls[index], loader, Modifier.fillMaxSize(), fit = true)
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { selected = null; onOpenWeb() }) { Text(stringResource(R.string.qzone_web_view)) }
                TextButton(onClick = { selected = null }) { Text(stringResource(R.string.qzone_close)) }
            }
        }
    }
}

@Composable
internal fun QZoneVideo(url: String?, onOpenWeb: () -> Unit) {
    var playing by remember(url) { mutableStateOf(false) }
    var failed by remember(url) { mutableStateOf(false) }
    if (url != null && playing && !failed) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            factory = { context ->
                VideoView(context).apply {
                    val controller = MediaController(context)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                    setOnErrorListener { _, _, _ -> failed = true; true }
                    setOnPreparedListener { start() }
                    setVideoURI(Uri.parse(url), mapOf(
                        "Referer" to "https://user.qzone.qq.com/",
                        "android-allow-cross-domain-redirect" to "0",
                    ))
                }
            },
            onRelease = { it.stopPlayback() },
        )
        TextButton(onClick = { playing = false }) { Text(stringResource(R.string.qzone_close)) }
    } else {
        TextButton(onClick = { if (url != null && !failed) playing = true else onOpenWeb() }) {
            Text(stringResource(if (url != null && !failed) R.string.qzone_play_video else R.string.qzone_video_in_web))
        }
    }
}
