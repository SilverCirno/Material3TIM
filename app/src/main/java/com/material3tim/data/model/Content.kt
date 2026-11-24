package com.material3tim.data.model

import androidx.compose.runtime.Immutable

/** ===================== 我的页子页面：收藏 ===================== */

enum class FavoriteType { TEXT, LINK, IMAGE, FILE, VOICE }

@Immutable
data class FavoriteItem(
    val id: String,
    val type: FavoriteType,
    val title: String,
    val snippet: String = "",
    val source: String = "",
    val timeLabel: String = "",
    val fileSize: String = "",
    val durationSeconds: Int = 0,
)

/** ===================== 我的页子页面：我的文件 ===================== */

enum class FileKind { DOC, PDF, IMAGE, VIDEO, ZIP, APK, MUSIC, OTHER }

@Immutable
data class FileItem(
    val id: String,
    val name: String,
    val kind: FileKind,
    val size: String,
    val timeLabel: String,
    val from: String = "",
)

@Immutable
data class StorageInfo(
    val usedText: String = "2.3 GB",
    val totalText: String = "64 GB",
    val usedFraction: Float = 0.36f,
    val imageText: String = "1.1 GB",
    val videoText: String = "620 MB",
    val docText: String = "380 MB",
    val otherText: String = "200 MB",
)

/** ===================== 我的页子页面：钱包 ===================== */

@Immutable
data class WalletService(
    val id: String,
    val name: String,
)

@Immutable
data class WalletEntry(
    val id: String,
    val title: String,
    val summary: String = "",
)

@Immutable
data class WalletData(
    val balance: String = "0.00",
    val services: List<WalletService> = emptyList(),
    val entries: List<WalletEntry> = emptyList(),
)

/** ===================== 我的页子页面：服务 ===================== */

@Immutable
data class ServiceEntry(
    val id: String,
    val name: String,
    /** Material 图标名称标识，供页面映射到具体 ImageVector */
    val icon: String,
)

@Immutable
data class ServiceGroup(
    val title: String,
    val entries: List<ServiceEntry>,
)

/** 群助手（消息列表入口卡片 + 二级页数据）。 */
@Immutable
data class GroupAssistantData(
    /** 是否应在消息列表显示「群助手」卡片（TIM：主最近会话存在 chatType=7/peerUin=9994 的伪会话）。 */
    val isRecentEntry: Boolean = false,
    /** 所有收进群助手的群的未读总数。 */
    val unreadCount: Int = 0,
    /** 收进群助手的群（按最近消息时间倒序；chatId 可直接进入对应群聊）。 */
    val groups: List<ChatItem> = emptyList(),
)

/** ===================== 联系人页子页面：新的朋友 ===================== */

enum class FriendRequestState { PENDING, ACCEPTED, REJECTED }

@Immutable
data class FriendRequest(
    val id: String,
    val name: String,
    val message: String,
    val timeLabel: String,
    val avatarSeed: Int,
    val state: FriendRequestState = FriendRequestState.PENDING,
    // ---- 宿主真实数据扩展（独立 App/示例数据保持默认值即可） ----
    /** 内核 uid（同意/拒绝好友申请时回传） */
    val uid: String = "",
    /** QQ 号 */
    val uin: String = "",
    val avatarPath: String? = null,
    /** 申请时间（毫秒，排序用） */
    val timeMillis: Long = 0L,
    /** true=对方申请加我（收到）；false=我主动申请对方（发出）。 */
    val isIncoming: Boolean = true,
)

/** ===================== 联系人页子页面：群聊 / 标签 / 公众号 ===================== */

@Immutable
data class TagGroup(
    val id: String,
    val name: String,
    val memberNames: List<String>,
)

@Immutable
data class OfficialAccount(
    val id: String,
    val name: String,
    val description: String,
    val lastArticle: String,
    val timeLabel: String,
    val avatarSeed: Int,
    val unreadCount: Int = 0,
)
