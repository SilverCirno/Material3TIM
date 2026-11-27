package com.material3tim.ui.screen.profile

import android.graphics.Color as AndroidColor
import android.view.View
import android.widget.Toast
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.material3tim.R
import com.material3tim.ui.UiMode
import com.material3tim.ui.component.material.ExpressiveAlertDialog
import com.material3tim.ui.component.material.ExpressiveInputDialog
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.ui.theme.M3TimTheme
import com.material3tim.ui.util.openQZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class QZonePage { Loading, Login, Preview, WebOnly, Error }

@Composable
internal fun QZoneScreen(route: Route.QZone) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val uin = route.uin
    var target by remember(route) { mutableStateOf(qZoneUin(uin).takeUnless { route.visitors }) }
    var useExplicitContactPage by remember(route) { mutableStateOf(false) }
    val initialUrl = if (route.visitors) qZoneWebEntry("") else {
        qZoneWebEntry(target.orEmpty(), route.ownProfile || useExplicitContactPage)
    }
    fun tryExplicitContactPage(view: WebView): Boolean {
        val number = target ?: return false
        if (route.ownProfile || route.visitors || useExplicitContactPage) return false
        useExplicitContactPage = true
        view.loadUrl(qZoneWebEntry(number, ownProfile = true))
        return true
    }
    val photos = remember(route) { QZonePhotoLoader() }
    var returnToTargetAfterLogin by remember(route) { mutableStateOf(false) }
    var retriedTarget by remember(route) { mutableStateOf(false) }
    var focusAnchor by remember(route) { mutableStateOf("") }
    val previewScript = remember(target) { readQZoneWebFeed(target.orEmpty()) }
    var showOfficialPage by rememberSaveable(route) { mutableStateOf(false) }
    var page by remember(route) { mutableStateOf(QZonePage.Loading) }
    var feed by remember(route) { mutableStateOf<QZoneWebFeed?>(null) }
    var visitorData by remember(route) { mutableStateOf<QZonePeople?>(null) }
    var generation by remember(route) { mutableIntStateOf(0) }
    var busy by remember(route) { mutableStateOf(false) }
    var commentPost by remember(route) { mutableStateOf<QZoneFeedCard?>(null) }
    var draft by remember(route) { mutableStateOf<Pair<String, String>?>(null) }
    var likerPost by remember(route) { mutableStateOf<QZoneFeedCard?>(null) }
    var likerData by remember(route) { mutableStateOf<QZonePeople?>(null) }
    var likerFailed by remember(route) { mutableStateOf(false) }
    val web = remember(context, route) {
        runCatching {
            WebView(context).apply {
                setBackgroundColor(AndroidColor.WHITE)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    setSupportMultipleWindows(false)
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    // 已核对的响应是桌面版个人空间及好友动态，避免跳到未适配的移动版 DOM。
                    val chrome = Regex("Chrome/([0-9.]+)").find(userAgentString)?.value
                    if (chrome != null) {
                        userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) $chrome Safari/537.36"
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    // 只显示网页，不把第三方脚本输出写入应用日志。
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean = true
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (!request.isForMainFrame) return false
                        val url = request.url.toString()
                        // 官网自己的作者链接仍可能是 HTTP。只对已确认的个人主页地址升成 HTTPS。
                        qZoneUpgradeProfileUrl(url)?.let { view.loadUrl(it); return true }
                        return !qZoneAllowsNavigation(url)
                    }

                    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                        feed = null
                        visitorData = null
                        val host = qZoneWebHost(url)
                        page = if (host == "i.qq.com" || host == "ptlogin2.qq.com" || host?.endsWith(".ptlogin2.qq.com") == true) {
                            if (target != null && !retriedTarget && !showOfficialPage) returnToTargetAfterLogin = true
                            QZonePage.Login
                        } else {
                            QZonePage.Loading
                        }
                        generation++
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        if (page == QZonePage.Error || qZoneWebHost(url) != "user.qzone.qq.com") return
                        val opened = qZoneProfileTarget(url)
                        if (returnToTargetAfterLogin && !showOfficialPage && opened != null) {
                            returnToTargetAfterLogin = false
                            val profilePath = url?.substringBefore('?')?.trimEnd('/')?.endsWith("/main") == true
                            if (opened != target || (route.ownProfile && !profilePath)) {
                                retriedTarget = true
                                view.loadUrl(qZoneWebEntry(target.orEmpty(), route.ownProfile || useExplicitContactPage))
                                return
                            }
                        }
                        generation++
                    }

                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        if (request.isForMainFrame &&
                            (showOfficialPage || qZoneProfileTarget(request.url.toString()) != target || !tryExplicitContactPage(view))
                        ) page = QZonePage.Error
                    }
                }
            }
        }.getOrNull()
    }
    DisposableEffect(web) {
        onDispose {
            web?.stopLoading()
            (web?.parent as? android.view.ViewGroup)?.removeView(web)
            web?.destroy()
        }
    }
    val actions = remember(web) { web?.let(::QZoneInteractions) }
    fun toast(res: Int) = Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show()
    fun openPerson(number: String) { navigator.push(Route.QZone(number)) }
    fun loadLikers(post: QZoneFeedCard, previous: QZonePeople?) {
        val bridge = actions ?: return
        if (busy) return
        busy = true
        likerFailed = false
        scope.launch {
            try {
                val next = bridge.likers(post, previous?.next)
                if (likerPost?.anchor != post.anchor) return@launch
                if (next == null) likerFailed = true
                else likerData = QZonePeople(
                    (previous?.people.orEmpty() + next.people).distinctBy { it.uin ?: it.name },
                    next.next.takeUnless { it == previous?.next },
                )
            } finally { busy = false }
        }
    }
    LaunchedEffect(web) { web?.loadUrl(initialUrl) }
    LaunchedEffect(web, generation, showOfficialPage, target) {
        val view = web ?: return@LaunchedEffect
        val bridge = actions ?: return@LaunchedEffect
        if (showOfficialPage || page == QZonePage.Error || generation == 0) return@LaunchedEffect
        if (page == QZonePage.Preview && (feed != null || visitorData != null)) return@LaunchedEffect
        repeat(14) {
            when (qZoneWebHost(view.url)) {
                "i.qq.com" -> { page = QZonePage.Login; return@LaunchedEffect }
                "user.qzone.qq.com" -> {
                    if (route.visitors) {
                        if (bridge.readProfileLink() != null) {
                            visitorData = bridge.visitors(qZoneUin(uin))
                            page = if (visitorData != null) QZonePage.Preview else QZonePage.Error
                            return@LaunchedEffect
                        }
                    } else if (!route.ownProfile && target != null && qZoneProfileTarget(view.url) != target) {
                        // 登录组件有时直接跳到当前账号的动态，不经过独立登录 URL。
                        if (bridge.readProfileLink() != null && tryExplicitContactPage(view)) return@LaunchedEffect
                    } else if (route.ownProfile && target == null) {
                        bridge.readProfileLink()?.let { self ->
                            target = self
                            view.loadUrl(qZoneWebEntry(self, ownProfile = true))
                            return@LaunchedEffect
                        }
                    }
                    val snapshot = if (route.visitors || (route.ownProfile && target == null)) null
                        else parseQZoneWebFeed(bridge.evaluate(previewScript))
                    if (snapshot != null && snapshot.cards.isNotEmpty()) {
                        feed = snapshot
                        page = QZonePage.Preview
                        return@LaunchedEffect
                    }
                }
            }
            if (page == QZonePage.Error) return@LaunchedEffect
            delay(750L)
        }
        if (page == QZonePage.Loading) {
            if (tryExplicitContactPage(view)) return@LaunchedEffect
            page = if (route.visitors) QZonePage.Error else QZonePage.WebOnly
        }
    }

    val useWeb = showOfficialPage || page == QZonePage.Login || page == QZonePage.WebOnly
    LaunchedEffect(web, useWeb, focusAnchor) {
        if (useWeb && focusAnchor.isNotBlank()) {
            delay(200L)
            web?.evaluateJavascript(focusQZoneCard(focusAnchor), null)
            focusAnchor = ""
        }
    }
    M3TimTheme(uiMode = UiMode.Material) {
        ExpressiveScaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(if (route.visitors) R.string.qzone_visitors else R.string.profile_qzone)) },
                    navigationIcon = {
                        TopBarBackButton(
                            onClick = { navigator.pop() },
                            contentDescription = stringResource(R.string.chat_detail_back),
                        )
                    },
                    actions = {
                        if (route.ownProfile) TextButton(onClick = {
                            navigator.push(Route.QZone(target.orEmpty(), visitors = true))
                        }) { Text(stringResource(R.string.qzone_visitors)) }
                        if (web != null) {
                            IconButton(onClick = {
                                val loadInitial = !useWeb || page == QZonePage.Error
                                page = QZonePage.Loading
                                feed = null
                                if (loadInitial) web.loadUrl(initialUrl) else web.reload()
                            }) {
                                Icon(Icons.Filled.Refresh, stringResource(R.string.qzone_refresh))
                            }
                            if (!route.visitors) TextButton(onClick = {
                                if (useWeb) {
                                    showOfficialPage = false
                                    page = QZonePage.Loading
                                    feed = null
                                    generation++
                                } else {
                                    showOfficialPage = true
                                    if (target != null && qZoneProfileTarget(web.url) != target) web.loadUrl(initialUrl)
                                }
                            }) { Text(stringResource(if (useWeb) R.string.qzone_md3_view else R.string.qzone_open_space)) }
                        }
                    },
                    colors = expressiveTopAppBarColors(),
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (web != null) {
                    Column(Modifier.fillMaxSize()) {
                        if (useWeb && (page == QZonePage.Login || page == QZonePage.WebOnly)) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val hint = if (page == QZonePage.Login) R.string.qzone_web_login_hint
                                    else R.string.qzone_web_no_preview
                                Text(stringResource(hint), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { openQZone(context, uin) }) {
                                    Text(stringResource(R.string.qzone_open_original))
                                }
                            }
                        }
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            factory = { web },
                            update = {
                                it.visibility = if (useWeb) View.VISIBLE else View.INVISIBLE
                                it.importantForAccessibility = if (useWeb) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                                    else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                            },
                        )
                    }
                }
                val snapshot = feed
                val visitors = visitorData
                when {
                    web == null || (page == QZonePage.Error && !showOfficialPage) -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(stringResource(if (route.visitors) R.string.qzone_visitors_failed else R.string.qzone_web_error))
                            if (!route.visitors) TextButton(onClick = { openQZone(context, uin) }) {
                                Text(stringResource(R.string.qzone_open_original))
                            }
                        }
                    }
                    !useWeb && route.visitors && visitors != null -> QZonePeopleList(
                        visitors.people, Modifier.fillMaxSize(), onPerson = ::openPerson,
                    )
                    !useWeb && snapshot != null -> QZoneCards(
                        feed = snapshot,
                        photos = photos,
                        currentProfile = target,
                        onFriends = if (target != null) ({ navigator.push(Route.QZone("")) }) else null,
                        onAuthor = ::openPerson,
                        busy = busy,
                        onLike = { post ->
                            if (!busy && actions != null) {
                                busy = true
                                scope.launch {
                                    try {
                                        if (!actions.toggleLike(post)) toast(R.string.qzone_action_unconfirmed)
                                        parseQZoneWebFeed(actions.evaluate(previewScript))?.let { feed = it }
                                    } finally { busy = false }
                                }
                            }
                        },
                        onLikers = { post ->
                            likerPost = post
                            likerData = null
                            loadLikers(post, null)
                        },
                        onComment = { post -> commentPost = post },
                        onOpenWeb = { anchor ->
                            focusAnchor = anchor
                            showOfficialPage = true
                        },
                    )
                    !useWeb -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
        val posting = commentPost
        if (posting != null) ExpressiveInputDialog(
            show = true,
            title = stringResource(R.string.qzone_write_comment),
            initialText = draft?.takeIf { it.first == posting.anchor }?.second.orEmpty(),
            placeholder = stringResource(R.string.qzone_comment_hint),
            confirmText = stringResource(R.string.qzone_send_comment),
            onConfirm = { message ->
                val bridge = actions
                if (bridge != null && !busy) {
                    commentPost = null
                    draft = posting.anchor to message
                    busy = true
                    scope.launch {
                        try {
                            if (bridge.comment(posting, message)) {
                                draft = null
                                toast(R.string.qzone_comment_sent)
                            } else toast(R.string.qzone_action_unconfirmed)
                            parseQZoneWebFeed(bridge.evaluate(previewScript))?.let { feed = it }
                        } finally { busy = false }
                    }
                }
            },
            onDismissRequest = { commentPost = null },
            singleLine = false,
            maxLines = 5,
            minHeight = 96.dp,
            confirmEnabled = { !busy && it.isNotBlank() && it.trim().length <= 500 },
        )
        val likes = likerPost
        if (likes != null) ExpressiveAlertDialog(show = true, onDismissRequest = { likerPost = null }) {
            Text(stringResource(R.string.qzone_likers), style = MaterialTheme.typography.titleLarge)
            val current = likerData
            if (current != null) {
                QZonePeopleList(current.people, Modifier.fillMaxWidth().heightIn(max = 330.dp)) {
                    likerPost = null
                    openPerson(it)
                }
            }
            if (busy) CircularProgressIndicator(Modifier.padding(12.dp))
            if (likerFailed) Text(stringResource(R.string.qzone_likers_failed), Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!busy && (likerFailed || current?.next != null)) {
                    TextButton(onClick = { loadLikers(likes, current) }) {
                        Text(stringResource(if (likerFailed) R.string.group_files_retry else R.string.qzone_load_more))
                    }
                }
                TextButton(onClick = { likerPost = null }) { Text(stringResource(R.string.qzone_close)) }
            }
        }
    }
}

