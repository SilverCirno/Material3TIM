package com.material3tim.ui.component

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 本地图片加载（TIM 进程里 Kernel 已经缓存好的头像 / 聊天图片）。
 *
 * 没有引入图片库：直接用 `BitmapFactory` 在 IO 线程解码，并做二级采样 + LRU 缓存。
 * 独立 App（示例数据）里没有本地路径，始终返回 null，调用方回落到占位/渐变头像。
 */
object LocalImageCache {

    /**
     * 按**字节预算**而不是条数做 LRU：低端机内存紧张时，64 张 1280px 的 ARGB 图可达数百 MB，
     * 会频繁触发 GC/大图上传，表现为列表滚动卡顿。这里限制总占用 ~32MB。
     */
    private const val MAX_BYTES = 32L * 1024 * 1024

    private fun key(path: String, maxDimension: Int): String = "$path@$maxDimension"

    private fun bytesOf(bitmap: ImageBitmap): Long =
        bitmap.width.toLong() * bitmap.height.toLong() * 4L

    private val cache = LinkedHashMap<String, ImageBitmap>(16, 0.75f, true)
    private var usedBytes = 0L

    fun get(path: String, maxDimension: Int): ImageBitmap? = synchronized(cache) {
        cache[key(path, maxDimension)]
    }

    fun put(path: String, maxDimension: Int, bitmap: ImageBitmap) = synchronized(cache) {
        val k = key(path, maxDimension)
        cache.put(k, bitmap)?.let { usedBytes -= bytesOf(it) }
        usedBytes += bytesOf(bitmap)
        val iterator = cache.entries.iterator()
        while (usedBytes > MAX_BYTES && iterator.hasNext()) {
            val entry = iterator.next()
            iterator.remove()
            usedBytes -= bytesOf(entry.value)
        }
    }

    fun clear() = synchronized(cache) {
        cache.clear()
        usedBytes = 0L
    }
}

/** 读取本地图片；[maxDimension] 限制解码后的最大边长，避免大图占内存。 */
@Composable
fun rememberLocalImage(path: String?, maxDimension: Int = 1280): ImageBitmap? {
    if (path.isNullOrBlank()) return null
    // 缓存 key 带上解码尺寸：详情页按原图尺寸解码，不能被气泡的缩略解码结果顶掉。
    var image by remember(path, maxDimension) {
        mutableStateOf(LocalImageCache.get(path, maxDimension))
    }
    LaunchedEffect(path, maxDimension) {
        if (image == null) {
            val decoded = withContext(Dispatchers.IO) { decodeFile(path, maxDimension) }
            if (decoded != null) {
                LocalImageCache.put(path, maxDimension, decoded)
                image = decoded
            }
        }
    }
    return image
}

private fun decodeFile(path: String, maxDimension: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    if (maxDimension > 0) {
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) {
            sample *= 2
        }
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
    }
    val bitmap = BitmapFactory.decodeFile(path, options)
    if (bitmap == null) {
        android.util.Log.w("Material3Tim", "decode local image failed: $path")
    }
    bitmap?.asImageBitmap()
}.getOrNull()

/**
 * 只读已解码缓存、不触发解码（滚动中延迟新图加载用）。
 * 未命中返回 null，由调用方显示占位；滚动停止后调用方切回 [rememberLocalImage] 触发解码。
 */
@Composable
fun rememberCachedLocalImage(path: String?, maxDimension: Int = 1280): ImageBitmap? =
    remember(path, maxDimension) {
        if (path.isNullOrBlank()) null else LocalImageCache.get(path, maxDimension)
    }
