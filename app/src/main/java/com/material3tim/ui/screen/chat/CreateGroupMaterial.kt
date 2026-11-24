package com.material3tim.ui.screen.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.ContactItem
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SegmentedItem
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/**
 * 创建群聊选人页（Material 3 Expressive 风格）：
 * 顶栏返回 + 标题 + 右侧「确认(N)」（未选中时禁用）；
 * 联系人列表复用联系人页行样式（SegmentedListItem），点整行切换选中，行尾 Checkbox 只展示状态。
 */
@Composable
internal fun CreateGroupMaterial(
    contacts: List<ContactItem>,
    loading: Boolean,
    selectedIds: Set<String>,
    creating: Boolean,
    onToggle: (ContactItem) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    val canConfirm = selectedIds.isNotEmpty() && !creating
    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_plus_create_group),
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
                    TextButton(onClick = onConfirm, enabled = canConfirm) {
                        Text(stringResource(R.string.create_group_confirm_count, selectedIds.size))
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
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            loading -> {
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            contacts.isEmpty() -> {
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.create_group_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = contentModifier,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(contacts, key = { _, contact -> contact.id }) { index, contact ->
                        SegmentedItem(index = index, count = contacts.size) {
                            CreateGroupContactRowMaterial(
                                contact = contact,
                                checked = contact.id in selectedIds,
                                onToggle = { onToggle(contact) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 联系人选择行：头像 + 昵称（+签名）+ 行尾 Checkbox，整行点击切换选中。 */
@Composable
private fun CreateGroupContactRowMaterial(
    contact: ContactItem,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    SegmentedListItem(
        onClick = onToggle,
        leadingContent = {
            ChatAvatar(
                name = contact.name,
                seed = contact.avatarSeed,
                size = 44.dp,
                isOnline = contact.isOnline,
                avatarPath = contact.avatarPath,
            )
        },
        headlineContent = {
            Text(
                text = contact.name,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = if (contact.signature.isNotBlank()) {
            {
                Text(
                    text = contact.signature,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            null
        },
        trailingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
        },
    )
}
