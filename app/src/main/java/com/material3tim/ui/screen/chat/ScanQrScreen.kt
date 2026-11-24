package com.material3tim.ui.screen.chat

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.material3tim.R
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.util.ExternalLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 扫一扫页面的状态。 */
internal sealed interface ScanQrState {
    /**
     * 默认操作页：三个按钮可用；可带上一次失败的原因（未识别/相机不可用/无法打开）
     * 与已识别但打不开的原始文本（供复制）。
     */
    data class Idle(
        val errorKind: ScanQrError? = null,
        val errorContent: String = "",
    ) : ScanQrState

    /** 正在 IO 解码二维码（loading，按钮禁用）。 */
    data object Working : ScanQrState
}

/** 扫一扫失败原因。 */
internal enum class ScanQrError {
    /** 设备没有可处理 ACTION_IMAGE_CAPTURE 的相机应用，或无法创建输出图片。 */
    CameraUnavailable,

    /** 照片/相册图片里没有识别到二维码。 */
    NoQrCode,

    /** 识别成功但内容无法分发（无可用方式打开）。 */
    OpenFailed,
}

/**
 * 扫一扫页面入口（消息页加号菜单「扫一扫」→ `Route.ScanQr`）。
 *
 * 默认显示三按钮操作页（不再自动拉相机）：
 * - 「打开相机」：系统相机拍照（MediaStore 输出）→ ZXing 解码；
 * - 「选择图片」：系统媒体选择器单选图片 → 直接解码；
 * - 「取消」：关闭页面。
 * 解码成功后按内容分发（http(s) → [ExternalLink]；其它优先 [AppDataSources.qrActionSource]，
 * 再退化 ACTION_VIEW）；成功 Toast 并关闭，失败在按钮上方显示原因（可再点按钮重试）。
 */
@Composable
fun ScanQrScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ScanQrState>(ScanQrState.Idle()) }
    // 相机照片写入的 MediaStore Uri：结果回调/旋转重建后还要用它解码，故 rememberSaveable。
    var pendingUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    fun deleteTempImage(uri: Uri?) {
        if (uri != null) {
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
    }

    // 解码 + 分发（相机与相册两条路径共用）。
    fun handlePickedUri(uri: Uri, deleteAfterDecode: Boolean) {
        state = ScanQrState.Working
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                decodeQrFromUri(context, uri, deleteAfterDecode)
            }
            if (text.isNullOrBlank()) {
                state = ScanQrState.Idle(errorKind = ScanQrError.NoQrCode)
                return@launch
            }
            val opened = runCatching { dispatchQrContent(context, text) }.getOrDefault(false)
            if (opened) {
                Toast.makeText(context, R.string.scan_opened, Toast.LENGTH_SHORT).show()
                onClose()
            } else {
                // 没有可打开的方式：回到操作页显示原因 + 可复制文本。
                state = ScanQrState.Idle(
                    errorKind = ScanQrError.OpenFailed,
                    errorContent = text,
                )
            }
        }
    }

    // 相册单选：取消（uri == null）直接留在默认操作页。
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            // 用户相册里的原图不能删除，只解码。
            handlePickedUri(uri, deleteAfterDecode = false)
        }
    }

    // 系统相机：取消直接留在默认操作页。
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = pendingUri
        pendingUri = null
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            // 相机写入的临时照片解码后删除。
            handlePickedUri(uri, deleteAfterDecode = true)
        } else {
            deleteTempImage(uri)
        }
    }

    fun launchCamera() {
        val uri = createScanOutputUri(context)
        if (uri == null) {
            state = ScanQrState.Idle(errorKind = ScanQrError.CameraUnavailable)
            return
        }
        pendingUri = uri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            deleteTempImage(uri)
            pendingUri = null
            state = ScanQrState.Idle(errorKind = ScanQrError.CameraUnavailable)
            return
        }
        val launched = runCatching { cameraLauncher.launch(intent) }.isSuccess
        if (!launched) {
            deleteTempImage(uri)
            pendingUri = null
            state = ScanQrState.Idle(errorKind = ScanQrError.CameraUnavailable)
        }
    }

    val working = state is ScanQrState.Working
    val onOpenCamera: () -> Unit = {
        if (!working) {
            state = ScanQrState.Idle()
            launchCamera()
        }
    }
    val onPickImage: () -> Unit = {
        if (!working) {
            state = ScanQrState.Idle()
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
    val onCopy: (String) -> Unit = { text ->
        runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("scan_result", text))
            Toast.makeText(context, R.string.scan_copied, Toast.LENGTH_SHORT).show()
        }
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> ScanQrMiuix(
            state = state,
            onOpenCamera = onOpenCamera,
            onPickImage = onPickImage,
            onCancel = onClose,
            onCopy = onCopy,
        )

        UiMode.Material -> ScanQrMaterial(
            state = state,
            onOpenCamera = onOpenCamera,
            onPickImage = onPickImage,
            onCancel = onClose,
            onCopy = onCopy,
        )
    }
}

