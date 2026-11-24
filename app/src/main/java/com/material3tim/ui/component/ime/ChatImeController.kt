package com.material3tim.ui.component.ime

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 聊天页 IME 控制器：集中封装 v0.5.188 可用的输入法补丁，M3E / Miuix 两套输入栏只做接线。
 *
 * 状态机：IDLE → FOCUSED → SHOW_REQUESTED → VISIBLE / FAILED。
 * 一次焦点事件内的尝试次数上限（禁止循环）：
 * 1. show 阶梯：WindowInsetsControllerCompat.show(ime) + imm.showSoftInput 各一次；
 * 2. 当前会话首次焦点：延迟 80ms 一次 imm.restartInput（Compose 已建立 InputConnection，避免 Gboard 退化英文键盘）；
 * 3. 500ms 仍不可见：一次 imm.showSoftInput 兜底；
 * 4. 仍不可见：一次 imm.restartInput + 一次 hide→show reset；
 * 5. 最后检查可见性，更新 VISIBLE / FAILED。
 *
 * 日志统一格式：`ime: state=.. event=.. reason=..`。
 */
@Suppress("DEPRECATION") // InputMethodManager.SHOW_IMPLICIT 在 API 35 标记 deprecated，但仍是经典 IMM 显示标志
class ChatImeController(private val context: Context) {

    enum class State { IDLE, FOCUSED, SHOW_REQUESTED, VISIBLE, FAILED }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var view: View? = null
    private var chatId: String = ""
    private var reboundChatId: String? = null
    private var inputFocused = false
    private var fallbackAttempted = false
    private var resetAttempted = false
    private var hideShowAttempted = false
    private var job: Job? = null

    /** 当前状态（调试/日志用）。 */
    var state: State = State.IDLE
        private set

    private val imm: InputMethodManager?
        get() = context.getSystemService(InputMethodManager::class.java)

    /** 每次组合（LocalView）更新一次目标 View。 */
    fun attach(view: View) {
        this.view = view
        log("attach")
    }

    /** 会话切换：重置首次焦点重绑标志与一次事件内的补丁次数。 */
    fun noteChatChanged(chatId: String) {
        this.chatId = chatId
        // 切会话后必须重新绑定一次 InputConnection（即使旧会话已经重绑过）。
        reboundChatId = null
        inputFocused = false
        fallbackAttempted = false
        resetAttempted = false
        hideShowAttempted = false
        job?.cancel()
        job = null
        state = State.IDLE
        log("chatChanged")
    }

    /** 焦点进入输入框：按 show 阶梯请求 IME，并启动延迟重绑/兜底/恢复（均单次）。 */
    fun onInputFocusGained() {
        inputFocused = true
        if (isImeVisible()) {
            state = State.VISIBLE
            log("focusGained", "already-visible")
            return
        }
        fallbackAttempted = false
        resetAttempted = false
        hideShowAttempted = false
        state = State.FOCUSED
        log("focusGained")
        showLadder("focus")
    }

    fun onInputFocusLost() {
        inputFocused = false
        job?.cancel()
        job = null
        state = State.IDLE
        log("focusLost")
    }

    /** 外部明确要求显示（如 @ 面板关闭后）。 */
    fun requestShow(reason: String) {
        if (isImeVisible()) {
            state = State.VISIBLE
            log("requestShow", "$reason skipped-visible")
            return
        }
        showLadder(reason)
    }

    /** 只收起 IME；clearFocus 由调用方负责。 */
    fun hideIme(reason: String) {
        job?.cancel()
        job = null
        inputFocused = false
        state = State.IDLE
        view?.let { target ->
            runCatching { imm?.hideSoftInputFromWindow(target.windowToken, 0) }
        }
        log("hide", reason)
    }

    /** 页面不再位于栈顶（横滑/返回/被新页面覆盖）：收起 IME，避免过渡动画期间键盘残留。 */
    fun onRouteLeaving() {
        hideIme("route-leaving")
    }

