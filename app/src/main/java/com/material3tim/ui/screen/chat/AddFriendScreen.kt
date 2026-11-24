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
import com.material3tim.data.model.AddFriendGroup
import com.material3tim.data.model.AddFriendSearchResult
import com.material3tim.data.model.AddFriendUser
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 「加好友/群」搜索页的 UI 状态。 */
internal sealed interface AddFriendUiState {
    /** 还没输入关键词。 */
    data object Idle : AddFriendUiState

    /** 正在搜索。 */
    data object Loading : AddFriendUiState

    /** 搜索成功但个人/群都没有匹配。 */
    data object Empty : AddFriendUiState

    /** 搜索失败（宿主数据源报错）。 */
    data object Failed : AddFriendUiState

    /** 独立 App / 宿主未注册：没有加好友数据源。 */
    data object HostOnly : AddFriendUiState

    /** 搜索结果：个人账号在上、群聊在下。 */
    data class Results(val result: AddFriendSearchResult) : AddFriendUiState
}

/** 输入防抖时长：连续输入时只搜索最后一次关键词。 */
private const val AddFriendSearchDebounceMillis = 300L

/**
 * 加好友/群搜索页入口（从消息页加号菜单「加好友/群」进入，`Route.AddFriend`）。
 *
 * 支持 QQ号 / 手机号 / 群号搜索；关键词防抖后调用 `AddFriendSource.search`（IO 线程），
 * 结果再按 [LocalUiMode] 分发给两套实现：个人账号、群聊各一张卡片。
 * 独立 App（addFriendSource == null）显示「仅宿主内可用」提示态。
 */
@Composable
fun AddFriendScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var keyword by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf<AddFriendUiState>(AddFriendUiState.Idle) }
    // 加群请求进行中：防止连续点击重复提交。
    var actionInFlight by remember { mutableStateOf(false) }

    val hostOnlyToast = stringResource(R.string.add_friend_host_only)
    val joinSentToast = stringResource(R.string.add_friend_join_sent)
    val joinFailedToast = stringResource(R.string.add_friend_join_failed)

    LaunchedEffect(keyword) {
        val query = keyword.trim()
        if (query.isEmpty()) {
            state = AddFriendUiState.Idle
            return@LaunchedEffect
        }
        // 防抖：关键词继续变化时本协程被取消，只有停手 300ms 后才真正搜索。
        delay(AddFriendSearchDebounceMillis)
        state = AddFriendUiState.Loading
        val source = AppDataSources.addFriendSource
        if (source == null) {
            state = AddFriendUiState.HostOnly
            return@LaunchedEffect
        }
        // 数据源是阻塞调用，必须放 IO 线程；异常/失败都归到 Failed 状态。
        val result = withContext(Dispatchers.IO) {
            runCatching { source.search(query) }.getOrNull()
        }
        state = when {
            result == null -> AddFriendUiState.Failed
            result.users.isEmpty() && result.groups.isEmpty() -> AddFriendUiState.Empty
            else -> AddFriendUiState.Results(result)
        }
    }

    // 点个人结果：不再直接发请求，先进「加好友验证页」填写验证消息（真正发请求在验证页里做）。
    val onOpenVerify: (AddFriendUser) -> Unit = { user ->
        navigator.push(
            Route.AddFriendVerify(
                uid = user.uid,
                uin = user.uin,
                name = user.name,
                detail = user.detail,
                phone = user.phone,
                token = user.token,
            ),
        )
    }

    // 申请加群：IO 调数据源，成功后 Toast「已提交加群申请」。
    val onJoinGroup: (AddFriendGroup) -> Unit = { group ->
        if (!actionInFlight) {
            val source = AppDataSources.addFriendSource
            if (source == null) {
                Toast.makeText(context, hostOnlyToast, Toast.LENGTH_SHORT).show()
            } else {
                actionInFlight = true
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching { source.joinGroup(group) }.getOrDefault(false)
                    }
                    actionInFlight = false
                    Toast.makeText(
                        context,
                        if (ok) joinSentToast else joinFailedToast,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    val onBack: () -> Unit = { navigator.pop() }
    when (LocalUiMode.current) {
        UiMode.Miuix -> AddFriendMiuix(
            keyword = keyword,
            state = state,
            onKeywordChange = { keyword = it },
            onBack = onBack,
            onOpenVerify = onOpenVerify,
            onJoinGroup = onJoinGroup,
        )
        UiMode.Material -> AddFriendMaterial(
            keyword = keyword,
            state = state,
            onKeywordChange = { keyword = it },
            onBack = onBack,
            onOpenVerify = onOpenVerify,
            onJoinGroup = onJoinGroup,
        )
    }
}