@Composable
private fun QZoneCards(
    feed: QZoneWebFeed,
    photos: QZonePhotoLoader,
    currentProfile: String?,
    onFriends: (() -> Unit)?,
    onAuthor: (String) -> Unit,
    busy: Boolean,
    onLike: (QZoneFeedCard) -> Unit,
    onLikers: (QZoneFeedCard) -> Unit,
    onComment: (QZoneFeedCard) -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp)) {
                if (feed.profile.isNotBlank()) Text(feed.profile, style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.qzone_feed_preview_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onFriends != null) {
                    TextButton(onClick = onFriends) { Text(stringResource(R.string.qzone_friend_feed)) }
                }
            }
        }
        items(feed.cards) { post ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val author = post.authorUin?.takeUnless { it == currentProfile }
                    QZoneAuthor(
                        name = post.author,
                        time = post.time,
                        avatar = post.avatar,
                        photos = photos,
                        onClick = author?.let { { onAuthor(it) } },
                    )
                    if (post.text.isNotBlank()) Text(post.text)
                    val openPost = { onOpenWeb(post.anchor) }
                    QZonePhotos(post.photos, photos, openPost)
                    if (post.hasVideo) QZoneVideo(post.video, openPost)
                    if (post.comments.isNotEmpty()) {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text(stringResource(R.string.qzone_comments), style = MaterialTheme.typography.titleSmall)
                        post.comments.forEach { comment ->
                            val member = comment.authorUin?.takeUnless { it == currentProfile }
                            QZoneAuthor(
                                name = comment.author,
                                time = comment.time,
                                avatar = comment.avatar,
                                photos = photos,
                                small = true,
                                onClick = member?.let { { onAuthor(it) } },
                            )
                            if (comment.text.isNotBlank()) Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                            QZonePhotos(comment.photos, photos, openPost, small = true)
                        }
                    }
                    if (post.anchor.isNotBlank()) {
                        Row {
                            if (post.liked != null) TextButton(onClick = { onLike(post) }, enabled = !busy) {
                                Text(stringResource(if (post.liked == true) R.string.qzone_unlike else R.string.qzone_like))
                            }
                            if (post.likeCount > 0) TextButton(onClick = { onLikers(post) }, enabled = !busy) {
                                Text(stringResource(R.string.qzone_like_count, post.likeCount))
                            }
                        }
                        if (post.canComment) {
                            TextButton(onClick = { onComment(post) }, enabled = !busy) {
                                Text(stringResource(R.string.qzone_write_comment))
                            }
                        }
                    }
                }
            }
        }
        item { TextButton(onClick = { onOpenWeb("") }) { Text(stringResource(R.string.qzone_view_all_in_web)) } }
    }
}

