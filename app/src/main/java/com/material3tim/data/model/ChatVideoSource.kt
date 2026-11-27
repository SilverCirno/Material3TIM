package com.material3tim.data.model

import java.net.URI

/** 只把可直接交给 Android 播放器的 HTTPS 完整 URL 当作串流地址。 */
internal fun chatVideoStreamUrl(vararg candidates: String?): String? = candidates.firstNotNullOfOrNull { candidate ->
    val value = candidate?.trim()?.takeIf { it.isNotEmpty() } ?: return@firstNotNullOfOrNull null
    val uri = runCatching { URI(value) }.getOrNull() ?: return@firstNotNullOfOrNull null
    value.takeIf { uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && uri.userInfo == null }
}
