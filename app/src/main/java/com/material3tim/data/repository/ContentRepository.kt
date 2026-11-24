package com.material3tim.data.repository

import com.material3tim.data.model.FavoriteItem
import com.material3tim.data.model.FavoriteType
import com.material3tim.data.model.FileItem
import com.material3tim.data.model.FileKind
import com.material3tim.data.model.FriendRequest
import com.material3tim.data.model.FriendRequestState
import com.material3tim.data.model.OfficialAccount
import com.material3tim.data.model.ServiceEntry
import com.material3tim.data.model.ServiceGroup
import com.material3tim.data.model.StorageInfo
import com.material3tim.data.model.TagGroup
import com.material3tim.data.model.WalletData
import com.material3tim.data.model.WalletEntry
import com.material3tim.data.model.WalletService

/**
 * 我的页 / 联系人页各子页面的示例内容。第 1 步只用于把 UI 填满。
 */
object ContentRepository {

    // ===================== 我的收藏 =====================
    val favorites: List<FavoriteItem> = listOf(
        FavoriteItem(
            id = "fav1",
            type = FavoriteType.TEXT,
            title = "Compose 重组优化的几个要点",
            snippet = "1. 用 derivedStateOf 收敛高频变化；2. 避免在组合函数里创建大对象；3. 列表用 key ...",
            source = "前端技术交流",
            timeLabel = "今天 09:41",
        ),
        FavoriteItem(
            id = "fav2",
            type = FavoriteType.LINK,
            title = "Material 3 Expressive 设计指南",
            snippet = "https://m3.material.io/blog",
            source = "产品讨论群",
            timeLabel = "昨天 21:12",
        ),
        FavoriteItem(
            id = "fav3",
            type = FavoriteType.FILE,
            title = "聊天列表视觉稿_v3.sketch",
            snippet = "来自 Material3 TIM 项目组",
            source = "Material3 TIM 项目组",
            timeLabel = "今天 09:32",
            fileSize = "8.6 MB",
        ),
        FavoriteItem(
            id = "fav4",
            type = FavoriteType.IMAGE,
            title = "家里的猫",
            snippet = "妈妈发来的照片",
            source = "妈妈",
            timeLabel = "今天 09:42",
        ),
        FavoriteItem(
            id = "fav5",
            type = FavoriteType.VOICE,
            title = "语音消息",
            snippet = "李娜 · 6″",
            source = "Material3 TIM 项目组",
            timeLabel = "今天 10:02",
            durationSeconds = 6,
        ),
        FavoriteItem(
            id = "fav6",
            type = FavoriteType.TEXT,
            title = "周末聚餐地址",
            snippet = "城关区万达广场 3 楼 海底捞，周六 18:00",
            source = "大学同学群",
            timeLabel = "3月12日",
        ),
        FavoriteItem(
            id = "fav7",
            type = FavoriteType.FILE,
            title = "Material3_TIM_v0.1.0.apk",
            snippet = "来自文件传输助手",
            source = "文件传输助手",
            timeLabel = "今天 10:24",
            fileSize = "2.3 MB",
        ),
    )

    // ===================== 我的文件 =====================
    val storage = StorageInfo()

    val files: List<FileItem> = listOf(
        FileItem("f1", "Material3_TIM_v0.1.0.apk", FileKind.APK, "2.3 MB", "今天 10:24", "文件传输助手"),
        FileItem("f2", "聊天列表视觉稿_v3.sketch", FileKind.OTHER, "8.6 MB", "今天 09:32", "Material3 TIM 项目组"),
        FileItem("f3", "Q2 产品规划.pdf", FileKind.PDF, "1.4 MB", "昨天 18:03", "产品讨论群"),
        FileItem("f4", "需求文档.docx", FileKind.DOC, "312 KB", "昨天 17:41", "李娜"),
        FileItem("f5", "旅行照片_2026.zip", FileKind.ZIP, "46 MB", "3月12日", "旅行计划"),
        FileItem("f6", "演示录屏.mp4", FileKind.VIDEO, "128 MB", "3月11日", "陈晨"),
        FileItem("f7", "主题颜色参考.png", FileKind.IMAGE, "2.1 MB", "3月10日", "Material3 TIM 项目组"),
        FileItem("f8", "会议录音.m4a", FileKind.MUSIC, "12 MB", "3月9日", "周会"),
        FileItem("f9", "开发计划.xlsx", FileKind.DOC, "96 KB", "3月8日", "张伟"),
    )

