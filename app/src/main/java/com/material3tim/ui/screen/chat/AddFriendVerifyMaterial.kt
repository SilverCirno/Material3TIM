package com.material3tim.ui.screen.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/**
 * 加好友验证页（Material 3 Expressive 风格）：
 * 顶栏返回 +「加好友」，目标用户卡片 + 按目标 friend_setting 变化的输入区 + 发送按钮。
 * - 查询中：loading 提示，发送禁用；
 * - 允许任何人：隐藏输入，显示无需验证提示；
 * - 需要验证信息：验证消息多行输入；
 * - 需要回答问题：逐题「问题：xxx」+ 单行「回答」输入（多条按 问题1/问题2…）。
 * 发送中按钮禁用并显示 loading，避免重复提交。
 */
@Composable
internal fun AddFriendVerifyMaterial(
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
    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_friend_verify_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    TopBarBackButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.chat_detail_back),
                    )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AddFriendVerifyUserCardMaterial(
                name = name,
                uin = uin,
                detail = detail,
                phone = phone,
            )
            when (val current = query) {
                AddFriendVerifyQuery.Loading -> {
                    AddFriendVerifyHintMaterial(stringResource(R.string.add_friend_setting_loading))
                }

                AddFriendVerifyQuery.AllowAnyone -> {
                    AddFriendVerifyHintMaterial(
                        stringResource(R.string.add_friend_setting_allow_anyone_hint),
                    )
                }

                AddFriendVerifyQuery.Message -> {
                    OutlinedTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !sending,
                        label = { Text(stringResource(R.string.add_friend_verify_message_label)) },
                        placeholder = { Text(stringResource(R.string.add_friend_verify_message_hint)) },
                        minLines = 3,
                        maxLines = 5,
                    )
                }

                is AddFriendVerifyQuery.Questions -> {
                    if (current.questions.isEmpty()) {
                        AddFriendVerifyQuestionMaterial(
                            title = null,
                            answer = answers.getOrElse(0) { "" },
                            onAnswerChange = { onAnswerChange(0, it) },
                            enabled = !sending,
                        )
                    } else {
                        current.questions.forEachIndexed { index, question ->
                            AddFriendVerifyQuestionMaterial(
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
            Button(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_friend_verify_sending))
                } else {
                    Text(stringResource(R.string.add_friend_verify_send))
                }
            }
        }
    }
}

/** 状态提示行（查询中 / 允许任何人无需验证）。 */
@Composable
private fun AddFriendVerifyHintMaterial(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

/** 单个验证问题 + 答案输入；title 为 null 时只显示「回答」输入框（问题列表为空时的兜底）。 */
@Composable
private fun AddFriendVerifyQuestionMaterial(
    title: String?,
    answer: String,
    onAnswerChange: (String) -> Unit,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(R.string.add_friend_setting_answer_label)) },
            placeholder = { Text(stringResource(R.string.add_friend_setting_answer_hint)) },
        )
    }
}

@Composable
private fun AddFriendVerifyUserCardMaterial(
    name: String,
    uin: Long,
    detail: String,
    phone: String,
) {
    val colorScheme = MaterialTheme.colorScheme
    val subtitle = addFriendVerifySubtitle(uin = uin, detail = detail, phone = phone)
    TonalCard(containerColor = MaterialTheme.colorScheme.surfaceBright) {
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
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { "—" },
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
