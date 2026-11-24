package com.material3tim.ui.screen.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.material3tim.data.source.AtTarget

/** 聊天页底部当前展开的面板（表情面板 / 附件面板互斥）。 */
internal enum class ChatPanel {
    None,
    Emoji,
    Attach,
}

/**
 * 聊天输入区的共享状态：输入文字 / 当前展开的面板 / 附件面板底部提示是否已关闭。
 *
 * 由 [ChatDetailScreen] 持有并传给 Material / Miuix 两套实现，切换 UI 模式时草稿与面板状态不丢。
 * 表情面板点击 emoji 直接往输入文字末尾追加，因此输入框状态必须放在两套 UI 之上。
 */
@Stable
internal class ChatComposerState(
    text: String = "",
    panel: ChatPanel = ChatPanel.None,
    attachHintVisible: Boolean = true,
) {
    var text: String by mutableStateOf(text)
        private set

    var panel: ChatPanel by mutableStateOf(panel)
        private set

    var attachHintVisible: Boolean by mutableStateOf(attachHintVisible)
        private set

    /** 待发送的 @ 目标（@成员 / @全体成员）；发送成功后由页面清空。 */
    var atTargets: List<AtTarget> by mutableStateOf(emptyList())
        private set

    /** 群聊 @ 成员选择面板是否展开（输入框以「@」结尾时自动打开）。 */
    var atPickerOpen: Boolean by mutableStateOf(false)
        private set

    val isPanelOpen: Boolean
        get() = panel != ChatPanel.None

    fun onTextChange(value: String) {
        text = value
    }

    /**
     * 输入框内容变化：群聊里输入以「@」结尾时打开成员选择面板，删除「@」/离开群聊时收起。
     * 手动清空输入文字时一并清空已选的 @ 目标，避免发出"空文本带 @"。
     */
    fun onAtTextChanged(oldValue: String, newValue: String, isGroup: Boolean) {
        text = newValue
        if (!isGroup) {
            atPickerOpen = false
            return
        }
        // 部分输入法（含 adb input）会把 @ 转成全角 ＠，两种都识别。
        val endsAt = newValue.endsWith("@") || newValue.endsWith("＠")
        // 只要本次输入让文本变长且以 @ 结尾就打开（关闭 Sheet 后文本里残留 @ 时，
        // 再输入一个 @ 也要能重新唤起，而不是必须删光重输）。
        val typedAt = endsAt && newValue.length > oldValue.length
        if (typedAt) {
            atPickerOpen = true
            // 键盘可见时表情/附件面板不应同时占位。
            panel = ChatPanel.None
        } else if (atPickerOpen && !endsAt) {
            atPickerOpen = false
        }
        if (newValue.isBlank() && atTargets.isNotEmpty()) {
            atTargets = emptyList()
        }
    }

    /**
     * 选择 @ 目标：把输入框末尾的「@」替换成「@昵称 」（没有末尾 @ 则直接追加），
     * 记录到 [atTargets]，并收起面板。
     */
    fun selectAt(target: AtTarget) {
        val insertion = target.display + " "
        text = when {
            text.endsWith("@") || text.endsWith("＠") -> text.dropLast(1) + insertion
            else -> text + insertion
        }
        atTargets = atTargets + target
        atPickerOpen = false
    }

    /**
     * 底部 Sheet 多选确认：去掉输入框末尾的「@」/「＠」，按顺序依次追加每个目标的「@昵称 」，
     * 记录到 [atTargets] 并关闭面板。
     */
    fun selectAts(targets: List<AtTarget>) {
        atPickerOpen = false
        if (targets.isEmpty()) return
        val base = when {
            text.endsWith("@") || text.endsWith("＠") -> text.dropLast(1)
            else -> text
        }
        text = base + targets.joinToString(separator = "") { it.display + " " }
        atTargets = atTargets + targets
    }

    fun dismissAtPicker() {
        atPickerOpen = false
    }

    fun clearAtTargets() {
        atTargets = emptyList()
    }

    fun appendEmoji(emoji: String) {
        text += emoji
    }

    fun clearText() {
        text = ""
    }

    /** 点击图标：已展开则收起，否则展开目标面板。 */
    fun togglePanel(target: ChatPanel) {
        panel = if (panel == target) ChatPanel.None else target
    }

    /** 直接切到某个面板（用于附件面板里的“表情包”入口）。 */
    fun showPanel(target: ChatPanel) {
        panel = target
    }

    fun closePanels() {
        panel = ChatPanel.None
    }

    fun dismissAttachHint() {
        attachHintVisible = false
    }
}

@Composable
internal fun rememberChatComposerState(): ChatComposerState = rememberSaveable(saver = ChatComposerStateSaver) {
    ChatComposerState()
}

private val ChatComposerStateSaver = listSaver<ChatComposerState, String>(
    save = { state ->
        listOf(state.text, state.panel.name, state.attachHintVisible.toString())
    },
    restore = { values ->
        ChatComposerState(
            text = values.getOrElse(0) { "" },
            panel = runCatching { ChatPanel.valueOf(values.getOrElse(1) { ChatPanel.None.name }) }
                .getOrDefault(ChatPanel.None),
            attachHintVisible = values.getOrElse(2) { "true" }.toBoolean(),
        )
    },
)
