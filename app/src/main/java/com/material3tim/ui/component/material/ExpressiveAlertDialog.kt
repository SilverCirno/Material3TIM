package com.material3tim.ui.component.material

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.material3tim.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 打开时卡片缩放起点（方案 A：0.92 → 1 spring 弹出）。 */
private const val CardEnterScale = 0.92f

/** 关闭时卡片缩放终点（与打开起点一致，避免视觉跳变）。 */
private const val CardExitScale = 0.92f

/** 自绘全屏遮罩不透明度（32% 黑）。 */
private const val ScrimAlpha = 0.32f

/**
 * 方案 A：M3E 自绘 Expressive 居中弹窗。
 *
 * - `Dialog` 只作为全屏容器（`usePlatformDefaultWidth=false`、`decorFitsSystemWindows=false`），
 *   不使用系统窗口 dim/窗口动画；
 * - 自绘全屏 32% 黑色遮罩（覆盖状态栏/导航栏，点击遮罩关闭）+ 自绘进出动画；
 * - 打开：遮罩 fadeIn 180ms，卡片 scaleIn(0.92→1, spring dampingRatio=0.8) + fadeIn 160ms；
 * - 关闭：卡片 scaleOut(1→0.92, tween 140) + fadeOut 120ms，遮罩 fadeOut 150ms，
 *   **动画全部播完才真正移除 Dialog**（MutableTransitionState + dialogVisible 控制，避免 0 帧消失）；
 * - 卡片：28dp 圆角 / surfaceContainerHigh / 阴影 6dp，内容四周 24dp，居中并避开系统栏。
 */
@Composable
internal fun ExpressiveAlertDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onExitFinished: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 关闭动画期间 Dialog 仍需存在：dialogVisible 直到退出动画 idle 才置 false。
    var dialogVisible by remember { mutableStateOf(show) }
    // 初值 currentState=false：即使首帧就是 show=true 也能播放入场动画；
    // 条件式挂载的调用点（?.let { Dialog(show=true) }）同样有动画。
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = show }
    }
    LaunchedEffect(show) {
        if (show) dialogVisible = true
        visibleState.targetState = show
    }
    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) {
            dialogVisible = false
            onExitFinished?.invoke()
        }
    }
    if (!dialogVisible) return
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // 方案 A 沉浸修复：Dialog 只作全屏容器，窗口本身透明（状态栏/导航栏也用遮罩盖住），
        // 不 resize（IME 打开时窗口不动，由下方自绘卡片上移 + 高度约束处理）。
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(dialogWindow) {
            dialogWindow?.let(::configureExpressiveDialogWindow)
            onDispose { }
        }
        val transition = rememberTransition(visibleState, label = "expressive-dialog")
        // 遮罩：打开 180ms fadeIn；关闭 150ms fadeOut。
        val scrimProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(durationMillis = 180)
                } else {
                    tween(durationMillis = 150)
                }
            },
            label = "expressive-dialog-scrim",
        ) { visible -> if (visible) 1f else 0f }
        // 卡片缩放：打开 spring 0.92→1；关闭 tween 140 1→0.92。
        val cardScale by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    spring(dampingRatio = 0.8f)
                } else {
                    tween(durationMillis = 140)
                }
            },
            label = "expressive-dialog-scale",
        ) { visible -> if (visible) 1f else CardExitScale }
        // 卡片透明度：打开 160ms；关闭 120ms。
        val cardAlpha by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(durationMillis = 160)
                } else {
                    tween(durationMillis = 120)
                }
            },
            label = "expressive-dialog-alpha",
        ) { visible -> if (visible) 1f else 0f }

        // 预测性返回跟手进度：0 = 正常，1 = 手势完成方向（API34+ 有跟手，API33 只有 invoked）。
        val backProgress = remember { Animatable(0f) }
        var backDismissing by remember { mutableStateOf(false) }
        val backScope = rememberCoroutineScope()
        LaunchedEffect(show) {
            if (show) {
                backProgress.snapTo(0f)
                backDismissing = false
            }
        }
        DialogPredictiveBackEffect(
            onProgress = { progress ->
                backScope.launch { backProgress.snapTo(progress.coerceIn(0f, 1f)) }
            },
            onCancelled = {
                backScope.launch {
                    backProgress.animateTo(0f, tween(durationMillis = 180))
                }
            },
            onInvoked = {
                // 手势完成：先播到 1 再走既有退出动画 dismiss；backDismissing 防重复 dismiss。
                if (!backDismissing) {
                    backDismissing = true
                    backScope.launch {
                        backProgress.animateTo(1f, tween(durationMillis = 140))
                        onDismissRequest()
                    }
                }
            },
        )
        // 预测性返回进度映射：卡片缩小/淡出、遮罩变淡（0.32 → 0.064）。
        val back = backProgress.value

        // IME / 系统栏：窗口 ADJUST_NOTHING 不 resize，这里用 WindowInsets 自行
        // 把卡片居中到「键盘以上的可视区」。
        val density = LocalDensity.current
        val imeBottomPx = WindowInsets.ime.getBottom(density)
        val statusTopPx = WindowInsets.statusBars.getTop(density)
        val navBottomPx = WindowInsets.navigationBars.getBottom(density)
        val imeBottomDp = with(density) { imeBottomPx.toDp() }
        val statusTopDp = with(density) { statusTopPx.toDp() }
        val navBottomDp = with(density) { navBottomPx.toDp() }
        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
        // 卡片高度上限：屏幕高 - 键盘 - 状态栏 - 导航栏 - 上下边距；超出内容区自己滚动。
        val maxCardHeight = (screenHeightDp - imeBottomDp - statusTopDp - navBottomDp - 48.dp)
            .coerceAtLeast(160.dp)

        Box(modifier = Modifier.fillMaxSize()) {
            // 自绘全屏遮罩（覆盖状态栏/导航栏）：点击关闭。
            val scrimInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = (ScrimAlpha * scrimProgress) * (1f - 0.8f * back)),
                    )
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            )
            // 卡片层：默认居中；键盘弹出时整体上移 ime/2，使卡片居中于「键盘以上可视区」，
            // 不会贴到状态栏，也不会随 IME resize 跳变。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, -(imeBottomPx / 2)) },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // 状态栏/导航栏安全区只作用于卡片，与 IME 偏移分开计算、不叠加。
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = statusTopDp + 24.dp,
                            bottom = navBottomDp + 24.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .heightIn(max = maxCardHeight)
                            .graphicsLayer {
                                // 预测性返回跟手：scale 1→0.92、alpha 1→0.4。
                                scaleX = cardScale * (1f - 0.08f * back)
                                scaleY = cardScale * (1f - 0.08f * back)
                                alpha = cardAlpha * (1f - 0.6f * back)
                            },
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 6.dp,
                        tonalElevation = 6.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            content = content,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 关闭动画收尾控制器：点击取消/确认时先播放退出动画，动画播完后再执行真正的
 * [pendingAction]（父级 set show=false / 清空选中状态等），保证条件式挂载的调用点也能看到关闭动画。
 */
