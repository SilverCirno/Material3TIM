package com.material3tim.ui.component.miuix.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.glass.GlassTextFieldDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Miuix HyperOS4 玻璃弹层封装（Phase 2d）。
 *
 * 设计约定：
 * - **必须常驻组合**：用 `show` 参数控制显隐，**不要** `if (show) { GlassConfirmDialog(...) }`
 *   包裹调用（否则离场动画/预测性返回/`onDismissFinished` 都拿不到）。
 * - `backdrop` 统一取页面级 [LocalPageBackdrop]（页面未提供时为 null → Glass 组件自动用不透明
 *   `containerColor` 回退，描边/阴影保留；模糊关闭或设备不支持 runtime shader 时同样回退）。
 * - 依赖 Miuix `Scaffold` 的 `MiuixPopupHost`（现有 Miuix 页面均有 Scaffold，根 Scaffold 在
 *   `M3TimRoot`）；`renderInRootScaffold` 保持默认 true。
 * - 文案/危险色/输入校验/业务回调由调用方决定，本文件只负责玻璃外观与按钮行排版。
 */

/** 玻璃 Sheet 的默认内容内边距（与 miuix GlassBottomSheet 默认值一致：横向 12dp、底部 0dp）。 */
private val GlassSheetInsideMargin = DpSize(12.dp, 0.dp)

/**
 * 玻璃确认对话框（危险/普通确认共用）。
 *
 * @param show 是否显示（常驻组合，靠此参数控制显隐）
 * @param onDismissRequest 点击外部 / 返回手势请求关闭
 * @param title 标题
 * @param confirmText 确认按钮文案
 * @param cancelText 取消按钮文案
 * @param onConfirm 确认回调（业务动作由调用方执行；调用方负责把自己的 show 状态置 false）
 * @param summary 可选说明文字
 * @param destructive true 时确认按钮用 `Destructive` 语义色（删除/踢人/清空等）
 * @param onDismissFinished 关闭动画完成回调（可选）
 */
@Composable
fun GlassConfirmDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    summary: String? = null,
    destructive: Boolean = false,
    onDismissFinished: (() -> Unit)? = null,
) {
    GlassDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        backdrop = LocalPageBackdrop.current,
        title = title,
        summary = summary,
        onDismissFinished = onDismissFinished,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassDialogButton(
                onClick = onDismissRequest,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = cancelText, style = MiuixTheme.textStyles.button)
            }
            GlassDialogButton(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                role = if (destructive) {
                    GlassDialogButtonRole.Destructive
                } else {
                    GlassDialogButtonRole.Primary
                },
            ) {
                Text(text = confirmText, style = MiuixTheme.textStyles.button)
            }
        }
    }
}

/**
 * 玻璃输入对话框（单行/多行文本）。
 *
 * 打开（show 变为 true）时会把输入框内容重置为 [initialText]，与旧实现
 * `var text by remember(show) { mutableStateOf(...) }` 的语义一致。
 *
 * @param show 是否显示（常驻组合，靠此参数控制显隐）
 * @param onDismissRequest 点击外部 / 返回手势请求关闭
 * @param title 标题
 * @param confirmText 确认按钮文案
 * @param cancelText 取消按钮文案
 * @param onConfirm 确认回调，参数为当前输入文本（**未 trim**，由调用方决定是否 trim/校验）
 * @param initialText 打开时的初始文本
 * @param placeholder 占位提示（会作为 TextField 的 label/placeholder 显示）
 * @param summary 可选说明文字
 * @param singleLine 是否单行
 * @param maxLines 多行时的最大行数（singleLine=true 时忽略）
 * @param confirmEnabled 确认按钮是否可点击（需要「非空才能确认」时，调用方配合 [onTextChange] 维护）
 * @param onTextChange 文本变化回调（含打开时重置为 initialText 的那次；用于外部维护 confirmEnabled 等）
 * @param onDismissFinished 关闭动画完成回调（可选）
 */
@Composable
fun GlassInputDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    cancelText: String,
    onConfirm: (String) -> Unit,
    initialText: String = "",
    placeholder: String? = null,
    summary: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    confirmEnabled: Boolean = true,
    onTextChange: (String) -> Unit = {},
    onDismissFinished: (() -> Unit)? = null,
) {
    // 常驻组合：文本状态保存在这里，每次打开（show: false → true）重置为 initialText。
    var text by remember { mutableStateOf(initialText) }
    LaunchedEffect(show) {
        if (show) {
            text = initialText
            onTextChange(initialText)
        }
    }
    GlassDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        backdrop = LocalPageBackdrop.current,
        title = title,
        summary = summary,
        onDismissFinished = onDismissFinished,
    ) {
        TextField(
            value = text,
            onValueChange = {
                text = it
                onTextChange(it)
            },
            // 玻璃表面上使用高对比度输入框配色（与 GlassTextField 默认一致）。
            colors = GlassTextFieldDefaults.textFieldColors(),
            label = placeholder.orEmpty(),
            useLabelAsPlaceholder = placeholder != null,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else maxLines,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassDialogButton(
                onClick = onDismissRequest,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = cancelText, style = MiuixTheme.textStyles.button)
            }
            GlassDialogButton(
                onClick = { onConfirm(text) },
                modifier = Modifier.weight(1f),
                enabled = confirmEnabled,
                role = GlassDialogButtonRole.Primary,
            ) {
                Text(text = confirmText, style = MiuixTheme.textStyles.button)
            }
        }
    }
}

/**
 * 玻璃底部 Sheet 封装（简单透传 `GlassBottomSheet`，内容区提供 `ColumnScope`）。
 *
 * 保留 `GlassBottomSheet` 自带能力：拖拽/外部点击/预测性返回关闭、遮罩动画、IME 与系统栏内边距、
 * 嵌套滚动（[enableNestedScroll]）；内容滚动、导航栏留白仍由调用方按需添加。
 *
 * @param show 是否显示（常驻组合，靠此参数控制显隐）
 * @param onDismissRequest 拖拽关闭 / 外部点击 / 返回手势请求关闭
 * @param title 可选标题（null 时不占标题栏，仅保留拖拽手柄）
 * @param backdrop 背景采样；null 时自动取页面 [LocalPageBackdrop]（页面未提供则 Glass 不透明回退）
 * @param insideMargin 内容内边距（默认 Glass 玻璃 Sheet 默认值；迁移旧 OverlayBottomSheet 时可传旧值保持布局）
 * @param enableNestedScroll 是否允许内容滚动传递给 Sheet 拖拽
 * @param content 内容区（已提供 ColumnScope，可直接用 `Modifier.weight` 等）
 */
@Composable
fun GlassSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String? = null,
    backdrop: Backdrop? = LocalPageBackdrop.current,
    insideMargin: DpSize = GlassSheetInsideMargin,
    enableNestedScroll: Boolean = true,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        backdrop = backdrop,
        title = title,
        insideMargin = insideMargin,
        enableNestedScroll = enableNestedScroll,
        onDismissFinished = onDismissFinished,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}
