package com.material3tim.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import com.material3tim.xposed.UiHost

/**
 * 打开聊天气泡里的链接：
 * - 非 QQ 域名：优先用 **Chrome Custom Tabs**（和 Telegram 一样：页面在应用内打开，
 *   多任务里仍显示 TIM，右上角菜单有「在 Chrome 浏览器中打开 / 正在 Chrome 应用内运行」）；
 *   没有任何 Custom Tabs 提供者时退回系统 ACTION_VIEW。
 * - QQ/腾讯自家域名：交给 TIM 自己的 QQBrowserActivity。
 */
internal object ExternalLink {

    private const val TAG = "Material3Tim"

    private val qqDomains = listOf(
        "qq.com",
        "tencent.com",
        "tencent.cn",
        "url.cn",
        "qqurl.cn",
        "qapp.com",
        "qcloud.com",
        "gtimg.cn",
        "weixin.qq.com",
    )

    /** 常见的 Custom Tabs 提供者（默认浏览器不支持时按顺序尝试）。 */
    private val browserPackages = listOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.chromium.chrome",
        "com.brave.browser",
        "com.microsoft.emmx",
        "org.mozilla.firefox",
        "com.google.android.apps.chrome",
    )

    fun open(context: Context, url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        val host = uri.host?.lowercase().orEmpty()
        val isQqLink = host.isNotEmpty() && qqDomains.any { domain ->
            host == domain || host.endsWith(".$domain")
        }
        if (isQqLink) {
            val internal = Intent().apply {
                setClassName(
                    context.packageName,
                    "com.tencent.mobileqq.activity.QQBrowserActivity",
                )
                putExtra("url", url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val opened = runCatching { context.startActivity(internal) }
                .onFailure { Log.w(TAG, "open qq link in TIM failed: ${it.message}") }
                .isSuccess
            if (opened) return
        }
        if (openCustomTab(context, uri)) return
        openWithSystemBrowser(context, uri)
    }

    /** 默认浏览器优先：系统里解析到的「默认浏览器」如果是 Custom Tabs 提供者就用它。 */
    private fun preferredProvider(context: Context): String? = runCatching {
        CustomTabsClient.getPackageName(context, null)
    }.getOrNull()

    private fun openCustomTab(context: Context, uri: Uri): Boolean {
        val candidates = buildList {
            preferredProvider(context)?.let(::add)
            addAll(browserPackages)
        }.distinct()
        for (pkg in candidates) {
            if (launchCustomTab(context, uri, pkg)) return true
        }
        // 没显式包名时也试一次（让系统按默认处理）。
        return launchCustomTab(context, uri, null)
    }

    private fun launchCustomTab(context: Context, uri: Uri, forcePackage: String?): Boolean {
        return runCatching {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .build()
            if (forcePackage != null) {
                customTabsIntent.intent.setPackage(forcePackage)
            }
            // TIM 的 startActivityForResult 有「即将离开 TIM」外跳确认框，标记后由 hook 直接放行；
            // 不加 NEW_TASK，让 Custom Tab 和 TIM 处在同一个任务（多任务里显示 TIM）。
            UiHost.expectExternalLaunch()
            customTabsIntent.launchUrl(context, uri)
            Log.i(TAG, "opened custom tab via ${forcePackage ?: "system default"}")
            true
        }.getOrElse {
            Log.w(TAG, "custom tab failed (${forcePackage ?: "default"}): ${it.javaClass.simpleName} ${it.message}")
            false
        }
    }

    private fun openWithSystemBrowser(context: Context, uri: Uri) {
        val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        UiHost.expectExternalLaunch()
        runCatching { context.startActivity(browserIntent) }
            .onFailure { Log.w(TAG, "open browser failed: ${it.message}") }
    }
}