private class DialogExitController {
    private var pendingAction: (() -> Unit)? = null
    var exitRequested by mutableStateOf(false)
        private set

    /** 请求播完退出动画后再执行 [afterAnimation]。 */
    fun requestExit(afterAnimation: () -> Unit) {
        pendingAction = afterAnimation
        exitRequested = true
    }

    /** 退出动画播完：执行挂起动作并复位。 */
    fun onExitFinished() {
        val action = pendingAction
        pendingAction = null
        exitRequested = false
        action?.invoke()
    }

    fun reset() {
        pendingAction = null
        exitRequested = false
    }

    fun effectiveShow(show: Boolean): Boolean = show && !exitRequested
}

@Composable
private fun rememberDialogExitController(show: Boolean): DialogExitController {
    val controller = remember { DialogExitController() }
    LaunchedEffect(show) {
        if (show) controller.reset()
    }
    return controller
}

/**
 * 确认型 Expressive 弹窗：标题 + 可选说明 + 取消/确认。
 * [danger]=true 时确认按钮用 error 色；[confirmLoading]=true 时按钮内显示进度并禁用。
 */
@Composable
internal fun ExpressiveConfirmDialog(
    show: Boolean,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    message: String? = null,
    danger: Boolean = false,
    confirmEnabled: Boolean = true,
    confirmLoading: Boolean = false,
) {
    val exitController = rememberDialogExitController(show)
    ExpressiveAlertDialog(
        show = exitController.effectiveShow(show),
        onDismissRequest = { exitController.requestExit(onDismissRequest) },
        onExitFinished = { exitController.onExitFinished() },
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.heightIn(min = 8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.heightIn(min = 20.dp))
        DialogButtonRow(
            confirmText = confirmText,
            onConfirm = { exitController.requestExit(onConfirm) },
            onDismissRequest = { exitController.requestExit(onDismissRequest) },
            danger = danger,
            confirmEnabled = confirmEnabled,
            loading = confirmLoading,
        )
    }
}

/**
 * 输入型 Expressive 弹窗：标题 + 自动聚焦 OutlinedTextField（单行/多行）+ 取消/确认。
 * 打开时自动聚焦并请求焦点；外层已 `imePadding()`，键盘弹出时卡片上移。
 */
