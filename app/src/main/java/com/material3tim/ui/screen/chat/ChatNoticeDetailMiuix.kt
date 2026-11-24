package com.material3tim.ui.screen.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.GroupNotice
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 群公告详情（Miuix）：完整正文不截断，可复制。
 * Phase 2e：HyperOS4 玻璃/纯色自适应顶栏 + 页面级 backdrop。
 */
@Composable
internal fun ChatNoticeDetailMiuix(
    notice: GroupNotice?,
    loaded: Boolean,
    onBack: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val enableBlur = LocalEnableBlur.current
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/设备不支持 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)
    val content = notice?.content.orEmpty()

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = notice?.title?.ifBlank { null }
                        ?: stringResource(R.string.chat_notice_list),
                    scrollBehavior = scrollBehavior,
                    backdrop = backdrop,
                    isContentScrolled = listState.canScrollBackward,
                    navigationIcon = {
                        // 顶栏按钮跟随顶栏风格：HyperTopBarGlassButton 自动选择玻璃圆钮 / 普通 IconButton。
                        HyperTopBarGlassButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onSurface,
                            )
                        }
                    },
                    actions = {
                        if (content.isNotBlank()) {
                            val onCopy: () -> Unit = {
                                clipboard.setText(AnnotatedString(content))
                                Toast.makeText(context, R.string.chat_info_copied, Toast.LENGTH_SHORT)
                                    .show()
                            }
                            HyperTopBarGlassButton(onClick = onCopy) {
                                Icon(
                                    imageVector = MiuixIcons.Copy,
                                    contentDescription = null,
                                    tint = colorScheme.onSurface,
                                )
                            }
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = 24.dp,
                    ),
                ) {
                    item(key = "notice") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            insideMargin = PaddingValues(0.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                val time = formatNoticeTime(notice?.timeMillis ?: 0L)
                                if (time.isNotBlank()) {
                                    Text(
                                        text = time,
                                        fontSize = 11.sp,
                                        color = colorScheme.onSurfaceVariantSummary,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                if (content.isNotBlank()) {
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    val uriHandler = remember(context) { openLinkHandler(context) }
                                    CompositionLocalProvider(
                                        LocalUriHandler provides uriHandler,
                                    ) {
                                        SelectionContainer {
                                            Text(
                                                text = linkifyContent(content, colorScheme.primary),
                                                fontSize = 14.sp,
                                                color = colorScheme.onSurface,
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = stringResource(
                                                if (loaded) R.string.chat_info_notice_empty else R.string.chat_info_notice,
                                            ),
                                            fontSize = 14.sp,
                                            color = colorScheme.onSurfaceVariantSummary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
