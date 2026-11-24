package com.material3tim.ui.screen.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.GroupNotice
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.material.ExpressiveInputDialog
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/** 群公告列表页（M3E）：全部历史公告，点击卡片看详情。 */
@Composable
internal fun ChatNoticeMaterial(
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
    val successMessage = stringResource(R.string.group_manage_notice_success)
    val failedMessage = stringResource(R.string.group_manage_failed)
    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.chat_notice_list),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    TopBarBackButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.chat_detail_back),
                    )
                },
                actions = {
                    if (canManage) {
                        TextButton(onClick = { showPublishDialog = true }) {
                            Text(
                                text = stringResource(R.string.group_manage_publish_notice),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!loading && canManage) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { showPublishDialog = true }) {
                                Text(text = stringResource(R.string.group_manage_publish_notice))
                            }
                        }
                        if (loaded && !loading) {
                            TextButton(onClick = onRefresh) {
                                Text(text = stringResource(R.string.group_files_retry))
                            }
                        }
                    }
                }
            } else {
                items(notices, key = { it.id }) { notice ->
                    TonalCard(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        onClick = { onOpenNotice(notice) },
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
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            val time = formatNoticeTime(notice.timeMillis)
                            if (time.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = notice.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }

    // 发布公告：Expressive 多行输入弹窗。
    ExpressiveInputDialog(
        show = showPublishDialog,
        title = stringResource(R.string.group_manage_publish_notice),
        initialText = "",
        placeholder = stringResource(R.string.group_manage_notice_hint),
        confirmText = stringResource(R.string.confirm),
        singleLine = false,
        maxLines = 5,
        minHeight = 140.dp,
        onConfirm = { noticeText ->
            showPublishDialog = false
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        AppDataSources.groupManageSource
                            ?.publishGroupNotice(chatId, noticeText.trim()) ?: false
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
        onDismissRequest = { showPublishDialog = false },
    )
}
