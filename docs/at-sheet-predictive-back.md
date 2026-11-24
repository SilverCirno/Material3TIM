# @ Bottom Sheet 的预测性返回实现（M3E 标准组件）

v0.5.119 起，M3E 的「选择提醒的人」Sheet 使用**标准 material3 `ModalBottomSheet`**，
同时具备与 Miuix 一致的预测性返回手势：手势拖动时 Sheet 跟手下滑、取消回弹、完成关闭。

本文记录这套做法，避免以后换组件/改宿主时重走弯路。

## 1. 为什么标准 Sheet 一开始收不到返回

- 项目里“页面级”的预测性返回来自 **Miuix NavDisplay + androidx.navigationevent**
  （模块通过 `enableOnBackInvokedCallback` 让 TIM 进程参与预测性返回），
  所有走 NavDisplay 的路由以及 Miuix `OverlayBottomSheet` 都能正常工作。
- material3 的 `ModalBottomSheet` 是 **Dialog 窗口**：它的返回事件走 Dialog 自己的
  `OnBackInvokedDispatcher`，不在 NavDisplay 的返回管线里；并且 material3
  1.5.0-alpha28 内部没有注册预测性返回回调（`ModalBottomSheetProperties` 只有
  `shouldDismissOnBackPress` 之类的布尔开关），所以返回手势/返回键都关不掉它。
- 早期为了能用，曾把 M3E Sheet 改成自绘 overlay（能跟手但外观不是标准组件）；
  按用户要求已换回标准 `ModalBottomSheet`，改为在 **Dialog 窗口层**补返回通道。

## 2. 实现方式（`ChatAtPanel.kt` · `ChatAtSheetMaterial` / `DialogBackDispatchEffect`）

1. 保持标准组件负责外观和手势：
   - `ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true))`；
   - scrim、大屏限宽、insets、向下拖拽关闭都由 material3 处理。
2. 背景与安全区（沉浸化）：
   - `containerColor = Color.Transparent`、`dragHandle = null`；
   - 内容根自绘 Surface：顶部 28dp 圆角 + `surfaceContainer` 背景 + 顶部 drag handle；
   - `contentWindowInsets = { WindowInsets(0, 0, 0, 0) }` 让 Sheet 背景一直画到导航栏后面；
   - 底部「取消 / 确定(n)」用 `atSheetBottomInset()` 避开安全区：
     键盘收起时取 `navigationBars` 高度，键盘弹出时取 0（由 `imePadding`/窗口处理），
     避免双重留白。
3. 预测性返回（核心）：
   - 在 Sheet 内容（即 Dialog 窗口的 composition）里取
     `LocalView.current.findOnBackInvokedDispatcher()`；
     Dialog 首帧可能未 attach，用 `view.postDelayed` 重试若干次，`onDispose` 时反注册。
   - API 34+ 注册 `OnBackAnimationCallback`：
     - `onBackProgressed(backEvent)` → `Animatable.snapTo(backEvent.progress)`；
     - 进度驱动内容根 `graphicsLayer { translationY = progress * size.height }`，实现跟手下滑；
     - `onBackCancelled` → `animateTo(0f)` 回弹；
     - `onBackInvoked` → `sheetState.hide()` + `onDismiss()`（幂等）。
   - API 33 注册普通 `OnBackInvokedCallback`（直接 hide + dismiss，无跟手动画）；
   - API < 33 或拿不到 dispatcher：静默降级，保留标准组件行为。
   - 优先级用 `OnBackInvokedDispatcher.PRIORITY_DEFAULT`。
4. `@` 面板可重新唤起（与预测返回同批修复）：
   - `ChatComposerState.onAtTextChanged` 的打开条件由「新文本以 `@` 结尾且旧文本不以 `@` 结尾」
     改为「本次输入使文本变长且以 `@`/`＠` 结尾」。
   - 这样关闭 Sheet 后输入框残留 `@` 时，再输入一个 `@`（变成 `@@`）也能重新打开面板。

## 3. 与 Miuix 的关系

- Miuix 的 `OverlayBottomSheet` 仍保持原有实现（In-Compose overlay + 自己的预测返回逻辑），未改动。
- M3E 标准 Sheet 走的是**系统 Dialog 窗口的预测返回回调**，与 NavDisplay 页面的
  navigationevent 管线互不影响；两套 UI 的返回行为一致：手势可跟手、可取消、可完成。

## 4. 排查要点

- 若返回无反应：先确认 TIM 进程已 opt-in 预测性返回（模块 `enable_predictive_back`
  生效，`ApplicationInfo.flags` 带 `FLAG_ENABLE_ON_BACK_INVOKED_CALLBACK`）；
- 若跟手无效但能关闭：检查 `findOnBackInvokedDispatcher()` 是否在 Dialog 首帧拿到、
  是否被别的高优先级回调抢先；
- 若底部按钮被手势条挡：确认 `atSheetBottomInset()` 生效，且没有和
  `contentWindowInsets` 同时加 padding 造成双重 inset。
