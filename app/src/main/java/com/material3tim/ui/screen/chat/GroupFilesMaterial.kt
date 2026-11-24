package com.material3tim.ui.screen.chat

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.GroupFileEntry
import com.material3tim.ui.component.material.ExpressiveConfirmDialog
import com.material3tim.ui.component.material.ExpressiveInputDialog
import com.material3tim.ui.component.material.ExpressiveListDialog
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SegmentedColumn
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/**
 * 群文件页（Material 3 Expressive）：目录浏览 + 下载进度 + 管理模式（上传/新建/重命名/移动/删除）。
 */
@Composable
internal fun GroupFilesMaterial(
    state: GroupFilesState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPickUpload: () -> Unit,
) {
    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.group_files_title),
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
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.group_files_refresh),
                        )
                    }
                    if (state.canManage) {
                        if (state.manageMode) {
                            IconButton(onClick = onPickUpload) {
                                Icon(
                                    imageVector = Icons.Filled.UploadFile,
                                    contentDescription = stringResource(R.string.group_files_upload),
                                )
                            }
                            IconButton(onClick = state::showCreateFolder) {
                                Icon(
                                    imageVector = Icons.Filled.CreateNewFolder,
                                    contentDescription = stringResource(R.string.group_files_new_folder),
                                )
                            }
                            IconButton(onClick = state::toggleManageMode) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.group_files_done),
                                )
                            }
                        } else {
                            TextButton(onClick = state::toggleManageMode) {
                                Text(text = stringResource(R.string.group_files_manage))
                            }
                        }
                    }
                },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
            )
        },
        bottomBar = {
            if (state.manageMode) {
                GroupFilesManageBarMaterial(state = state)
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            GroupFilesBreadcrumbMaterial(state = state, onBack = onBack)
            Box(modifier = Modifier.weight(1f)) {
                when {
                    !state.sourceAvailable -> GroupFilesEmptyText(
                        text = stringResource(R.string.group_files_host_only),
                    )
                    state.loading && state.entries.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    state.error != null && state.entries.isEmpty() -> GroupFilesErrorMaterial(
                        message = state.error.orEmpty(),
                        onRetry = onRefresh,
                    )
                    state.entries.isEmpty() -> GroupFilesEmptyText(
                        text = stringResource(R.string.group_files_empty),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.folders.isNotEmpty()) {
                            item(key = "section_folders") {
                                SegmentedColumn(
                                    title = stringResource(R.string.group_files_section_folders),
                                ) {
                                    state.folders.forEach { entry ->
                                        item(key = "folder_${state.entryKey(entry)}") {
                                            GroupFilesFolderRowMaterial(state = state, entry = entry)
                                        }
                                    }
                                }
                            }
                        }
                        if (state.files.isNotEmpty()) {
                            item(key = "section_files") {
                                SegmentedColumn(
                                    title = stringResource(R.string.group_files_section_files),
                                ) {
                                    state.files.forEach { entry ->
                                        item(key = "file_${state.entryKey(entry)}") {
                                            GroupFilesFileRowMaterial(state = state, entry = entry)
                                        }
                                    }
                                }
                            }
                        }
                        if (!state.isEnd) {
                            item(key = "load_more") {
                                LaunchedEffect(state.entries.size) {
                                    state.loadMore()
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (state.loadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                        if (state.error != null && state.entries.isNotEmpty()) {
                            item(key = "load_more_error") {
                                Text(
                                    text = state.error.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    GroupFilesDialogsMaterial(state = state)
}

@Composable
private fun GroupFilesBreadcrumbMaterial(state: GroupFilesState, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.breadcrumb,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (state.canGoUp) {
                        Modifier.clickable { state.goUp() }
                    } else {
                        Modifier
                    },
                ),
        )
        Text(
            text = stringResource(R.string.group_files_folder_count, state.allFileCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupFilesEmptyText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupFilesErrorMaterial(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.group_files_retry))
        }
    }
}

@Composable
private fun GroupFilesFolderRowMaterial(state: GroupFilesState, entry: GroupFileEntry) {
    val leading: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    val headline: @Composable () -> Unit = {
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val supporting: (@Composable () -> Unit)? = if (entry.fileCount > 0) {
        {
            Text(
                text = stringResource(R.string.group_files_folder_count, entry.fileCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        null
    }
    val trailing: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
    if (state.manageMode) {
        SegmentedListItem(
            checked = state.isSelected(entry),
            onCheckedChange = { state.toggleSelect(entry) },
            leadingContent = leading,
            headlineContent = headline,
            supportingContent = supporting,
            trailingContent = trailing,
        )
    } else {
        SegmentedListItem(
            onClick = { state.openFolder(entry) },
            leadingContent = leading,
            headlineContent = headline,
            supportingContent = supporting,
            trailingContent = trailing,
        )
    }
}

@Composable
private fun GroupFilesFileRowMaterial(state: GroupFilesState, entry: GroupFileEntry) {
    val colorScheme = MaterialTheme.colorScheme
    val download = state.downloads[entry.fileId]
    val downloading = download != null && !download.done && !download.failed
    val onClick: () -> Unit = {
        when {
            state.manageMode -> state.toggleSelect(entry)
            entry.localPath != null -> state.openFile(entry)
            else -> state.download(entry)
        }
    }
    val leading: @Composable () -> Unit = {
        Icon(
            imageVector = groupFileKindIcon(entry),
            contentDescription = null,
            tint = colorScheme.primary,
        )
    }
    val headline: @Composable () -> Unit = {
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val meta = groupFileMetaText(entry)
    val supporting: (@Composable () -> Unit)? = if (meta.isNotBlank()) {
        {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        null
    }
    val trailing: (@Composable () -> Unit)? = when {
        state.manageMode -> null
        entry.localPath != null -> {
            {
                TextButton(onClick = { state.openFile(entry) }) {
                    Text(text = stringResource(R.string.group_files_open))
                }
            }
        }
        downloading -> {
            {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.width(64.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { download.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${download.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        else -> {
            {
                IconButton(onClick = { state.download(entry) }) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = stringResource(R.string.group_files_download),
                        tint = colorScheme.primary,
                    )
                }
            }
        }
    }
    if (state.manageMode) {
        SegmentedListItem(
            checked = state.isSelected(entry),
            onCheckedChange = { state.toggleSelect(entry) },
            leadingContent = leading,
            headlineContent = headline,
            supportingContent = supporting,
            trailingContent = trailing,
        )
    } else {
        SegmentedListItem(
            onClick = onClick,
            leadingContent = leading,
            headlineContent = headline,
            supportingContent = supporting,
            trailingContent = trailing,
        )
    }
}

@Composable
private fun GroupFilesManageBarMaterial(state: GroupFilesState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.group_files_selected_count, state.selectedKeys.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = state::showDelete,
                enabled = state.selectedKeys.isNotEmpty(),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(2.dp))
                Text(text = stringResource(R.string.group_files_delete))
            }
            TextButton(
                onClick = state::showMove,
                enabled = state.selectedEntries.isNotEmpty() && state.selectedEntries.none { it.isFolder },
            ) {
                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(2.dp))
                Text(text = stringResource(R.string.group_files_move))
            }
            TextButton(
                onClick = state::showRename,
                enabled = state.selectedEntries.size == 1,
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(2.dp))
                Text(text = stringResource(R.string.group_files_rename))
            }
        }
    }
}

@Composable
private fun GroupFilesDialogsMaterial(state: GroupFilesState) {
    when (val dialog = state.dialog) {
        null -> Unit
        GroupFilesDialog.CreateFolder -> GroupFilesTextInputDialogMaterial(
            title = stringResource(R.string.group_files_new_folder),
            placeholder = stringResource(R.string.group_files_folder_hint),
            initial = "",
            onConfirm = state::createFolder,
            onDismiss = state::dismissDialog,
        )
        is GroupFilesDialog.Rename -> GroupFilesTextInputDialogMaterial(
            title = stringResource(R.string.group_files_rename),
            placeholder = stringResource(R.string.group_files_rename_hint),
            initial = dialog.entry.name,
            onConfirm = { name -> state.rename(dialog.entry, name) },
            onDismiss = state::dismissDialog,
        )
        is GroupFilesDialog.Move -> GroupFilesMoveDialogMaterial(
            targets = state.moveTargets,
            onSelect = { folderId -> state.move(dialog.entries, folderId) },
            onDismiss = state::dismissDialog,
        )
        is GroupFilesDialog.Delete -> ExpressiveConfirmDialog(
            show = true,
            title = stringResource(R.string.group_files_delete_title),
            message = stringResource(R.string.group_files_delete_message),
            confirmText = stringResource(R.string.group_files_delete),
            danger = true,
            onConfirm = { state.delete(dialog.entries) },
            onDismissRequest = state::dismissDialog,
        )
    }
}

@Composable
private fun GroupFilesTextInputDialogMaterial(
    title: String,
    placeholder: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // 新建文件夹/重命名：Expressive 输入弹窗。
    ExpressiveInputDialog(
        show = true,
        title = title,
        initialText = initial,
        placeholder = placeholder,
        confirmText = stringResource(R.string.confirm),
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
    )
}
@Composable
private fun GroupFilesMoveDialogMaterial(
    targets: List<GroupFilesFolder>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // 移动文件：Expressive 列表选择弹窗（点击目标即执行原 move 逻辑）。
    ExpressiveListDialog(
        show = true,
        title = stringResource(R.string.group_files_move_title),
        options = targets.map { it.name },
        onSelect = { index -> targets.getOrNull(index)?.let { onSelect(it.id) } },
        onDismissRequest = onDismiss,
    )
}