    // ===================== 钱包 =====================
    val wallet = WalletData(
        balance = "0.00",
        services = listOf(
            WalletService("w1", "收付款"),
            WalletService("w2", "转账"),
            WalletService("w3", "红包"),
            WalletService("w4", "零钱"),
            WalletService("w5", "账单"),
            WalletService("w6", "卡券"),
        ),
        entries = listOf(
            WalletEntry("we1", "账单", "查看收支明细"),
            WalletEntry("we2", "银行卡", "管理已绑定的银行卡"),
            WalletEntry("we3", "支付设置", "免密支付、优先支付方式"),
            WalletEntry("we4", "安全保障", "实名认证、支付密码"),
        ),
    )

    // ===================== 服务 =====================
    val serviceGroups: List<ServiceGroup> = listOf(
        ServiceGroup(
            title = "常用",
            entries = listOf(
                ServiceEntry("s1", "QQ 钱包", "wallet"),
                ServiceEntry("s2", "会员中心", "vip"),
                ServiceEntry("s3", "游戏中心", "game"),
                ServiceEntry("s4", "TIM 邮箱", "mail"),
            ),
        ),
        ServiceGroup(
            title = "生活",
            entries = listOf(
                ServiceEntry("s5", "充值中心", "recharge"),
                ServiceEntry("s6", "电影演出", "movie"),
                ServiceEntry("s7", "出行服务", "travel"),
                ServiceEntry("s8", "生活缴费", "bill"),
            ),
        ),
        ServiceGroup(
            title = "效率",
            entries = listOf(
                ServiceEntry("s9", "腾讯文档", "doc"),
                ServiceEntry("s10", "腾讯会议", "meeting"),
                ServiceEntry("s11", "微云", "cloud"),
                ServiceEntry("s12", "更多服务", "more"),
            ),
        ),
    )

    // ===================== 新的朋友 =====================
    val friendRequests: List<FriendRequest> = listOf(
        FriendRequest("fr1", "周杰", "我是周杰，通过「手机联系人」添加", "今天 11:02", 20),
        FriendRequest("fr2", "孙宇", "我是孙宇，产品讨论群里的", "今天 09:18", 19),
        FriendRequest("fr3", "秦岚", "你好，我是秦岚", "昨天 22:40", 31, FriendRequestState.ACCEPTED),
        FriendRequest("fr4", "吕薇", "通过「群聊成员」添加", "昨天 19:12", 35),
        FriendRequest("fr5", "孔明", "我是孔明，久仰", "3月12日", 38, FriendRequestState.ACCEPTED),
    )

    // ===================== 标签 =====================
    val tags: List<TagGroup> = listOf(
        TagGroup("tag1", "家人", listOf("妈妈", "爸爸")),
        TagGroup("tag2", "同事", listOf("刘洋", "李娜", "陈晨", "赵敏")),
        TagGroup("tag3", "设计", listOf("陈晨", "冯琳")),
        TagGroup("tag4", "同学", listOf("王强", "韩雨", "何军")),
    )

    // ===================== 公众号 =====================
    val officialAccounts: List<OfficialAccount> = listOf(
        OfficialAccount(
            id = "oa1",
            name = "Material Design",
            description = "Google 官方设计系统动态",
            lastArticle = "Material 3 Expressive 正式发布",
            timeLabel = "今天 08:12",
            avatarSeed = 6,
            unreadCount = 2,
        ),
        OfficialAccount(
            id = "oa2",
            name = "腾讯 TIM",
            description = "TIM 官方账号",
            lastArticle = "TIM 4.1.0 版本更新说明",
            timeLabel = "昨天 18:00",
            avatarSeed = 5,
        ),
        OfficialAccount(
            id = "oa3",
            name = "Kotlin 开发者",
            description = "Kotlin 官方中文社区",
            lastArticle = "Kotlin 2.4 新特性一览",
            timeLabel = "3月12日",
            avatarSeed = 9,
        ),
        OfficialAccount(
            id = "oa4",
            name = "少数派",
            description = "高效工作，品质生活",
            lastArticle = "我的 Android 桌面整理术",
            timeLabel = "3月11日",
            avatarSeed = 12,
        ),
        OfficialAccount(
            id = "oa5",
            name = "LSPosed",
            description = "Xposed 框架官方账号",
            lastArticle = "LSPosed 1.10 更新公告",
            timeLabel = "3月9日",
            avatarSeed = 17,
        ),
    )
}