    /** 页面销毁：取消内部协程并复位，防止泄漏。 */
    fun onScreenDisposed() {
        job?.cancel()
        job = null
        state = State.IDLE
        scope.cancel()
        log("disposed")
    }

    // ---------------- 内部实现 ----------------

    private fun showLadder(reason: String) {
        val target = view ?: return
        state = State.SHOW_REQUESTED
        log("show", reason)

        // 阶梯 1：WindowInsetsControllerCompat.show(ime)（Activity/Dialog 窗口为准）。
        runCatching {
            val window = resolveWindow(target) ?: return@runCatching
            WindowCompat.getInsetsController(window, target).show(WindowInsetsCompat.Type.ime())
        }
        // 阶梯 2：经典 IMM.showSoftInput。
        runCatching { imm?.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT) }
        log("show-immediate", reason)

        // 注意：只有「焦点真正进入输入框」(reason=focus) 才消耗本会话的首次重绑机会。
        // 点击输入框时 UI 会先主动 requestShow(reason=click)，若这里也消耗标志，
        // 紧接着的 focus 阶梯会认为「已重绑过」而跳过 restartInput —— Gboard 的
        // InputConnection 在切换会话后仍然指向旧会话，表现为「键盘弹出来了但打字进不去」。
        val firstFocusForChat = reason == "focus" && reboundChatId != chatId
        if (firstFocusForChat) reboundChatId = chatId

        job?.cancel()
        job = scope.launch {
            var waited = 0L
            if (firstFocusForChat) {
                delay(80L)
                waited += 80L
                if (!inputFocused) return@launch
                val current = view ?: return@launch
                runCatching { imm?.restartInput(current) }
                log("restartInput", "first-focus")
                runCatching { imm?.showSoftInput(current, InputMethodManager.SHOW_IMPLICIT) }
            }
            // 500ms 内轮询可见性：一旦键盘出现过就直接结束（避免用户快速收起后被兜底再次弹起）。
            var visibleSeen = false
            var pollElapsed = 0L
            val deadline = (500L - waited).coerceAtLeast(0L)
            while (pollElapsed < deadline && inputFocused) {
                delay(50L)
                pollElapsed += 50L
                if (isImeVisible()) {
                    visibleSeen = true
                    break
                }
            }
            if (!inputFocused) return@launch
            if (visibleSeen || isImeVisible()) {
                state = State.VISIBLE
                log("visible")
                return@launch
            }
            // 500ms 兜底：一次 imm.showSoftInput。
            if (!fallbackAttempted) {
                fallbackAttempted = true
                val current = view ?: return@launch
                runCatching { imm?.showSoftInput(current, InputMethodManager.SHOW_IMPLICIT) }
                log("fallbackShow", reason)
            }
            // 兜底后只等待结果：不再 restartInput / hide→show reset（会把已弹出的键盘再次收起，造成闪烁）。
            delay(250L)
            if (isImeVisible()) {
                state = State.VISIBLE
                log("visible-after-fallback")
            } else {
                state = State.FAILED
                log("failed", reason)
            }
        }
    }

    /** 当前键盘是否可见（供面板开关前记录状态）。 */
    fun isImeVisibleNow(): Boolean = isImeVisible()

    private fun isImeVisible(): Boolean {
        val target = view ?: return false
        return runCatching {
            ViewCompat.getRootWindowInsets(target)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }.getOrDefault(false)
    }

    private fun resolveWindow(target: View): android.view.Window? {
        var parent = target.parent
        while (parent != null) {
            if (parent is DialogWindowProvider) return parent.window
            parent = parent.parent
        }
        return (target.context as? android.app.Activity)?.window
    }

    private fun log(event: String, reason: String = "") {
        Log.d("Material3Tim", "ime: state=$state event=$event reason=$reason")
    }
}
