package com.material3tim.ui.screen.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/**
 * 扫一扫页（Material 3 Expressive 风格）：
 * 默认三按钮操作页：全宽「打开相机」，下方并排「选择图片」「取消」。
 * 解码中显示 loading（按钮禁用）；失败时在按钮上方显示原因，若已识别出文本则展示内容 + 「复制内容」。
 */
@Composable
internal fun ScanQrMaterial(
    state: ScanQrState,
    onOpenCamera: () -> Unit,
    onPickImage: () -> Unit,
    onCancel: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val working = state is ScanQrState.Working
    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.scan_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    TopBarBackButton(
                        onClick = onCancel,
                        contentDescription = stringResource(R.string.chat_detail_back),
                    )
                },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                when (state) {
                    ScanQrState.Working -> {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.scan_decoding),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    is ScanQrState.Idle -> {
                        state.errorKind?.let { kind ->
                            Text(
                                text = scanQrErrorTitle(kind),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (state.errorContent.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.scan_result_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = state.errorContent,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            TextButton(onClick = { onCopy(state.errorContent) }) {
                                Text(stringResource(R.string.scan_copy))
                            }
                        }
                    }
                }
                // 第一行：全宽主按钮「打开相机」。
                Button(
                    onClick = onOpenCamera,
                    enabled = !working,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(stringResource(R.string.scan_open_camera))
                }
                // 第二行：「选择图片」在左、「取消」在右。
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onPickImage,
                        enabled = !working,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(stringResource(R.string.scan_pick_image))
                    }
                    FilledTonalButton(
                        onClick = onCancel,
                        enabled = !working,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(stringResource(R.string.scan_cancel))
                    }
                }
            }
            }
        }
    }
}
