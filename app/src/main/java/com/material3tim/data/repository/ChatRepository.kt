package com.material3tim.data.repository

import com.material3tim.data.model.ChatInfoData
import com.material3tim.data.model.ChatItem
import com.material3tim.data.model.ChatMember
import com.material3tim.data.model.ChatMessage
import com.material3tim.data.model.ChatMessageType
import com.material3tim.data.model.ContactDetail
import com.material3tim.data.model.ContactItem
import com.material3tim.data.model.UserProfile

/**
 * 第一阶段：用实例数据填充消息 / 联系人 / 我的 页面。
 * 后续接入 TIM 数据后，这里会换成真正的数据源。
 */
object ChatRepository {

    private val sampleProfile = UserProfile(
        name = "Linx",
        timId = "314159265",
        signature = "这个人很懒，什么都没有留下",
        avatarSeed = 3,
    )

    /** 当前账号资料：宿主里优先真实资料，独立 App 用示例数据。 */
    val profile: UserProfile
        get() = LiveProfile.peek() ?: sampleProfile

    val sampleChats: List<ChatItem> = listOf(
        ChatItem(
            id = "chat_file_helper",
            name = "文件传输助手",
            lastMessage = "Material3_TIM_v0.1.0.apk",
            timeLabel = "10:24",
            isPinned = true,
            avatarSeed = 1,
        ),
        ChatItem(
            id = "chat_project",
            name = "Material3 TIM 项目组",
            lastMessage = "界面风格切换的动画已经修好了，可以再看看",
            timeLabel = "10:08",
            unreadCount = 12,
            isPinned = true,
            isGroup = true,
            memberCount = 8,
            avatarSeed = 5,
        ),
        ChatItem(
            id = "chat_mom",
            name = "妈妈",
            lastMessage = "晚上回家吃饭吗？",
            timeLabel = "09:47",
            unreadCount = 2,
            isPinned = true,
            avatarSeed = 8,
            isOnline = true,
        ),
        ChatItem(
            id = "chat_product",
            name = "产品讨论群",
            lastMessage = "陈晨：新版聊天列表的视觉稿放在群文件里了",
            timeLabel = "09:12",
            unreadCount = 36,
            isGroup = true,
            memberCount = 26,
            avatarSeed = 2,
        ),
        ChatItem(
            id = "chat_zhangwei",
            name = "张伟",
            lastMessage = "那个 Compose 的悬浮底栏效果确实好看",
            timeLabel = "08:56",
            unreadCount = 1,
            avatarSeed = 4,
            isOnline = true,
        ),
        ChatItem(
            id = "chat_tim_team",
            name = "TIM 团队",
            lastMessage = "【公告】4.1.0 版本更新说明",
            timeLabel = "昨天",
            isGroup = true,
            memberCount = 128,
            isMuted = true,
            avatarSeed = 6,
        ),
        ChatItem(
            id = "chat_lina",
            name = "李娜",
            lastMessage = "文档我改完了，你抽空看下～",
            timeLabel = "昨天",
            draft = "好的，我马上",
            avatarSeed = 9,
        ),
        ChatItem(
            id = "chat_college",
            name = "大学同学群",
            lastMessage = "王强：[图片]",
            timeLabel = "昨天",
            unreadCount = 99,
            isGroup = true,
            memberCount = 56,
            isMuted = true,
            avatarSeed = 11,
        ),
        ChatItem(
            id = "chat_wangqiang",
            name = "王强",
            lastMessage = "周末打球不？",
            timeLabel = "前天",
            avatarSeed = 7,
        ),
        ChatItem(
            id = "chat_frontend",
            name = "前端技术交流",
            lastMessage = "刘洋：Compose 的 recomposition 计数可以用 Layout Inspector 看",
            timeLabel = "前天",
            isGroup = true,
            memberCount = 342,
            isMuted = true,
            avatarSeed = 10,
        ),
        ChatItem(
            id = "chat_liuyang",
            name = "刘洋",
            lastMessage = "这段代码你帮我 review 一下？",
            timeLabel = "前天",
            isOnline = true,
            avatarSeed = 12,
        ),
        ChatItem(
            id = "chat_family",
            name = "家人群",
            lastMessage = "爸：周末都回来吃饭",
            timeLabel = "3月12日",
            isGroup = true,
            memberCount = 6,
            avatarSeed = 13,
        ),
        ChatItem(
            id = "chat_system",
            name = "系统通知",
            lastMessage = "你的账号于 3 月 12 日在新设备登录",
            timeLabel = "3月12日",
            isMuted = true,
            avatarSeed = 0,
        ),
        ChatItem(
            id = "chat_zhaomin",
            name = "赵敏",
            lastMessage = "收到，谢谢！",
            timeLabel = "3月11日",
            avatarSeed = 14,
        ),
        ChatItem(
            id = "chat_chenchen",
            name = "陈晨",
            lastMessage = "新的图标已经切好图了，在共享盘",
            timeLabel = "3月11日",
            avatarSeed = 15,
            isOnline = true,
        ),
        ChatItem(
            id = "chat_reading",
            name = "读书会",
            lastMessage = "本周共读：《设计中的设计》第三章",
            timeLabel = "3月10日",
            isGroup = true,
            memberCount = 48,
            isMuted = true,
            avatarSeed = 16,
        ),
        ChatItem(
            id = "chat_travel",
            name = "旅行计划",
            lastMessage = "酒店订好了，四晚海景房",
            timeLabel = "3月9日",
            isGroup = true,
            memberCount = 4,
            avatarSeed = 17,
        ),
        ChatItem(
            id = "chat_helper",
            name = "TIM 客服",
            lastMessage = "感谢您的反馈，我们会尽快处理",
            timeLabel = "3月8日",
            isMuted = true,
            avatarSeed = 18,
        ),
    )

