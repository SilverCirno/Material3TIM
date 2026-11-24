package com.material3tim.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 加好友验证页（Miuix 风格）：
 * 顶栏返回 +「加好友」，目标用户 Card + 按目标 friend_setting 变化的输入区 + 发送按钮。
 * - 查询中：loading 提示，发送禁用；
 * - 允许任何人：隐藏输入，显示无需验证提示；
 * - 需要验证信息：验证消息多行输入 Card；
 * - 需要回答问题：逐题「问题：xxx」+ 单行「回答」输入 Card（多条按 问题1/问题2…）。
 * 发送中按钮禁用并显示「发送中…」，避免重复提交。
 */
@Composable
internal fun AddFriendVerifyMiuix(
    name: String,
    uin: Long,
    detail: String,
    phone: String,
    query: AddFriendVerifyQuery,
    message: String,
    onMessageChange: (String) -> Unit,
    answers: List<String>,
    onAnswerChange: (Int, String) -> Unit,
    sending: Boolean,
    canSend: Boolean,
    onBack: () -> Unit,
    onSend: () -> Unit,
) {
    val enableBlur = LocalEnableBlur.current
    val scrollBehavior = MiuixScrollBehavior()
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/设备不支持 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)
    val layoutDirection = LocalLayoutDirection.current

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.add_friend_verify_title),
                    scrollBehavior = scrollBehavior,
                    backdrop = backdrop,
                    navigationIcon = {
                        // 顶栏按钮跟随顶栏风格：HyperTopBarGlassButton 自动选择玻璃圆钮 / 普通 IconButton。
                        HyperTopBarGlassButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.chat_detail_back),
                                tint = colorScheme.onSurface,
                            )
                        }
                    },
                )
            },
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 采样层要早于 inset/padding：记录不透明底 + 页面内容（不含玻璃顶栏自身）。
                    .then(
                        if (backdrop != null) {
                            Modifier
                                .layerBackdrop(backdrop)
                                .background(colorScheme.surface)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
                            end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp,
                            top = innerPadding.calculateTopPadding() + 12.dp,
                        )
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AddFriendVerifyUserCardMiuix(
                        name = name,
                        uin = uin,
                        detail = detail,
                        phone = phone,
                    )
                    when (val current = query) {
                        AddFriendVerifyQuery.Loading -> {
                            AddFriendVerifyHintMiuix(stringResource(R.string.add_friend_setting_loading))
                        }

                        AddFriendVerifyQuery.AllowAnyone -> {
                            AddFriendVerifyHintMiuix(
                                stringResource(R.string.add_friend_setting_allow_anyone_hint),
                            )
                        }

                        AddFriendVerifyQuery.Message -> {
                            AddFriendVerifyMessageCardMiuix(
                                message = message,
                                onMessageChange = onMessageChange,
                                enabled = !sending,
                            )
                        }

                        is AddFriendVerifyQuery.Questions -> {
                            if (current.questions.isEmpty()) {
                                AddFriendVerifyQuestionCardMiuix(
                                    title = null,
                                    answer = answers.getOrElse(0) { "" },
                                    onAnswerChange = { onAnswerChange(0, it) },
                                    enabled = !sending,
                                )
                            } else {
                                current.questions.forEachIndexed { index, question ->
                                    AddFriendVerifyQuestionCardMiuix(
                                        title = if (current.questions.size == 1) {
                                            stringResource(R.string.add_friend_setting_question, question)
                                        } else {
                                            stringResource(
                                                R.string.add_friend_setting_question_indexed,
                                                index + 1,
                                                question,
                                            )
                                        },
                                        answer = answers.getOrElse(index) { "" },
                                        onAnswerChange = { onAnswerChange(index, it) },
                                        enabled = !sending,
                                    )
                                }
                            }
                        }
                    }
                    TextButton(
                        text = stringResource(
                            if (sending) R.string.add_friend_verify_sending else R.string.add_friend_verify_send,
                        ),
                        onClick = onSend,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canSend,
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    }
}

/** 状态提示行（查询中 / 允许任何人无需验证）。 */
@Composable
private fun AddFriendVerifyHintMiuix(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = colorScheme.onSurfaceVariantSummary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

/** 验证消息输入 Card（setting == 1 / 查询失败回退）。 */
@Composable
private fun AddFriendVerifyMessageCardMiuix(
    message: String,
    onMessageChange: (String) -> Unit,
    enabled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.add_friend_verify_message_label),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                textStyle = TextStyle(fontSize = 15.sp, color = colorScheme.onSurface),
                cursorBrush = SolidColor(colorScheme.primary),
                singleLine = false,
                maxLines = 5,
                decorationBox = { innerTextField ->
                    Box {
                        if (message.isEmpty()) {
                            Text(
                                text = stringResource(R.string.add_friend_verify_message_hint),
                                fontSize = 15.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

/** 单个验证问题 + 单行答案输入 Card；title 为 null 时只显示「回答」输入（问题列表为空兜底）。 */
@Composable
private fun AddFriendVerifyQuestionCardMiuix(
    title: String?,
    answer: String,
    onAnswerChange: (String) -> Unit,
    enabled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = stringResource(R.string.add_friend_setting_answer_label),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(4.dp))
            BasicTextField(
                value = answer,
                onValueChange = onAnswerChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                textStyle = TextStyle(fontSize = 15.sp, color = colorScheme.onSurface),
                cursorBrush = SolidColor(colorScheme.primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (answer.isEmpty()) {
                            Text(
                                text = stringResource(R.string.add_friend_setting_answer_hint),
                                fontSize = 15.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
private fun AddFriendVerifyUserCardMiuix(
    name: String,
    uin: Long,
    detail: String,
    phone: String,
) {
    val subtitle = addFriendVerifySubtitle(uin = uin, detail = detail, phone = phone)
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatAvatar(
                name = name.ifBlank { "—" },
                seed = uin.hashCode(),
                size = 56.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { "—" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight(600),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