@Composable
internal fun ExpressiveInputDialog(
    show: Boolean,
    title: String,
    initialText: String,
    placeholder: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    minHeight: Dp = 0.dp,
    confirmEnabled: (String) -> Boolean = { it.isNotBlank() },
) {
    var text by remember(show) { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }
    val exitController = rememberDialogExitController(show)
    LaunchedEffect(show) {
        if (show) {
            delay(120)
            runCatching { focusRequester.requestFocus() }
        }
    }
    ExpressiveAlertDialog(
        show = exitController.effectiveShow(show),
        onDismissRequest = { exitController.requestExit(onDismissRequest) },
        onExitFinished = { exitController.onExitFinished() },
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.heightIn(min = 14.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = singleLine,
            maxLines = maxLines,
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier),
        )
        Spacer(Modifier.heightIn(min = 20.dp))
        DialogButtonRow(
            confirmText = confirmText,
            onConfirm = { exitController.requestExit { onConfirm(text) } },
            onDismissRequest = { exitController.requestExit(onDismissRequest) },
            confirmEnabled = confirmEnabled(text),
        )
    }
}

/**
 * 自定义禁言时长弹窗：标题 + 天/时/分三个数字输入框 + 取消/确认。
 * 总秒数 = 天*86400 + 时*3600 + 分*60，≤0 时确认禁用；校验范围 天0~365、时0~23、分0~59。
 */
@Composable
internal fun ExpressiveDurationDialog(
    show: Boolean,
    title: String,
    onConfirm: (days: String, hours: String, minutes: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var days by remember(show) { mutableStateOf("") }
    var hours by remember(show) { mutableStateOf("") }
    var minutes by remember(show) { mutableStateOf("") }
    val exitController = rememberDialogExitController(show)
    val totalSeconds = durationTotalSeconds(days, hours, minutes)
    ExpressiveAlertDialog(
        show = exitController.effectiveShow(show),
        onDismissRequest = { exitController.requestExit(onDismissRequest) },
        onExitFinished = { exitController.onExitFinished() },
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.heightIn(min = 14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DurationField(
                label = stringResource(R.string.group_manage_mute_days),
                value = days,
                onValueChange = { days = sanitizeDurationDigits(it, 3) },
                modifier = Modifier.weight(1f),
            )
            DurationField(
                label = stringResource(R.string.group_manage_mute_hours),
                value = hours,
                onValueChange = { hours = sanitizeDurationDigits(it, 2) },
                modifier = Modifier.weight(1f),
            )
            DurationField(
                label = stringResource(R.string.group_manage_mute_minutes),
                value = minutes,
                onValueChange = { minutes = sanitizeDurationDigits(it, 2) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.heightIn(min = 20.dp))
        DialogButtonRow(
            confirmText = stringResource(R.string.confirm),
            onConfirm = {
                exitController.requestExit { onConfirm(days, hours, minutes) }
            },
            onDismissRequest = { exitController.requestExit(onDismissRequest) },
            confirmEnabled = totalSeconds > 0,
        )
    }
}

@Composable
private fun DurationField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

private fun sanitizeDurationDigits(value: String, maxLength: Int): String =
    value.filter { it.isDigit() }.take(maxLength)

private fun durationTotalSeconds(days: String, hours: String, minutes: String): Int =
    (days.toIntOrNull()?.coerceIn(0, 365) ?: 0) * 86400 +
        (hours.toIntOrNull()?.coerceIn(0, 23) ?: 0) * 3600 +
        (minutes.toIntOrNull()?.coerceIn(0, 59) ?: 0) * 60

/**
 * 列表选择型 Expressive 弹窗（群文件「移动到」）：标题 + 可滚动列表（≤60% 屏高）+ 取消。
 * 点击某一项立即回调下标（原逻辑在回调里执行）。
 */
@Composable
internal fun ExpressiveListDialog(
    show: Boolean,
    title: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val maxListHeight = (LocalConfiguration.current.screenHeightDp * 0.6f).dp
    val exitController = rememberDialogExitController(show)
    ExpressiveAlertDialog(
        show = exitController.effectiveShow(show),
        onDismissRequest = { exitController.requestExit(onDismissRequest) },
        onExitFinished = { exitController.onExitFinished() },
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.heightIn(min = 12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxListHeight),
        ) {
            itemsIndexed(options) { index, label ->
                TonalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    onClick = { exitController.requestExit { onSelect(index) } },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { exitController.requestExit(onDismissRequest) }) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    }
}

/** 底部「取消 / 确认」按钮行：确认可 danger/enabled/loading。 */
@Composable
private fun DialogButtonRow(
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    danger: Boolean = false,
    confirmEnabled: Boolean = true,
    loading: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDismissRequest) {
            Text(text = stringResource(R.string.cancel))
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled && !loading,
            colors = if (danger) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(text = confirmText)
            }
        }
    }
}