    val sampleContacts: List<ContactItem> = listOf(
        ContactItem("c_mom", "妈妈", 'M', "晚上早点回家", 8, isOnline = true),
        ContactItem("c_baba", "爸爸", 'B', "周末一起吃饭", 13),
        ContactItem("c_zhangwei", "张伟", 'Z', "Compose 真好用", 4, isOnline = true),
        ContactItem("c_lina", "李娜", 'L', "正在搬砖", 9),
        ContactItem("c_wangqiang", "王强", 'W', "球场见", 7),
        ContactItem("c_liuyang", "刘洋", 'L', "review 一下", 12, isOnline = true),
        ContactItem("c_zhaomin", "赵敏", 'Z', "", 14),
        ContactItem("c_chenchen", "陈晨", 'C', "图标切图中", 15, isOnline = true),
        ContactItem("c_sunyu", "孙宇", 'S', "", 19),
        ContactItem("c_zhoujie", "周杰", 'Z', "音乐使我快乐", 20, isVip = true),
        ContactItem("c_wuhao", "吴昊", 'W', "", 21),
        ContactItem("c_zhengshuang", "郑爽", 'Z', "", 22),
        ContactItem("c_fenglin", "冯琳", 'F', "", 23),
        ContactItem("c_chuyang", "褚阳", 'C', "", 24),
        ContactItem("c_weimin", "卫民", 'W', "", 25),
        ContactItem("c_jiangli", "蒋丽", 'J', "", 26),
        ContactItem("c_shenyu", "沈宇", 'S', "", 27),
        ContactItem("c_hanyu", "韩雨", 'H', "", 28),
        ContactItem("c_yangyang", "杨洋", 'Y', "", 29),
        ContactItem("c_zhuxi", "朱曦", 'Z', "", 30),
        ContactItem("c_qinlan", "秦岚", 'Q', "", 31),
        ContactItem("c_youwei", "尤伟", 'Y', "", 32),
        ContactItem("c_xuchang", "许昌", 'X', "", 33),
        ContactItem("c_hejun", "何军", 'H', "", 34),
        ContactItem("c_lvwei", "吕薇", 'L', "", 35),
        ContactItem("c_shiyu", "施宇", 'S', "", 36),
        ContactItem("c_zhanghao", "张浩", 'Z', "", 37),
        ContactItem("c_kongming", "孔明", 'K', "", 38),
        ContactItem("c_caocao", "曹操", 'C', "对酒当歌", 39),
        ContactItem("c_linx", "林夕", 'L', "", 40),
    )

    /** 联系人详情页点「发消息」时按需合成的会话（保证返回后可再次按 id 查到）。 */
    private val syntheticChats = mutableMapOf<String, ChatItem>()

    fun chatById(id: String): ChatItem? =
        LiveChats.chatById(id)
            ?: sampleChats.firstOrNull { it.id == id }
            ?: syntheticChats[id]

