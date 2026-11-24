package com.material3tim.data.source

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 宿主（TIM 进程）内核推送事件。
 *
 * TIM 的 `IKernelMsgListener` 回调发生在内核线程；[com.material3tim.xposed.TimBridge]
 * 把回调整理成 [MessageEvent] 塞进 [LiveEvents]，UI 层订阅后再刷新对应页面：
 * - 会话列表：订阅 `LiveChats.liveFlow`，由 TimBridge 触发 `LiveChats.refresh()`
 * - 聊天详情：订阅 [messageEvents]，收到当前会话的新消息时重新拉取消息
 * - 底部角标：订阅 `LiveChats.liveFlow` 计算未读总数
 *
 * 独立 App（无宿主）里永远不会有事件，UI 各处的订阅可安全使用。
 */
data class MessageEvent(
    /** 目标会话 id（如 `tim_1_<uid>` / `tim_2_<groupCode>`）；解析不出来时为 null。 */
    val chatId: String?,
    /** 本次事件涉及的消息 id（可能为空，例如会话列表批量更新）。 */
    val msgIds: List<Long>,
    val kind: Kind,
    val timestampMs: Long = System.currentTimeMillis(),
) {
    enum class Kind {
        /** 新消息（onRecvMsg / onRecvMsgResp 等） */
        NEW,

        /** 消息状态更新（onMsgInfoListUpdate / onMsgUpdate） */
        UPDATE,

        /** 消息删除（onMsgDelete） */
        DELETE,

        /** 撤回（onMsgRecall） */
        RECALL,

        /** 其它只用于触发刷新的内核回调 */
        UNKNOWN,
    }
}

/** 页面数据异步刷新完成事件：慢查询（成员/公告/SSO）在后台完成后通知 UI 局部刷新。 */
data class DataUpdate(
    /** 目标会话 id（如 `tim_2_<groupCode>`）。 */
    val chatId: String,
    val kind: Kind,
    val timestampMs: Long = System.currentTimeMillis(),
) {
    enum class Kind {
        /** 群成员列表已刷新（ChatInfoData.members）。 */
        MEMBERS,

        /** 群公告列表已刷新（loadGroupNotices）。 */
        NOTICES,

        /** 群资料/最新公告 memo 已刷新（ChatInfoData.notice/description/memberCount 等）。 */
        CHAT_INFO,

        /** 用户资料已刷新（UserProfile）。 */
        USER_PROFILE,
    }
}

object LiveEvents {

    private val _messageEvents = MutableSharedFlow<MessageEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** 内核消息事件流（宿主里才会产生）。 */
    val messageEvents: SharedFlow<MessageEvent> = _messageEvents.asSharedFlow()

    private val _dataUpdates = MutableSharedFlow<DataUpdate>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** 页面数据异步刷新事件流（宿主里才会产生）。 */
    val dataUpdates: SharedFlow<DataUpdate> = _dataUpdates.asSharedFlow()

    /** 供 xposed 桥调用：线程安全，不会阻塞调用方。 */
    fun emitMessageEvent(event: MessageEvent) {
        _messageEvents.tryEmit(event)
    }

    /** 供 xposed 桥调用：后台数据刷新完成后 emit，UI 收到后局部刷新对应页面/区块。 */
    fun emitDataUpdate(update: DataUpdate) {
        _dataUpdates.tryEmit(update)
    }
}
