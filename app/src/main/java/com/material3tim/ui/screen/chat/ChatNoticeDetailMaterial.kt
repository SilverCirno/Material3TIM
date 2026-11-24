package com.material3tim.ui.screen.chat

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.GroupNotice
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/** 群公告详情（M3E）：完整正文不截断，可复制。 */
@Composable
internal fun ChatNoticeDetailMaterial(
    notice: GroupNotice?,
    loaded: Boolean,
    onBack: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val content = notice?.content.orEmpty()
    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = notice?.title?.ifBlank { null }
                            ?: stringResource(R.string.chat_notice_list),
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
                    if (content.isNotBlank()) {
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(content))
                                Toast.makeText(context, R.string.chat_info_copied, Toast.LENGTH_SHORT)
                                    .show()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.chat_info_copied),
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
        ) {
            item(key = "notice") {
                TonalCard(containerColor = MaterialTheme.colorScheme.surfaceBright) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        val time = formatNoticeTime(notice?.timeMillis ?: 0L)
                        if (time.isNotBlank()) {
                            Text(
                                text = time,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        if (content.isNotBlank()) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val uriHandler = androidx.compose.runtime.remember(context) { openLinkHandler(context) }
                            androidx.compose.runtime.CompositionLocalProvider(
                                androidx.compose.ui.platform.LocalUriHandler provides uriHandler,
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = linkifyContent(content, MaterialTheme.colorScheme.primary),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