    /** 联系人按首字母分组（A-Z，# 结尾），供联系人页使用。 */
    /** 联系人列表：宿主里用真实好友，独立 App 用示例数据。 */
    val contacts: List<ContactItem>
        get() = LiveContacts.current

    fun groupedContacts(): List<Pair<Char, List<ContactItem>>> {
        if (LiveContacts.isLive) return LiveContacts.grouped(LiveContacts.current)
        return sampleContacts
            .groupBy { it.letter }
            .toSortedMap(compareBy { it })
            .map { (letter, list) -> letter to list.sortedBy { it.name } }
    }

    fun contactById(id: String): ContactItem? =
        LiveContacts.peek()?.firstOrNull { it.id == id }
            ?: sampleContacts.firstOrNull { it.id == id }

    /** 找出（或合成）某个联系人对应的会话，供联系人详情页「发消息」使用。 */
    fun chatForContact(contact: ContactItem): ChatItem {
        sampleChats.firstOrNull { !it.isGroup && it.name == contact.name }?.let { return it }
        val id = "chat_${contact.id}"
        return syntheticChats.getOrPut(id) {
            ChatItem(
                id = id,
                name = contact.name,
                lastMessage = "开始聊天吧",
                timeLabel = "",
                avatarSeed = contact.avatarSeed,
                isOnline = contact.isOnline,
            )
        }
    }

    fun contactDetailFor(id: String): ContactDetail? {
        val contact = contactById(id) ?: return null
        return ContactDetail(
            contact = contact,
            timId = (contact.avatarSeed * 1111 + 31415926).toString(),
            remark = if (contact.name == "妈妈" || contact.name == "爸爸") "家人" else "",
            phone = "138****${(contact.avatarSeed * 37 % 10000).toString().padStart(4, '0')}",
            email = "${contact.id.removePrefix("c_")}@example.com",
            region = "甘肃 兰州",
            tags = when (contact.letter) {
                'C' -> listOf("同事", "设计")
                'L' -> listOf("同事")
                'M', 'B' -> listOf("家人")
                else -> emptyList()
            },
            chatId = chatForContact(contact).id,
        )
    }

    /** 群聊列表（联系人页-群聊入口）。 */
    fun groupChats(): List<ChatItem> = sampleChats.filter { it.isGroup }

}

/** 会话内的示例消息。真实数据将在后续步骤从 TIM 读取。 */
object MessageRepository {

    private val profileSeed = ChatRepository.profile.avatarSeed

