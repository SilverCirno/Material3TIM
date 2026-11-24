package com.material3tim.ui.component.ime

import android.graphics.Rect
import android.util.Log
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 可靠的键盘高度（dp），用于聊天页内容底部 padding。
 *
 * 背景：TIM 宿主窗口里 Compose 的 `WindowInsets.ime` 可能为 0/不更新，因此：
 * 1. 优先用 View 层 `ViewCompat.getRootWindowInsets(view).getInsets(Type.ime()).bottom`
 *    （不经过 Compose inset 消费，宿主窗口异常时也可能拿不到 → 0）；
 * 2. 兜底用 `ViewTreeObserver.OnGlobalLayoutListener` + `getWindowVisibleDisplayFrame`：
 *    `keyboard = rootView.height - visibleFrame.bottom - navigationBarBottom`；
 * 3. 两者取较大值作为最终高度；仅在变化时打日志
 *    `ime height: composeInsets=..px detected=..px dp=..`（TAG=Material3Tim）。
 *
 * 只做布局高度，不做 IME show/hide（那部分在 ChatImeController）。
 */
@Composable
fun rememberKeyboardHeightDp(): State<Dp> {
    val view = LocalView.current
    val density = LocalDensity.current
    // Compose insets（可能为 0/不更新，仅作候选之一）。
    val composeImePx = WindowInsets.ime.getBottom(density)
    var detectedPx by remember { mutableIntStateOf(0) }

    DisposableEffect(view) {
        val root = view.rootView
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            runCatching { root.getWindowVisibleDisplayFrame(rect) }
            val navBarBottom = runCatching {
                ViewCompat.getRootWindowInsets(root)
                    ?.getInsets(WindowInsetsCompat.Type.navigationBars())
                    ?.bottom ?: 0
            }.getOrDefault(0)
            // 键盘高度 = 根视图高度 - 可见区域底部 - 导航栏高度（可见区域含状态栏，故不扣 top）。
            detectedPx = (root.height - rect.bottom - navBarBottom).coerceAtLeast(0)
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose { root.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }

    val effectivePx = maxOf(composeImePx, detectedPx)
    val keyboardHeightDp = with(density) { effectivePx.toDp() }
    // 用 State 暴露高度：聊天页 LaunchedEffect(Unit) 里的 snapshotFlow 必须读取 State.value，
    // 直接捕获 Dp 局部变量会拿到首次组合的 0.dp（永远不变化，滚动逻辑不会执行）。
    val state = remember { mutableStateOf(keyboardHeightDp) }
    SideEffect {
        if (state.value != keyboardHeightDp) state.value = keyboardHeightDp
    }
    LaunchedEffect(composeImePx, detectedPx) {
        Log.d(
            "Material3Tim",
            "ime height: composeInsets=${composeImePx}px detected=${detectedPx}px dp=${keyboardHeightDp.value}",
        )
    }
    return state
}


