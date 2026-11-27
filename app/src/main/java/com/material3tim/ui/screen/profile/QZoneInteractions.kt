package com.material3tim.ui.screen.profile

import android.webkit.WebView
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.coroutines.resume

internal data class QZonePerson(val uin: String?, val name: String, val visitSeconds: Long = 0L)
internal data class QZonePeople(val people: List<QZonePerson>, val next: String? = null)

private fun decodeObject(raw: String?): JSONObject? = runCatching {
    JSONObject(JSONTokener(raw ?: return null).nextValue() as? String ?: return null)
}.getOrNull()

private fun decodePeople(data: JSONObject): QZonePeople? {
    val entries = data.optJSONArray("people") ?: return null
    return QZonePeople(buildList {
        for (i in 0 until minOf(100, entries.length())) {
            val entry = entries.optJSONObject(i) ?: continue
            val uin = qZoneUin(entry.optString("uin"))
            val name = entry.optString("name").trim().take(100)
            if (name.isNotBlank() || uin != null) add(QZonePerson(uin, name, entry.optLong("time")))
        }
    }, qZoneUin(data.optString("next")))
}

/** 定位用户点中的卡片。站点登录态和操作参数始终留在 QQ 空间文档里。 */
private fun onCard(anchor: String, action: String): String = """
    (() => {
      if (location.origin !== 'https://user.qzone.qq.com') return null;
      let card = document.getElementById(${JSONObject.quote(anchor)});
      if (!card) {
        try { card = document.querySelector('#QM_Feeds_Iframe')?.contentDocument?.getElementById(${JSONObject.quote(anchor)}); } catch (_) {}
      }
      if (!card?.matches('.f-single')) return null;
      const doc = card.ownerDocument;
      const like = card.querySelector('.qz_like_btn_v3[data-islike]');
      $action
    })();
""".trimIndent()

internal class QZoneInteractions(private val web: WebView) {
    suspend fun evaluate(script: String): String? = withTimeoutOrNull(1800L) {
        suspendCancellableCoroutine { cont ->
            runCatching { web.evaluateJavascript(script) { value -> if (cont.isActive) cont.resume(value) } }
                .onFailure { if (cont.isActive) cont.resume(null) }
        }
    }

    suspend fun readProfileLink(): String? {
        val result = evaluate("""
            (() => location.origin === 'https://user.qzone.qq.com' && document.querySelector('#feed_friend_list')
                ? document.querySelector('#aMainPage[href]')?.href || null : null)();
        """.trimIndent())
        return runCatching { JSONTokener(result ?: return null).nextValue() as? String }
            .getOrNull()?.let(::qZoneProfileTarget)
    }

    suspend fun toggleLike(post: QZoneFeedCard): Boolean {
        val was = post.liked ?: return false
        val previous = if (was) "1" else "0"
        val dispatched = evaluate(onCard(post.anchor, """
            if (!like || like.getAttribute('data-islike') !== '$previous') return false;
            like.click(); return true;
        """.trimIndent())) == "true"
        if (!dispatched) return false
        return confirm { evaluate(onCard(post.anchor, "return like?.getAttribute('data-islike') === '${if (was) "0" else "1"}';")) == "true" }
    }

    suspend fun comment(post: QZoneFeedCard, message: String): Boolean {
        val text = message.trim().takeIf { it.isNotEmpty() && it.length <= 500 } ?: return false
        val quoted = JSONObject.quote(text)
        val token = JSONObject.quote(java.util.UUID.randomUUID().toString())
        val dispatched = evaluate(onCard(post.anchor, """
            const owner = doc.defaultView;
            const entry = card.querySelector('.comment_default_inputentry');
            const feed = owner?.QZONE?.Feeds?.queryFeedByElem?.(card);
            if (!entry || !feed || !owner.seajs || !window.QZONE?.FP?._t?.checkLogin?.()) return false;
            window.__m3timComment = {id: $token, status: 'pending'};
            const done = status => {
              if (window.__m3timComment?.id === $token) window.__m3timComment.status = status;
            };
            owner.seajs.use('v8/ic/comment', module => {
              try {
                const reply = module?.ReplyV2;
                if (window.__m3timComment?.id !== $token || !reply || reply.sending) { done('failed'); return; }
                module.onClick.call(entry, feed, 'qz_reply');
                if (reply.coreParam?.feed !== feed || reply.coreParam?.isReply || typeof reply.submit !== 'function') {
                  done('failed'); return;
                }
                // 走官网自己的编辑器提交方法，token、权限校验和验证码均由官网处理。
                reply.submit({content: $quoted}, result => {
                  done(Number(result?.ret) === 0 && Number(result?.err || 0) === 0 ? 'ok' : 'failed');
                });
              } catch (_) { done('failed'); }
            });
            return true;
        """.trimIndent())) == "true"
        if (!dispatched) return false
        // 等待官网回调，不把临时插入页面的「发送中」文案当成功，也不自动重发。
        repeat(24) {
            when (evaluate("window.__m3timComment?.id === $token ? window.__m3timComment.status : null")) {
                "\"ok\"" -> return true
                "\"failed\"" -> return false
            }
            delay(500L)
        }
        return false
    }