    private val messagesByChat: Map<String, List<ChatMessage>> = mapOf(
        "chat_project" to listOf(
            ChatMessage(
                id = "m1", chatId = "chat_project", senderName = "系统消息", senderSeed = 0,
                type = ChatMessageType.SYSTEM, content = "陈晨 邀请 赵敏 加入了群聊",
                timeSeparator = "今天 09:30",
            ),
            ChatMessage(
                id = "m2", chatId = "chat_project", senderName = "陈晨", senderSeed = 15,
                type = ChatMessageType.TEXT, content = "大家早，新版聊天列表的视觉稿我放到群文件里了",
                timeLabel = "09:32", showSenderName = true,
            ),
            ChatMessage(
                id = "m3", chatId = "chat_project", senderName = "陈晨", senderSeed = 15,
                type = ChatMessageType.FILE, content = "聊天列表视觉稿_v3.sketch",
                fileSize = "8.6 MB", timeLabel = "09:32",
            ),
            ChatMessage(
                id = "m4", chatId = "chat_project", senderName = "我", senderSeed = profileSeed,
                isMe = true, type = ChatMessageType.TEXT, content = "收到，我看看和现在的实现差多少",
                timeLabel = "09:40", isRead = true,
            ),
            ChatMessage(
                id = "m5", chatId = "chat_project", senderName = "张伟", senderSeed = 4,
                type = ChatMessageType.TEXT, content = "悬浮底栏那个效果什么时候能合进去？",
                timeLabel = "09:52", showSenderName = true,
            ),
            ChatMessage(
                id = "m6", chatId = "chat_project", senderName = "我", senderSeed = profileSeed,
                isMe = true, type = ChatMessageType.TEXT, content = "今晚吧，先把置顶卡片的对比度修一下",
                timeLabel = "09:55", isRead = false,
            ),
            ChatMessage(
                id = "m7", chatId = "chat_project", senderName = "李娜", senderSeed = 9,
                type = ChatMessageType.VOICE, content = "", voiceSeconds = 6,
                timeLabel = "10:02", showSenderName = true,
            ),
            ChatMessage(
                id = "m8", chatId = "chat_project", senderName = "我", senderSeed = profileSeed,
                isMe = true, type = ChatMessageType.TEXT, content = "界面风格切换的动画已经修好了，可以再看看",
                timeLabel = "10:08", isRead = true,
            ),
            ChatMessage(
                id = "m9", chatId = "chat_project", senderName = "张伟", senderSeed = 4,
                type = ChatMessageType.TEXT, content = "顺便问一下",
                timeLabel = "10:12", showSenderName = true,
            ),
            ChatMessage(
                id = "m10", chatId = "chat_project", senderName = "张伟", senderSeed = 4,
                type = ChatMessageType.TEXT, content = "悬浮底栏的模糊在真机上效果怎么样？",
                timeLabel = "10:12",
            ),
            ChatMessage(
                id = "m11", chatId = "chat_project", senderName = "张伟", senderSeed = 4,
                type = ChatMessageType.IMAGE, content = "真机截图",
                timeLabel = "10:12",
            ),
            ChatMessage(
                id = "m12", chatId = "chat_project", senderName = "我", senderSeed = profileSeed,
                isMe = true, type = ChatMessageType.TEXT, content = "比模拟器上强太多了",
                timeLabel = "10:14", isRead = true,
            ),
            ChatMessage(
                id = "m13", chatId = "chat_project", senderName = "我", senderSeed = profileSeed,
                isMe = true, type = ChatMessageType.TEXT, content = "回头给你录个屏",
                timeLabel = "10:14", isRead = false,
            ),
        ),
        "chat_mom" to listOf(
            ChatMessage(
                id = "mm1", chatId = "chat_mom", senderName = "妈妈", senderSeed = 8,
                type = ChatMessageType.TEXT, content = "今天降温了，多穿点",
                timeSeparator = "今天 09:20", timeLabel = "09:20",
            ),
            ChatMessage(
                id = "mm2", chatId = "chat_mom", senderName = "我", senderSeed = profileSeed,
                isMe = true, type = ChatMessageType.TEXT, content = "知道啦",
                timeLabel = "09:31", isRead = true,
            ),
            ChatMessage(
                id = "mm3", chatId = "chat_mom", senderName = "妈妈", senderSeed = 8,
                type = ChatMessageType.IMAGE, content = "家里的小猫",
                timeLabel = "09:42",
            ),
            ChatMessage(
                id = "mm4", chatId = "chat_mom", senderName = "妈妈", senderSeed = 8,
                type = ChatMessageType.TEXT, content = "晚上回家吃饭吗？",
                timeLabel = "09:47",
            ),
        ),
        "chat_zhangwei" to listOf(
            ChatMessage(
                id = "zw1", chatId = "chat_zhangwei", senderName = "张伟", senderSeed = 4,
                type = ChatMessageType.TEXT, content = "那个 Compose 的悬浮底栏效果确实好看",
                timeSeparator = "今天 08:56", timeLabel = "08:56",
            ),
            ChatMessage(
                id = "zw2", chatId = "chat_zhangwei", senderName = "张伟", senderSeed = 4,
                type = ChatMessageType.LINK, content = "Compose 手势与嵌套滚动最佳实践",
                timeLabel = "08:57",
            ),
            ChatMessage(
                id = "zw3", chatId = "chat_zhangwei", senderName = "我", senderSeed = profileSeed,
                isMe = true, type = ChatMessageType.TEXT, content = "是的，不过真机上模糊才好看",
                timeLabel = "09:00", isRead = true,
            ),
        ),
        "chat_lina" to listOf(
            ChatMessage(
                id = "li1", chatId = "chat_lina", senderName = "李娜", senderSeed = 9,
                type = ChatMessageType.TEXT, content = "文档我改完了，你抽空看下～",
                timeSeparator = "昨天 18:20", timeLabel = "18:20",
            ),
            ChatMessage(
                id = "li2", chatId = "chat_lina", senderName = "我", senderSeed = profileSeed,
                isMe = true, type = ChatMessageType.TEXT, content = "好的，我马上",
                timeLabel = "18:22", isRead = false,
            ),
        ),
        "chat_file_helper" to listOf(
            ChatMessage(
                id = "fh1", chatId = "chat_file_helper", senderName = "我", senderSeed = profileSeed,
                isMe = true, type = ChatMessageType.FILE, content = "Material3_TIM_v0.1.0.apk",
                fileSize = "2.3 MB", timeSeparator = "今天 10:24", timeLabel = "10:24",
            ),
        ),
        "chat_family" to listOf(
            ChatMessage(
                id = "fm1", chatId = "chat_family", senderName = "爸", senderSeed = 13,
                type = ChatMessageType.TEXT, content = "周末都回来吃饭",
                timeSeparator = "昨天 20:10", timeLabel = "20:10", showSenderName = true,
            ),
            ChatMessage(
                id = "fm2", chatId = "chat_family", senderName = "妈妈", senderSeed = 8,
                type = ChatMessageType.TEXT, content = "我去买菜",
                timeLabel = "20:12", showSenderName = true,
            ),
            ChatMessage(
                id = "fm3", chatId = "chat_family", senderName = "我", senderSeed = profileSeed,
                isMe = true, type = ChatMessageType.RED_PACKET, content = "给爸妈的红包",
                timeLabel = "20:15", isRead = true,
            ),
        ),
    )

