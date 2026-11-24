package com.material3tim.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 扫一扫页（Miuix 风格）：
 * 默认三按钮操作页：全宽「打开相机」，下方并排「选择图片」「取消」。
 * 解码中显示 loading（按钮禁用）；失败时在按钮上方显示原因，若已识别出文本则展示内容 + 「复制内容」。
 *
 * Phase 2e：HyperOS4 玻璃/纯色自适应顶栏 + 页面级 backdrop（页面结构/操作不变）。
 */
@Composable
internal fun ScanQrMiuix(
    state: ScanQrState,
    onOpenCamera: () -> Unit,
    onPickImage: () -> Unit,
    onCancel: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val enableBlur = LocalEnableBlur.current
    val scrollBehavior = MiuixScrollBehavior()
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/设备不支持 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)
    val layoutDirection = LocalLayoutDirection.current
    val working = state is ScanQrState.Working

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.scan_title),
                    scrollBehavior = scrollBehavior,
                    backdrop = backdrop,
                    navigationIcon = {
                        // 顶栏按钮跟随顶栏风格：HyperTopBarGlassButton 自动选择玻璃圆钮 / 普通 IconButton。
                        HyperTopBarGlassButton(onClick = onCancel) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.chat_detail_back),
                                tint = colorScheme.onSurface,
                            )
                        }
                    },
                )
            },
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 采样层要早于 inset/padding：记录不透明底 + 页面内容（不含玻璃顶栏自身）。
                    .then(
                        if (backdrop != null) {
                            Modifier
                                .layerBackdrop(backdrop)
                                .background(colorScheme.surface)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = innerPadding.calculateStartPadding(layoutDirection) + 24.dp,
                            end = innerPadding.calculateEndPadding(layoutDirection) + 24.dp,
                            top = innerPadding.calculateTopPadding() + 24.dp,
                            bottom = 24.dp,
                        ),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        insideMargin = PaddingValues(20.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            when (state) {
                                ScanQrState.Working -> {
                                    CircularProgressIndicator(color = colorScheme.primary)
                                    Text(
                                        text = stringResource(R.string.scan_decoding),
                                        fontSize = 14.sp,
                                        color = colorScheme.onSurfaceVariantSummary,
                                    )
                                }

                                is ScanQrState.Idle -> {
                                    state.errorKind?.let { kind ->
                                        Text(
                                            text = scanQrErrorTitle(kind),
                                            fontSize = 14.sp,
                                            color = colorScheme.error,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                    if (state.errorContent.isNotBlank()) {
                                        Text(
                                            text = stringResource(R.string.scan_result_label),
                                            fontSize = 12.sp,
                                            color = colorScheme.primary,
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 120.dp)
                                                .verticalScroll(rememberScrollState()),
                                        ) {
                                            Text(
                                                text = state.errorContent,
                                                fontSize = 14.sp,
                                                color = colorScheme.onSurface,
                                            )
                                        }
                                        TextButton(
                                            text = stringResource(R.string.scan_copy),
                                            onClick = { onCopy(state.errorContent) },
                                        )
                                    }
                                }
                            }
                            // 第一行：全宽「打开相机」（配色与「选择图片」一致）。
                            TextButton(
                                text = stringResource(R.string.scan_open_camera),
                                onClick = onOpenCamera,
                                enabled = !working,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                            )
                            // 第二行：「选择图片」在左、「取消」在右。
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                TextButton(
                                    text = stringResource(R.string.scan_pick_image),
                                    onClick = onPickImage,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    enabled = !working,
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                )
                                TextButton(
                                    text = stringResource(R.string.scan_cancel),
                                    onClick = onCancel,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    enabled = !working,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