/** 失败提示文案（两套 UI 共用）。 */
@Composable
internal fun scanQrErrorTitle(kind: ScanQrError): String = when (kind) {
    ScanQrError.CameraUnavailable -> stringResource(R.string.scan_error_camera)
    ScanQrError.NoQrCode -> stringResource(R.string.scan_error_no_qr)
    ScanQrError.OpenFailed -> stringResource(R.string.scan_error_open_failed)
}

/** 创建系统相机拍照输出用的 MediaStore Uri（无需 FileProvider / 存储权限，API 29+）。 */
private fun createScanOutputUri(context: Context): Uri? = runCatching {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "material3tim_scan_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    }
    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
}.getOrNull()

/**
 * 从 Uri 读取图片并解码二维码。
 * [deleteAfterDecode] 为 true 时（相机临时照片）无论成功失败都删除；相册原图（false）不删除。
 * 先读尺寸再按 2048 长边降采样，避免高像素照片解码时内存翻倍/被系统杀（二维码识别率不受影响）。
 */
private fun decodeQrFromUri(context: Context, uri: Uri, deleteAfterDecode: Boolean): String? {
    val bitmap = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 2048)
        }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
    return try {
        bitmap?.let { decodeQrBitmap(it) }
    } finally {
        if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
        if (deleteAfterDecode) {
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
    }
}

/** 计算 BitmapFactory 降采样比例：长边不超过 [maxDimension]。 */
private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sampleSize = 1
    while (maxOf(width, height) / sampleSize > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

/** ZXing 解码：QR_CODE + TRY_HARDER；失败依次尝试旋转 90/180/270 度。 */
private fun decodeQrBitmap(source: Bitmap): String? {
    val hints: Map<DecodeHintType, Any> = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    )
    val reader = MultiFormatReader()
    reader.setHints(hints)
    val rotatedBitmaps = ArrayList<Bitmap>(3)
    try {
        var current = source
        repeat(4) {
            val text = runCatching {
                reader.decodeWithState(toBinaryBitmap(current)).text
            }.getOrNull()
            if (!text.isNullOrBlank()) return text.trim()
            reader.reset()
            if (rotatedBitmaps.size < 3) {
                val next = Bitmap.createBitmap(
                    current,
                    0,
                    0,
                    current.width,
                    current.height,
                    Matrix().apply { postRotate(90f) },
                    true,
                )
                // 中间旋转图用完即回收，避免整张图叠加占内存。
                if (current !== source && !current.isRecycled) current.recycle()
                rotatedBitmaps.add(next)
                current = next
            }
        }
        return null
    } finally {
        rotatedBitmaps.forEach { if (!it.isRecycled) it.recycle() }
    }
}

private fun toBinaryBitmap(bitmap: Bitmap): BinaryBitmap {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    return BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
}

/**
 * 二维码内容分发（按顺序）：
 * 1. http(s) 链接 → [ExternalLink.open]（与聊天气泡一致）；
 * 2. 其它内容 → [AppDataSources.qrActionSource] 交给 TIM 原生分发；
 * 3. 仍失败 → 内容带 scheme 时尝试系统 ACTION_VIEW。
 *
 * @return 是否已成功发起打开动作。
 */
private fun dispatchQrContent(context: Context, rawText: String): Boolean {
    val text = rawText.trim()
    if (text.isEmpty()) return false
    val lower = text.lowercase()
    if (lower.startsWith("http://") || lower.startsWith("https://")) {
        ExternalLink.open(context, text)
        return true
    }
    val source = AppDataSources.qrActionSource
    if (source != null && runCatching { source.openQrContent(text) }.getOrDefault(false)) {
        return true
    }
    val uri = runCatching { Uri.parse(text) }.getOrNull() ?: return false
    if (uri.scheme.isNullOrBlank()) return false
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (intent.resolveActivity(context.packageManager) == null) return false
    return runCatching { context.startActivity(intent) }.isSuccess
}