    /** 返回某个会话的消息（时间顺序：旧 → 新）。没有预置数据的会话生成一段通用示例。 */
    fun messagesFor(chat: ChatItem): List<ChatMessage> {
        val base = messagesByChat[chat.id] ?: run {
            val seed = chat.avatarSeed
            listOf(
                ChatMessage(
                    id = "${chat.id}_g1", chatId = chat.id, senderName = chat.name, senderSeed = seed,
                    type = ChatMessageType.TEXT,
                    content = "在吗？",
                    timeSeparator = "今天 09:10",
                    timeLabel = "09:10",
                ),
                ChatMessage(
                    id = "${chat.id}_g2", chatId = chat.id, senderName = "我", senderSeed = profileSeed,
                    isMe = true, type = ChatMessageType.TEXT, content = "在的，怎么了？",
                    timeLabel = "09:12", isRead = true,
                ),
                ChatMessage(
                    id = "${chat.id}_g3", chatId = chat.id, senderName = chat.name, senderSeed = seed,
                    type = ChatMessageType.TEXT, content = chat.lastMessage,
                    timeLabel = "09:15",
                ),
            )
        }
        // 独立 App 的示例消息也要有稳定的 msgId：聊天页「多选 → 转发」依赖
        // chatRecordMsgId 非 0 才能勾选（宿主里由 TIM 内核提供真实 msgId）。
        // 聊天记录气泡的 chatRecordMsgId 指向被合并的消息，不能覆盖，保持原值。
        return base.mapIndexed { index, message ->
            if (message.chatRecordMsgId != 0L) {
                message
            } else {
                message.copy(chatRecordMsgId = 9_000_000L + index)
            }
        }
    }

    /** 聊天信息页的数据：群成员 / 公告等。 */
    private fun sampleTimIdOf(seed: Int): String =
        (seed.toLong() * 1111L + 31415926L).toString()

    fun chatInfoFor(chat: ChatItem): ChatInfoData {
        val members = if (chat.isGroup) {
            val names = ChatRepository.contacts.map { it.name }.take(5) + listOf("我")
            names.mapIndexed { index, name ->
                ChatMember(
                    name = name,
                    avatarSeed = ChatRepository.contacts.getOrNull(index)?.avatarSeed ?: (index * 3 + 2),
                    isOwner = index == 0,
                )
            }
        } else {
            listOf(
                ChatMember(chat.name, chat.avatarSeed),
                ChatMember(ChatRepository.profile.name, ChatRepository.profile.avatarSeed),
            )
        }
        return ChatInfoData(
            chat = chat,
            members = members,
            notice = if (chat.isGroup) "本群用于 Material3 TIM 的开发讨论，请勿转发外链。" else "",
            timId = if (chat.isGroup) {
                chat.id.split('_').getOrNull(2).orEmpty()
            } else {
                sampleTimIdOf(chat.avatarSeed)
            },
            description = if (chat.isGroup) {
                "Material3 TIM 交流群，欢迎反馈问题与建议。"
            } else {
                "这个人很懒，什么都没有留下"
            },
            memberCount = chat.memberCount,
        )
    }
}