    suspend fun likers(post: QZoneFeedCard, cursor: String?): QZonePeople? {
        val token = JSONObject.quote(java.util.UUID.randomUUID().toString())
        val after = qZoneUin(cursor)
        val started = evaluate(onCard(post.anchor, """
            const key = like?.getAttribute('data-unikey');
            const me = window.QZONE?.FP?._t?.checkLogin?.();
            if (!key || !me || !window.QZFL?.JSONGetter) return false;
            const request = new QZFL.JSONGetter(location.origin + '/proxy/domain/users.qzone.qq.com/cgi-bin/likes/get_like_list_app', '', {
              uin: me, unikey: key.replace(/^https/, 'http').replace(/\.1&g_iframedescend=1$/, ''),
              begin_uin: '${after ?: "0"}', query_count: 60, if_first_page: ${if (after == null) 1 else 0}
            }, 'utf-8');
            window.__m3timLikes = {id: $token, value: null};
            const done = value => { if (window.__m3timLikes?.id === $token) window.__m3timLikes.value = value; };
            request.onSuccess = result => {
              const list = result?.data?.like_uin_info;
              if (Number(result?.code) !== 0 || !Array.isArray(list)) { done({failed:true}); return; }
              done({people:list.slice(0,60).map(x=>({uin:String(x.fuin||''),name:String(x.nick||'').slice(0,100)})),
                    next:list.length >= 60 ? String(list[59].fuin||'') : ''});
            };
            request.onError = () => done({failed:true});
            request.send('_Callback'); return true;
        """.trimIndent())) == "true"
        if (!started) return null
        return waitForPeople("""
            (() => {
              const value = window.__m3timLikes;
              if (value?.id !== $token || !value.value) return null;
              delete window.__m3timLikes;
              return JSON.stringify(value.value);
            })();
        """.trimIndent())
    }

    suspend fun visitors(expectedUin: String?): QZonePeople? {
        val expected = JSONObject.quote(qZoneUin(expectedUin).orEmpty())
        // 桌面官网的旧加载器引用 HTTP 脚本；只补载这两个已核对的腾讯 HTTPS 资源。
        val started = evaluate("""
            (() => {
              if (location.origin !== 'https://user.qzone.qq.com' || !document.querySelector('#QM_3_Body')) return false;
              const me = String(window.QZONE?.FP?._t?.checkLogin?.() || '');
              if (!me || ($expected && me !== $expected)) return false;
              if (window.__m3timVisitorLoading) return true;
              window.__m3timVisitorLoading = true;
              const load = src => new Promise((resolve, reject) => {
                const script = document.createElement('script'); script.src = src;
                script.onload = resolve; script.onerror = reject; document.head.append(script);
              });
              (async () => {
                try {
                  if (!window.QZONE?.module?.Visitor) await load('https://qzonestyle.gtimg.cn/qzone/v6/friend_manage/module/visitor.js');
                  if (!window.QM?.Visitor) await load('https://qzonestyle.gtimg.cn/qzone/v8/ic/qm/qm_3_v8.js');
                  if (!document.querySelector('#visitMeContainer')) window.QM?.Visitor?.bootstrap();
                } finally { window.__m3timVisitorLoading = false; }
              })().catch(() => {});
              return true;
            })();
        """.trimIndent()) == "true"
        if (!started) return null
        return waitForPeople("""
            (() => {
              const data = window.QM?.Visitor?._insCache?.visitMe?.getData?.();
              if (Array.isArray(data?.items)) return JSON.stringify({people:data.items.slice(0,100).map(x => ({
                uin: String(x.uin || ''), name: String(x.name || '').slice(0,100), time: Number(x.time) || 0
              }))});
              const host = document.querySelector('#visitMeContainer');
              if ((host?.innerText || '').includes('最近没有好友访问')) return JSON.stringify({people:[]});
              return null;
            })();
        """.trimIndent())
    }

    private suspend fun waitForPeople(script: String): QZonePeople? {
        repeat(18) {
            val data = decodeObject(evaluate(script))
            if (data != null) return decodePeople(data)
            delay(500L)
        }
        return null
    }

    private suspend fun confirm(predicate: suspend () -> Boolean): Boolean {
        repeat(12) {
            if (predicate()) return true
            delay(350L)
        }
        return false
    }
}
