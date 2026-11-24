package com.material3tim.ui.screen.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

/** http(s) 链接与裸域名（www. 开头）的粗略匹配，末尾标点会被修剪。 */
private val UrlRegex = Regex(
    """(https?://[^\s<>"“”‘’（）()【】\[\]{}，。！？；：、]+|(?<![\w.])www\.[^\s<>"“”‘’（）()【】\[\]{}，。！？；：、]+)""",
    RegexOption.IGNORE_CASE,
)

private val TrailingPunctuation = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '。', '，', '；', '：', '！', '？', '、')

/**
 * 把文本里的 URL 变成可点击链接；点击由 [com.material3tim.ui.util.ExternalLink] 处理
 * （非 QQ 链接走系统浏览器，QQ 链接走 TIM 自己的浏览器）。
 * 链接颜色需要与气泡内容色有对比度，否则自己的蓝色气泡上看不见。
 */
internal fun linkifyContent(text: String, linkColor: Color): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var lastIndex = 0
        UrlRegex.findAll(text).forEach { match ->
            val raw = match.value.trimEnd(*TrailingPunctuation)
            if (raw.isEmpty()) return@forEach
            val start = match.range.first
            if (start > lastIndex) append(text.substring(lastIndex, start))
            val url = if (raw.startsWith("http", ignoreCase = true)) raw else "https://$raw"
            withLink(
                LinkAnnotation.Url(
                    url = url,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ),
                ),
            ) {
                append(raw)
            }
            lastIndex = start + raw.length
        }
        if (lastIndex < text.length) append(text.substring(lastIndex))
    }
}

/** 让 Text 里的 LinkAnnotation 点击走 [com.material3tim.ui.util.ExternalLink]。 */
internal fun openLinkHandler(context: android.content.Context): androidx.compose.ui.platform.UriHandler =
    object : androidx.compose.ui.platform.UriHandler {
        override fun openUri(uri: String) {
            com.material3tim.ui.util.ExternalLink.open(context, uri)
        }
    }
