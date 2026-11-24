package com.material3tim.data.model

/**
 * 群文件 / 群文件夹条目（TIM NT kernel `GroupItem` 的 UI 投影）。
 *
 * - 文件：`isFolder=false`，用 [fileId]/[fileModelId]/[md5]/[fileSize] 下载；
 * - 文件夹：`isFolder=true`，用 [folderId] 作为下一次 listGroupFiles 的 parentFolderId。
 */
data class GroupFileEntry(
    /** 文件 id（文件必填；文件夹为空） */
    val fileId: String = "",
    /** 所属文件夹 id，根目录为 "" */
    val parentFolderId: String = "",
    /** 文件夹 id（isFolder=true 时有值） */
    val folderId: String = "",
    val name: String = "",
    val isFolder: Boolean = false,
    val fileSize: Long = 0L,
    val uploaderName: String = "",
    /** 上传时间（秒级时间戳；0 表示未知） */
    val uploadTime: Long = 0L,
    val downloadTimes: Int = 0,
    /** 已下载的本地路径（可空） */
    val localPath: String? = null,
    val md5: String = "",
    val fileModelId: Long = 0L,
    /** busId（删除/重命名/移动时需要） */
    val busId: Int = 0,
    /** 文件夹内文件数（isFolder=true 时可能有值） */
    val fileCount: Int = 0,
    /** 过期时间（秒级，0 表示不过期/未知） */
    val deadTime: Int = 0,
)

/** 群文件列表分页结果。 */
data class GroupFilePage(
    val entries: List<GroupFileEntry> = emptyList(),
    val isEnd: Boolean = true,
    val nextIndex: Int = 0,
    val allFileCount: Int = 0,
    val error: String? = null,
)

/** 群文件下载进度（0..100）。 */
data class GroupFileDownload(
    val fileId: String = "",
    val progress: Int = 0,
    val path: String? = null,
    val done: Boolean = false,
    val failed: Boolean = false,
    val message: String? = null,
)
