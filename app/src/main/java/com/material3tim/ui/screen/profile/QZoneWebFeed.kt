package com.material3tim.ui.screen.profile

import org.json.JSONObject
import org.json.JSONTokener
import java.net.URI
import java.util.Locale

internal data class QZoneComment(
    val author: String,
    val authorUin: String?,
    val avatar: String?,
    val time: String,
    val text: String,
    val photos: List<String>,
)
internal data class QZoneFeedCard(
    val anchor: String,
    val author: String,
    val authorUin: String?,
    val avatar: String?,
    val time: String,
    val text: String,
    val photos: List<String>,
    val video: String?,
    val hasVideo: Boolean,
    val comments: List<QZoneComment>,
    val liked: Boolean?,
    val likeCount: Int,
    val canComment: Boolean,
)

internal data class QZoneWebFeed(val profile: String, val cards: List<QZoneFeedCard>)

internal fun qZoneUin(raw: String?): String? = raw?.trim()
    ?.takeIf { it.length <= 20 && it.all { ch -> ch in '0'..'9' } }
    ?.toLongOrNull()?.takeIf { it > 0L }?.toString()

internal fun qZoneWebEntry(uin: String, ownProfile: Boolean = false): String = qZoneUin(uin)
    ?.let { "https://user.qzone.qq.com/$it" + if (ownProfile) "/main" else "" } ?: "https://i.qq.com/"

internal fun qZoneWebHost(url: String?): String? = runCatching {
    val uri = URI(url ?: return null)
    if (!uri.scheme.equals("https", ignoreCase = true) || uri.userInfo != null || uri.port !in listOf(-1, 443)) return null
    uri.host?.lowercase(Locale.ROOT)
}.getOrNull()

internal fun qZoneAllowsNavigation(url: String?): Boolean = qZoneWebHost(url)?.let { host ->
    host == "i.qq.com" || host == "qzone.qq.com" || host.endsWith(".qzone.qq.com") ||
        host == "ptlogin2.qq.com" || host.endsWith(".ptlogin2.qq.com")
} ?: false

private val ProfilePath = Regex("^/(\\d+)(?:/main)?/?$")

internal fun qZoneProfileTarget(url: String?): String? = runCatching {
    val uri = URI(url ?: return null)
    if (!uri.host.equals("user.qzone.qq.com", true) || uri.userInfo != null) return null
    val allowedPort = when (uri.scheme?.lowercase(Locale.ROOT)) {
        "http" -> 80
        "https" -> 443
        else -> return null
    }
    if (uri.port != -1 && uri.port != allowedPort) return null
    qZoneUin(ProfilePath.matchEntire(uri.path ?: "")?.groupValues?.get(1))
}.getOrNull()

internal fun qZoneUpgradeProfileUrl(url: String): String? =
    if (url.startsWith("http://", ignoreCase = true)) {
        val main = runCatching { URI(url).path.trimEnd('/').endsWith("/main") }.getOrDefault(false)
        qZoneProfileTarget(url)?.let { qZoneWebEntry(it, ownProfile = main) }
    } else null

internal fun qZoneAvatarUrl(raw: String?): String? {
    val text = raw?.trim()?.takeIf { it.length <= 4096 } ?: return null
    // 老页面的头像链接仍可能是 HTTP，只升级已确认的腾讯头像主机。
    val https = if (text.startsWith("http://", ignoreCase = true)) "https://${text.substring(7)}" else text
    val host = qZoneWebHost(https) ?: return null
    return https.takeIf { host.matches(Regex("qlogo[0-9]*\\.store\\.qq\\.com")) || host.endsWith(".qlogo.cn") || host == "qlogo.cn" }
}

internal fun qZoneMediaUrl(raw: String?, video: Boolean): String? {
    val host = qZoneWebHost(raw) ?: return null
    val allowed = if (video) {
        host == "photovideo.photo.qq.com" || host.endsWith(".photovideo.photo.qq.com")
    } else {
        host == "qpic.cn" || host.endsWith(".qpic.cn") || host == "photo.qq.com" || host.endsWith(".photo.qq.com")
    }
    return raw?.takeIf { allowed && it.length <= 4096 }
}

private fun JSONObject.photos(limit: Int): List<String> = buildList {
    val images = optJSONArray("photos") ?: return@buildList
    for (i in 0 until minOf(images.length(), limit)) qZoneMediaUrl(images.optString(i), false)?.let(::add)
}.distinct()

