package com.material3tim.ui.screen.chat

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.material3tim.R
import com.material3tim.data.model.AddFriendUser
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 加好友验证页按目标用户 friend_setting 呈现的输入模式。
 *
 * 0=允许任何人（无需验证信息）/ 1=需要验证信息 / 2、3、4=需要回答验证问题。
 */
internal sealed interface AddFriendVerifyQuery {
    /** 正在查询目标用户的加好友设置（发送按钮禁用）。 */
    data object Loading : AddFriendVerifyQuery

    /** setting == 0：允许任何人添加，隐藏验证消息输入，发送空 message。 */
    data object AllowAnyone : AddFriendVerifyQuery

    /** setting == 1 或查询失败回退：填写验证消息。 */
    data object Message : AddFriendVerifyQuery

    /** setting 2/3/4 或带验证问题：逐题回答；questions 为空时退化为一个「回答」输入框。 */
    data class Questions(val questions: List<String>) : AddFriendVerifyQuery
}

/**
 * 加好友验证页入口（`Route.AddFriendVerify`，从加好友/群搜索结果里的个人账号点进来）。
 *
 * 进入页面先去 IO 查询目标用户的 friend_setting：
 * - setting 0：隐藏验证消息输入，提示无需验证，发送空 message；
 * - setting 1 / 查询失败：保持验证消息输入；
 * - setting 2/3/4 或带验证问题：逐题展示问题 + 答案输入，发送时按 TIM 原生模板组装 message。
 * 发送成功后 Toast 并返回搜索页，失败优先展示服务端文案并停留本页。
 */
@Composable
fun AddFriendVerifyScreen(
    uid: String,
    uin: Long,
    name: String,
    detail: String,
    phone: String,
    token: String,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 查询设置与发送请求共用同一份目标用户数据。
    val user = remember(uid, uin, name, detail, phone, token) {
        AddFriendUser(uid = uid, uin = uin, name = name, detail = detail, phone = phone, token = token)
    }

    // 预填一句默认验证消息（用户可改），按 uid 保存，换目标用户时重置。
    val defaultMessage = stringResource(R.string.add_friend_verify_default_message)
    var message by rememberSaveable(uid) { mutableStateOf(defaultMessage) }
    // 验证问题的答案（index 对应问题顺序）；普通验证消息模式下不使用。
    var answers by rememberSaveable(uid) { mutableStateOf<List<String>>(emptyList()) }
    var query by remember(uid) { mutableStateOf<AddFriendVerifyQuery>(AddFriendVerifyQuery.Loading) }
    var sending by remember(uid) { mutableStateOf(false) }

    val hostOnlyToast = stringResource(R.string.add_friend_host_only)
    val sentToast = stringResource(R.string.add_friend_request_sent)
    val failedToast = stringResource(R.string.add_friend_request_failed)

    // 进页面先查目标用户的加好友设置（阻塞调用，IO 线程）；失败回退为「验证消息」模式。
    LaunchedEffect(uid) {
        val source = AppDataSources.addFriendSource
        if (source == null) {
            query = AddFriendVerifyQuery.Message
            return@LaunchedEffect
        }
        val info = withContext(Dispatchers.IO) {
            runCatching { source.queryFriendSetting(user) }.getOrNull()
        }
        query = when {
            info == null -> AddFriendVerifyQuery.Message
            info.setting == 0 -> AddFriendVerifyQuery.AllowAnyone
            info.questions.isNotEmpty() || info.setting in 2..4 -> {
                val count = if (info.questions.isEmpty()) 1 else info.questions.size
                // 配置变更后重新查询时保留已输入的答案（数量一致才保留）。
                if (answers.size != count) answers = List(count) { "" }
                AddFriendVerifyQuery.Questions(info.questions)
            }
            else -> AddFriendVerifyQuery.Message
        }
    }

    val answerCount = when (val current = query) {
        is AddFriendVerifyQuery.Questions ->
            if (current.questions.isEmpty()) 1 else current.questions.size
        else -> 0
    }
    val answersValid = answerCount == 0 ||
        (answers.size >= answerCount && answers.take(answerCount).all { it.isNotBlank() })
    val canSend = !sending && query != AddFriendVerifyQuery.Loading && answersValid

    // 按当前模式组装发送文案：
    // - 允许任何人：空串；
    // - 单个问题：答案原文；
    // - 多个问题：问题1:<q1>\n回答:<a1>\n问题2:<q2>\n回答:<a2>（TIM 原生模板，半角冒号）；
    // - 验证消息：输入框文本。
    val buildMessage: () -> String = {
        when (val current = query) {
            AddFriendVerifyQuery.AllowAnyone -> ""
            is AddFriendVerifyQuery.Questions -> when {
                current.questions.size <= 1 -> answers.getOrElse(0) { "" }.trim()
                else -> current.questions.mapIndexed { index, question ->
                    "问题${index + 1}:$question\n回答:${answers.getOrElse(index) { "" }.trim()}"
                }.joinToString("\n")
            }
            else -> message.trim()
        }
    }

    val onAnswerChange: (Int, String) -> Unit = { index, value ->
        if (index in answers.indices) {
            answers = answers.toMutableList().also { it[index] = value }
        }
    }
    val onBack: () -> Unit = { if (!sending) navigator.pop() }
    val onSend: () -> Unit = {
        if (canSend) {
            val source = AppDataSources.addFriendSource
            if (source == null) {
                // 独立 App：没有加好友数据源，明确提示而不是静默失败。
                Toast.makeText(context, hostOnlyToast, Toast.LENGTH_SHORT).show()
            } else {
                val payload = buildMessage()
                sending = true
                scope.launch {
                    // 内核加好友是阻塞调用，必须放 IO 线程。
                    val ok = withContext(Dispatchers.IO) {
                        runCatching { source.requestAddFriend(user, payload) }.getOrDefault(false)
                    }
                    sending = false
                    if (ok) {
                        Toast.makeText(context, sentToast, Toast.LENGTH_SHORT).show()
                        navigator.pop()
                    } else {
                        // 服务端/风控给了可读文案（如“操作过于频繁，请稍后再试”）就优先展示。
                        val serverError = source.lastAddFriendError()?.takeIf { it.isNotBlank() }
                        Toast.makeText(
                            context,
                            serverError ?: failedToast,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> AddFriendVerifyMiuix(
            name = name,
            uin = uin,
            detail = detail,
            phone = phone,
            query = query,
            message = message,
            onMessageChange = { message = it },
            answers = answers,
            onAnswerChange = onAnswerChange,
            sending = sending,
            canSend = canSend,
            onBack = onBack,
            onSend = onSend,
        )

        UiMode.Material -> AddFriendVerifyMaterial(
            name = name,
            uin = uin,
            detail = detail,
            phone = phone,
            query = query,
            message = message,
            onMessageChange = { message = it },
            answers = answers,
            onAnswerChange = onAnswerChange,
            sending = sending,
            canSend = canSend,
            onBack = onBack,
            onSend = onSend,
        )
    }
}

/**
 * 目标用户卡片副标题：优先 QQ 号，其次手机号，最后用搜索结果里的详情/签名兜底。
 * 两种 UI 风格共用。
 */
@Composable
internal fun addFriendVerifySubtitle(uin: Long, detail: String, phone: String): String = when {
    uin > 0L -> stringResource(R.string.add_friend_verify_qq, uin.toString())
    phone.isNotBlank() -> stringResource(R.string.add_friend_verify_phone, phone)
    else -> detail
}