@Composable
private fun QZoneAuthor(
    name: String,
    time: String,
    avatar: String?,
    photos: QZonePhotoLoader,
    small: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QZoneAvatar(avatar, photos, Modifier.size(if (small) 30.dp else 40.dp))
        Column {
            Text(
                text = name.ifBlank { stringResource(R.string.chat_search_unknown_sender) },
                style = if (small) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (time.isNotBlank()) Text(
                time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QZonePeopleList(people: List<QZonePerson>, modifier: Modifier, onPerson: (String) -> Unit) {
    if (people.isEmpty()) {
        Box(modifier.padding(20.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.qzone_people_empty))
        }
        return
    }
    LazyColumn(modifier, contentPadding = PaddingValues(vertical = 8.dp)) {
        items(people) { person ->
            val name = person.name.ifBlank { person.uin ?: stringResource(R.string.chat_search_unknown_sender) }
            val uin = person.uin
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                if (uin != null) TextButton(onClick = { onPerson(uin) }) { Text(name) }
                else Text(name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                if (person.visitSeconds in 1L..4_102_444_800L) {
                    val timestamp = remember(person.visitSeconds) {
                        DateFormat.getDateTimeInstance().format(Date(person.visitSeconds * 1000L))
                    }
                    Text(
                        timestamp,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
        }
    }
}