internal fun parseQZoneWebFeed(result: String?): QZoneWebFeed? = runCatching {
    val encoded = JSONTokener(result ?: return null).nextValue() as? String ?: return null
    val data = JSONObject(encoded)
    val rows = data.optJSONArray("cards") ?: return null
    val cards = buildList {
        for (index in 0 until minOf(rows.length(), 30)) {
            val row = rows.optJSONObject(index) ?: continue
            val text = row.optString("text").trim().take(4000)
            val images = row.photos(9)
            val video = qZoneMediaUrl(row.optString("video"), true)
            val hasVideo = video != null || row.optBoolean("hasVideo")
            if (text.isBlank() && images.isEmpty() && !hasVideo) continue
            val comments = buildList {
                val entries = row.optJSONArray("comments") ?: return@buildList
                for (i in 0 until minOf(entries.length(), 8)) {
                    val c = entries.optJSONObject(i) ?: continue
                    val message = c.optString("text").trim().take(600)
                    val photos = c.photos(2)
                    if (message.isNotEmpty() || photos.isNotEmpty()) {
                        add(QZoneComment(
                            author = c.optString("author").trim().take(100),
                            authorUin = qZoneProfileTarget(c.optString("authorUrl")),
                            avatar = qZoneAvatarUrl(c.optString("avatar")),
                            time = c.optString("time").take(120),
                            text = message,
                            photos = photos,
                        ))
                    }
                }
            }
            add(QZoneFeedCard(
                anchor = row.optString("anchor").takeIf { it.length <= 120 && it.matches(Regex("[a-zA-Z0-9_-]+")) }.orEmpty(),
                author = row.optString("author").trim().take(100),
                authorUin = qZoneProfileTarget(row.optString("authorUrl")),
                avatar = qZoneAvatarUrl(row.optString("avatar")),
                time = row.optString("time").trim().take(120),
                text = text,
                photos = images,
                video = video,
                hasVideo = hasVideo,
                comments = comments,
                liked = if (row.isNull("liked")) null else row.optBoolean("liked"),
                likeCount = row.optInt("likeCount").coerceAtLeast(0),
                canComment = row.optBoolean("canComment"),
            ))
        }
    }
    QZoneWebFeed(data.optString("profile").trim().take(100), cards)
}.getOrNull()

/** 只读已经渲染的卡片，不读取 Cookie、令牌、存储或登录表单，不触发网页操作。 */
internal fun readQZoneWebFeed(uin: String): String {
    val target = qZoneUin(uin).orEmpty()
    return """
    (() => {
      if (location.protocol !== 'https:' || location.hostname !== 'user.qzone.qq.com') return null;
      const target = '$target';
      if (target && location.pathname.split('/')[1] !== target) return null;
      const frame = document.querySelector('#QM_Feeds_Iframe');
      let personal = null;
      try { personal = frame?.contentDocument?.querySelector('.feed-home'); } catch (_) {}
      const feed = target ? (personal || document.querySelector('.feed-home')) : document.querySelector('#feed_friend_list');
      if (!feed) return null;
      const textOf = (node, selector, max) => {
        const e = node.querySelector(selector);
        return e ? (e.innerText || e.textContent || '').trim().slice(0, max) : '';
      };
      const media = (node, selector, limit) => [...node.querySelectorAll(selector)].slice(0,limit).map(e => {
        let value = e.currentSrc || e.getAttribute('src') || e.getAttribute('data-src') || '';
        try { return value ? new URL(value, e.baseURI).href.slice(0,4096) : ''; } catch (_) { return ''; }
      }).filter(Boolean);
      const comment = row => {
        const content = row.querySelector('.comments-item-bd .comments-content, .comments_item_bd .comments_content');
        if (!content || content.closest('.comments-item, .comments_item') !== row) return null;
        const copy = content.cloneNode(true);
        const authorSelector = '.nickname, a.q_des';
        copy.querySelector(authorSelector)?.remove();
        copy.querySelectorAll('.comments-op, .comments_op, .comments-thumbnails, .mod-comments-sub, .comments-list').forEach(e => e.remove());
        return {
          author: textOf(content, authorSelector, 100),
          authorUrl: content.querySelector(authorSelector)?.href || '',
          avatar: media(row, '.ui-avatar img, .ui_avatar img', 1)[0] || '',
          time: textOf(content, '.comments-op .state, .comments_op .state, .comments-op .c_tx3, .comments_op .c_tx3', 120),
          text: (copy.textContent || '').trim().replace(/^[:：]\s*/, '').slice(0,600),
          photos: media(content, '.comments-thumbnails img', 2)
        };
      };
      const cards = [...feed.querySelectorAll('.f-single')].slice(0,30).map(card => ({
        anchor: (card.id || '').slice(0,120),
        author: textOf(card, '.f-single-head .f-name', 100),
        authorUrl: card.querySelector('.f-single-head .f-name[href], .f-single-head .user-avatar[href]')?.href || '',
        avatar: media(card, '.f-single-head .user-avatar img, .f-single-head .user-pto img', 1)[0] || '',
        time: textOf(card, '.f-single-head .info-detail', 120),
        text: textOf(card, '.f-ct .txt-box', 4000) || textOf(card, '.qz_summary', 4000) || textOf(card, '.f-info', 4000),
        photos: media(card, '.f-ct img', 9),
        video: media(card, '.f-ct video, .f-ct video source', 2)[0] || '',
        hasVideo: !!card.querySelector('.f-ct video, .f-ct object, .f-ct [class*="video"], .f-ct [class*="player"]'),
        comments: [...card.querySelectorAll('.mod-comments .comments-item, .mod_comments .comments_item')].slice(0,8).map(comment).filter(Boolean),
        liked: ({'0': false, '1': true})[card.querySelector('.qz_like_btn_v3')?.getAttribute('data-islike')] ?? null,
        likeCount: Number(card.querySelector('.qz_like_btn_v3')?.getAttribute('data-likecnt')) || 0,
        canComment: !!card.querySelector('.qz_btn_reply, .comment_default_inputentry .textinput')
      }));
      return JSON.stringify({profile: textOf(document, '.layout-head .head-title', 100), cards});
    })();
    """.trimIndent()
}

internal fun focusQZoneCard(anchor: String): String = """
    (() => {
      if (location.hostname !== 'user.qzone.qq.com') return;
      let item = document.getElementById(${JSONObject.quote(anchor)});
      try { item ||= document.querySelector('#QM_Feeds_Iframe')?.contentDocument?.getElementById(${JSONObject.quote(anchor)}); } catch (_) {}
      item?.scrollIntoView({block:'center'});
    })();
""".trimIndent()