/**
 * 配置自绘弹窗的 Dialog 窗口（问题 1：状态栏/导航栏沉浸；问题 2：IME 不 resize）。
 * - 窗口背景透明 + 系统栏透明 + FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS，让自绘遮罩真正铺满全屏；
 * - `setDimAmount(0f)`：不使用系统窗口 dim（遮罩自绘）；
 * - `setLayout(MATCH_PARENT, MATCH_PARENT)`：全屏容器，遮罩覆盖状态栏/导航栏区域；
 * - `SOFT_INPUT_ADJUST_NOTHING`：键盘弹出时不 resize，卡片位置由 Compose 用 IME insets 自己算。
 */
@Suppress("DEPRECATION")
private fun configureExpressiveDialogWindow(window: Window) {
    window.setDimAmount(0f)
    window.setLayout(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
    )
    window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(false)
    } else {
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
    }
    window.statusBarColor = android.graphics.Color.TRANSPARENT
    window.navigationBarColor = android.graphics.Color.TRANSPARENT
}

/**
 * Dialog 窗口的预测性返回注册（API 33+）。
 *
 * Compose Dialog 的视图在首帧可能还没 attach 到 window，拿不到 OnBackInvokedDispatcher，
 * 因此像 ChatAtPanel 的 DialogBackDispatchEffect 一样用 view.postDelayed 重试若干次。
 * - API34+：OnBackAnimationCallback，onBackProgressed 跟手 / onBackCancelled 回弹 / onBackInvoked 完成；
 * - API33：普通 OnBackInvokedCallback（无跟手，直接走完成逻辑 → 既有退出动画）；
 * - API<33 / 注册失败：不注册、不消费，退回 Dialog 默认返回键 dismiss 行为（不崩溃）。
 */
@Composable
private fun DialogPredictiveBackEffect(
    onProgress: (Float) -> Unit,
    onCancelled: () -> Unit,
    onInvoked: () -> Unit,
) {
    val view = LocalView.current
    val latestOnProgress = rememberUpdatedState(onProgress)
    val latestOnCancelled = rememberUpdatedState(onCancelled)
    val latestOnInvoked = rememberUpdatedState(onInvoked)
    DisposableEffect(view) {
        var disposed = false
        var unregister: (() -> Unit)? = null
        fun attempt(remaining: Int) {
            if (disposed || unregister != null) return
            val cleanup = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerDialogBackDispatch(
                    view = view,
                    onProgress = { latestOnProgress.value(it) },
                    onCancelled = { latestOnCancelled.value() },
                    onInvoked = { latestOnInvoked.value() },
                )
            } else {
                null
            }
            if (cleanup != null) {
                unregister = cleanup
            } else if (remaining > 0) {
                view.postDelayed({ attempt(remaining - 1) }, 50L)
            }
        }
        attempt(6)
        onDispose {
            disposed = true
            unregister?.invoke()
            unregister = null
        }
    }
}

/** API33+：取 View 所在窗口的 OnBackInvokedDispatcher 注册返回回调，返回 unregister。 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun registerDialogBackDispatch(
    view: View,
    onProgress: (Float) -> Unit,
    onCancelled: () -> Unit,
    onInvoked: () -> Unit,
): (() -> Unit)? {
    val dispatcher = runCatching { view.findOnBackInvokedDispatcher() }.getOrNull() ?: return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        registerDialogPredictiveBackDispatch(dispatcher, onProgress, onCancelled, onInvoked)
    } else {
        val callback = OnBackInvokedCallback { onInvoked() }
        val registered = runCatching {
            dispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                callback,
            )
        }.isSuccess
        if (registered) {
            { runCatching { dispatcher.unregisterOnBackInvokedCallback(callback) } }
        } else {
            null
        }
    }
}

/** API34+：预测性返回动画回调（跟手进度 / 取消回弹 / 完成后关闭）。 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun registerDialogPredictiveBackDispatch(
    dispatcher: OnBackInvokedDispatcher,
    onProgress: (Float) -> Unit,
    onCancelled: () -> Unit,
    onInvoked: () -> Unit,
): (() -> Unit)? {
    val callback = object : OnBackAnimationCallback {
        override fun onBackStarted(backEvent: BackEvent) = Unit

        override fun onBackProgressed(backEvent: BackEvent) {
            onProgress(backEvent.progress)
        }

        override fun onBackCancelled() {
            onCancelled()
        }

        override fun onBackInvoked() {
            onInvoked()
        }
    }
    runCatching {
        dispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            callback,
        )
    }.getOrElse { return null }
    return { runCatching { dispatcher.unregisterOnBackInvokedCallback(callback) }.getOrNull() }
}
