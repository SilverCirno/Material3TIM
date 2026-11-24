package com.material3tim.ui.screen.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.GroupNotice
import com.material3tim.ui.component.miuix.glass.GlassInputDialog
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarIconButton
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.material3tim.data.source.AppDataSources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/** 群公告列表页（Miuix）：全部历史公告，点击卡片看详情。 */
@Composable
internal fun ChatNoticeMiuix(
    groupName: String,
    notices: List<GroupNotice>,
    loaded: Boolean,
    loading: Boolean,
    chatId: String,
    canManage: Boolean,
    onBack: () -> Unit,
    onOpenNotice: (GroupNotice) -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPublishDialog by remember { mutableStateOf(false) }
    // 公告输入内容：用于「非空才可确认」（由 GlassInputDialog.onTextChange 同步）。
    var publishText by remember { mutableStateOf("") }
    val successMessage = stringResource(R.string.group_manage_notice_success)
    val failedMessage = stringResource(R.string.group_manage_failed)
    val enableBlur = LocalEnableBlur.current
    val scrollBehavior = MiuixScrollBehavior()
    // Phase 2b：页面级 Glass 采样 backdrop（关闭模糊/无 runtime shader 时为 null → 纯色回退）。
    val backdrop = rememberPageBackdrop(enableBlur)

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
    Scaffold(
        topBar = {
            HyperTopAppBar(
                title = stringResource(R.string.chat_notice_list),
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                navigationIcon = {
                    // 圆形 Glass 按钮（玻璃模式）/ 普通 IconButton（纯色回退）。
                    HyperTopBarIconButton(
                        onClick = onBack,
                        contentDescription = null,
                        icon = MiuixIcons.Back,
                    )
                },
                actions = {
                    if (canManage) {
                        TextButton(
                            text = stringResource(R.string.group_manage_publish_notice),
                            onClick = { showPublishDialog = true },
                            colors = ButtonDefaults.textButtonColorsPrimary(),
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (notices.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = when {
                                    loading -> stringResource(R.string.loading)
                                    loaded -> stringResource(R.string.chat_info_notice_empty)
                                    else -> stringResource(R.string.chat_info_notice)
                                },
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                            if (!loading && canManage) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(
                                    text = stringResource(R.string.group_manage_publish_notice),
                                    onClick = { showPublishDialog = true },
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                )
                            }
                            if (loaded && !loading) {
                                TextButton(
                                    text = stringResource(R.string.group_files_retry),
                                    onClick = onRefresh,
                                )
                            }
                        }
                    }
                } else {
                    items(notices, key = { it.id }) { notice ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpenNotice(notice) },
                            showIndication = true,
                            insideMargin = PaddingValues(0.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = notice.title.ifBlank {
                                            stringResource(R.string.chat_notice_list)
                                        },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = MiuixIcons.ChevronForward,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                                val time = formatNoticeTime(notice.timeMillis)
                                if (time.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = time,
                                        fontSize = 11.sp,
                                        color = colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = notice.content,
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }

    // 发布群公告：HyperOS4 玻璃输入对话框（多行；常驻组合，空白不可确认）。
    // provider 在 Scaffold 处已结束，这里再包一层供玻璃对话框采样（null 时自动不透明回退）。
    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        GlassInputDialog(
            show = showPublishDialog,
            onDismissRequest = { showPublishDialog = false },
            title = stringResource(R.string.group_manage_publish_notice),
            summary = stringResource(R.string.group_manage_notice_hint),
            initialText = "",
            singleLine = false,
            maxLines = 5,
            confirmText = stringResource(R.string.confirm),
            cancelText = stringResource(R.string.cancel),
            confirmEnabled = publishText.isNotBlank(),
            onTextChange = { publishText = it },
            onConfirm = { text ->
                showPublishDialog = false
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            AppDataSources.groupManageSource
                                ?.publishGroupNotice(chatId, text.trim()) ?: false
                        }.getOrDefault(false)
                    }
                    Toast.makeText(
                        context,
                        if (ok) successMessage else failedMessage,
                        Toast.LENGTH_SHORT,
                    ).show()
                    if (ok) onRefresh()
                }
            },
        )
    }
}
