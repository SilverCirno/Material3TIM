package com.material3tim.ui.component.miuix.glass

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassPopup
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 菜单里「删除」等危险项的红色（与气泡菜单 / 会话菜单的既有红色一致）。 */
val GlassMenuDangerColor: androidx.compose.ui.graphics.Color =
    androidx.compose.ui.graphics.Color(0xFFE5484D)

/**
 * 根级玻璃菜单的一个菜单项。
 *
 * 触发方只描述「文案 + 图标 + 是否危险 + 是否可用 + 业务动作」；
 * 真正的 `GlassTransformPopup` / 位置计算 / 进出场动画全部交给页面级 [GlassMenuHost]，
 * 这样菜单不会被气泡 / 列表行 / 面板的父级裁剪。
 */
data class GlassMenuEntry(
    val label: String,
    val icon: ImageVector? = null,
    val isDanger: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * 玻璃菜单状态（每个可弹菜单的页面各自 remember 一个，经 [LocalGlassMenu] 下发）。
 *
 * 两种锚点路径：
 * - **anchor 路径（主路径）**：触发控件内用一个 1dp 透明 Box（位置 = 手指按压点 / 相机项中心点）
 *   挂 `glassPopupAnchor`，把窗口矩形写进 [GlassPopupAnchor]；宿主用 `GlassTransformPopup`
 *   从该 1x1 锚点形状变换展开 / 收缩，**内置预测性返回跟手动画**（返回手势直接驱动回缩）。
 * - **anchorBounds 兜底**：没有 anchor 对象时退回 `GlassPopup(anchorBounds = Rect)`（无 morph，
 *   但不会崩溃）。
 *
 * 首帧机制（[pendingOpen]）：`GlassTransformPopup` 只有在「先以 show=false 组合、之后再置 true」
 * 时才有入场过渡；而 host 的 anchor 分支是首次打开时才挂载的。因此 [show] 先让 anchor 分支以
 * show=false 组合一帧，宿主用 [openNow] 在下一帧把 visible 置 true，入场动画才完整。
 */
@Stable
class GlassMenuState {
    /** 菜单是否可见（驱动 host 中 GlassTransformPopup / GlassPopup 的 show）。 */
    var visible: Boolean by mutableStateOf(false)
        private set

    /** 触发控件的 morph 锚点（GlassTransformPopup 用：1x1 窗口矩形 + 预测性返回/形状变换）。 */
    var anchor: GlassPopupAnchor? by mutableStateOf(null)
        private set

    /** 首帧延迟：新 anchor 分支先以 show=false 组合一帧，宿主 [openNow] 再打开（保证入场形状动画）。 */
    private var pendingOpen: Boolean by mutableStateOf(false)

    /** 供宿主 LaunchedEffect 观察：anchor 已组合、等待下一帧触发入场动画。 */
    internal val pendingOpenForHost: Boolean get() = pendingOpen

    /** 无 anchor 时的兜底锚点矩形（窗口坐标）；正常路径不使用。 */
    var anchorBounds: Rect by mutableStateOf(Rect.Zero)
        private set

    /** 当前菜单项（hide/dismiss 后保留，保证退场动画期间内容不闪空）。 */
    var entries: List<GlassMenuEntry> by mutableStateOf(emptyList())
        private set

    /** 触发时页面提供的背景采样；null 时组件自动走不透明回退（描边/阴影保留）。 */
    var backdrop: Backdrop? by mutableStateOf(null)
        private set

    /** 外部关闭（空白点击 / 返回键 / 预测性返回）时通知触发方清除自己的 show 状态。 */
    private var onDismiss: (() -> Unit)? = null

    /** 当前菜单的发出方 token：触发方离开组合时用于避免残留菜单（见 [hideIfOwner]）。 */
    private var owner: Any? = null

    /**
     * 打开菜单（主路径）：传入触发控件的 morph 锚点（1x1 按压点），
     * 宿主用 `GlassTransformPopup` 从该点展开（并跟随预测性返回收缩回该点）。
     *
     * 会先延迟一帧（[pendingOpen]）再真正置 visible，保证 anchor 分支的入场形状动画有起点。
     */
    fun show(
        owner: Any,
        anchor: GlassPopupAnchor?,
        entries: List<GlassMenuEntry>,
        backdrop: Backdrop?,
        onDismiss: () -> Unit,
    ) {
        this.owner = owner
        this.anchor = anchor
        this.entries = entries
        this.backdrop = backdrop
        this.onDismiss = onDismiss
        if (anchor == null) {
            // 兜底路径不需要形状动画，直接显示。
            pendingOpen = false
            visible = true
        } else {
            // 先以 show=false 组合 anchor 分支，下一帧再由宿主 openNow 打开 → 入场过渡正常。
            visible = false
            pendingOpen = true
        }
    }

    /** 打开菜单（兜底）：没有 morph 锚点时用旧 Rect 定位的 GlassPopup（无形状动画）。 */
    fun show(
        owner: Any,
        anchorBounds: Rect,
        entries: List<GlassMenuEntry>,
        backdrop: Backdrop?,
        onDismiss: () -> Unit,
    ) {
        this.owner = owner
        this.anchor = null
        this.anchorBounds = anchorBounds
        this.entries = entries
        this.backdrop = backdrop
        this.onDismiss = onDismiss
        pendingOpen = false
        visible = true
    }

    /** 仅关闭菜单、不回调触发方（用于「点击菜单项」：业务动作马上会自己清理状态）。 */
    fun hide() {
        visible = false
        pendingOpen = false
        onDismiss = null
        owner = null
        // 保留 anchor / entries / backdrop：GlassTransformPopup 退场动画期间仍需要锚点与内容，
        // 否则浮层会瞬间消失（表现成「直接关闭没动画」）。
    }

    /** 外部关闭菜单：先隐藏，再通知触发方同步状态（点击空白 / 返回 / 预测性返回）。 */
    fun dismiss() {
        if (!visible) return
        visible = false
        pendingOpen = false
        val callback = onDismiss
        onDismiss = null
        owner = null
        // 保留 anchor / entries / backdrop：预测性返回/关闭的收缩动画完整播放。
        callback?.invoke()
    }

    /**
     * 触发方离开组合时调用：只有「当前菜单确实由它弹出」才关闭。
     * 例如消息被对方撤回 / 群成员被移出列表时，气泡/行消失后菜单不应残留在屏幕上。
     */
    fun hideIfOwner(owner: Any) {
        if (this.owner !== owner || !visible) return
        visible = false
        pendingOpen = false
        onDismiss = null
        this.owner = null
    }

    /** 由宿主在「anchor 分支已以 show=false 组合一帧」后调用，真正把菜单置为可见（触发入场形状动画）。 */
    internal fun openNow() {
        if (!pendingOpen) return
        pendingOpen = false
        visible = true
    }
}

/** 页面内触发方用来打开页面级玻璃菜单；页面未提供时为 null（不会弹菜单，但不会崩溃）。 */
val LocalGlassMenu = compositionLocalOf<GlassMenuState?> { null }

/**
 * 页面级玻璃菜单宿主：放在页面内容层根 `Box` 的最后（NavDisplay 子树内 → 返回优先级在页面处理器之后）。
 *
 * - anchor != null：`GlassTransformPopup` 从 1x1 按压点形状变换展开 / 收缩，
 *   **预测性返回跟手动画由组件内部完成**（返回手势进度直接映射到面板回缩），不受控件矩形干扰；
 * - anchor == null：`GlassPopup(anchorBounds = Rect)` 兜底（无 morph，不崩溃）；
 * - 不做全屏压暗；退场期间保留 anchor/entries，内容不闪空。
 */
@Composable
fun BoxScope.GlassMenuHost(state: GlassMenuState) {
    val anchor = state.anchor
    if (anchor != null) {
        // 新 anchor 第一次组合时 show=false；等这一帧真正完成组合后，下一帧再置 visible=true，
        // 保证 GlassTransformPopup 的 updateTransition 从 false 过渡到 true（入场形状动画）。
        LaunchedEffect(anchor, state.pendingOpenForHost) {
            if (state.pendingOpenForHost) {
                withFrameNanos { }
                state.openNow()
            }
        }
        GlassTransformPopup(
            show = state.visible,
            onDismissRequest = { state.dismiss() },
            anchor = anchor,
            backdrop = state.backdrop,
            anchorContent = {},
            simplified = true,
        ) {
            state.entries.forEach { entry -> GlassMenuItem(entry, state) }
        }
    } else {
        GlassPopup(
            show = state.visible,
            onDismissRequest = { state.dismiss() },
            anchorBounds = state.anchorBounds,
            backdrop = state.backdrop,
        ) {
            state.entries.forEach { entry -> GlassMenuItem(entry, state) }
        }
    }
}

/** 渲染一条菜单项（两种浮层共用）：危险项统一红色；点击先关菜单再执行业务动作。 */
@Composable
private fun GlassMenuItem(entry: GlassMenuEntry, state: GlassMenuState) {
    GlassPopupItem(
        text = entry.label,
        icon = entry.icon,
        enabled = entry.enabled,
        onClick = {
            // 与原 popup 菜单一致：先关菜单，再执行业务动作。
            state.hide()
            entry.onClick()
        },
        contentColor = if (entry.isDanger) {
            GlassMenuDangerColor
        } else {
            MiuixTheme.colorScheme.onSurfaceContainer
        },
    )
}
