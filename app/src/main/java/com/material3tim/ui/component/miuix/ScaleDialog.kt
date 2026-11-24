package com.material3tim.ui.component.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.glass.GlassTextFieldDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 页面缩放输入对话框（80% - 110%）。
 *
 * HyperOS4：`OverlayDialog` → miuix-glass `GlassDialog`（常驻组合，`show` 控制显隐；
 * backdrop 取页面 `LocalPageBackdrop`，页面未提供时自动不透明回退）。
 * 输入框保留原数字校验与「%」后缀；按钮改用 `GlassDialogButton` 适配玻璃表面。
 */
@Composable
fun ScaleDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    volumeState: () -> Float,
    onVolumeChange: (Float) -> Unit,
) {
    GlassDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        backdrop = LocalPageBackdrop.current,
        title = stringResource(R.string.settings_page_scale),
        summary = "80% - 110%",
    ) {
        // 每次打开重置输入（沿用原 remember(show) 语义）。
        var text by remember(show) {
            mutableStateOf((volumeState() * 100).toInt().toString())
        }
        TextField(
            modifier = Modifier.padding(bottom = 16.dp),
            value = text,
            maxLines = 1,
            // 玻璃表面上使用高对比度输入框配色（与 GlassTextField 默认一致）。
            colors = GlassTextFieldDefaults.textFieldColors(),
            trailingIcon = {
                Text(
                    text = "%",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = colorScheme.onSurfaceVariantActions,
                )
            },
            onValueChange = { newValue ->
                if (newValue.isEmpty()) {
                    text = ""
                } else {
                    val valid = newValue.all { it.isDigit() }
                    if (valid) {
                        text = newValue
                    }
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassDialogButton(
                onClick = onDismissRequest,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    style = MiuixTheme.textStyles.button,
                )
            }
            GlassDialogButton(
                onClick = {
                    val parsed = text.toIntOrNull()
                    val clamped = parsed?.coerceIn(80, 110) ?: (volumeState() * 100).toInt()
                    onVolumeChange(clamped / 100f)
                    onDismissRequest()
                },
                modifier = Modifier.weight(1f),
                role = GlassDialogButtonRole.Primary,
            ) {
                Text(
                    text = stringResource(android.R.string.ok),
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
    }
}
