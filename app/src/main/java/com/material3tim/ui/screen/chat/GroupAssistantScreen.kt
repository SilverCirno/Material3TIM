package com.material3tim.ui.screen.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.material3tim.data.model.ChatItem
import com.material3tim.data.model.GroupAssistantData
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 消息列表里「群助手」伪会话的固定 id（与 bridge 映射一致）。 */
internal const val GroupAssistantChatId = "tim_troop_assistant"

/**
 * 群助手二级页入口（消息列表群助手卡片点击进入）：
 * 数据来自 [AppDataSources.chatSource]?.loadGroupAssistant（阻塞 IO），按 [LocalUiMode] 分发两套实现。
 */
@Composable
fun GroupAssistantScreen() {
    val navigator = LocalNavigator.current
    var data by remember { mutableStateOf<GroupAssistantData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTick) {
        loading = true
        failed = false
        val source = AppDataSources.chatSource
        if (source == null) {
            // 独立 App / 宿主未就绪：无群助手数据 → 空态。
            data = null
            loading = false
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            runCatching { source.loadGroupAssistant() }.getOrNull()
        }
        data = loaded
        failed = loaded == null
        loading = false
    }

    val groups = data?.groups.orEmpty()
    val onBack: () -> Unit = { navigator.pop() }
    val onOpenGroup: (ChatItem) -> Unit = { group ->
        navigator.push(Route.ChatDetail(group.id))
    }
    val onRetry: () -> Unit = { refreshTick += 1 }

    when (LocalUiMode.current) {
        UiMode.Material -> GroupAssistantMaterial(
            groups = groups,
            loading = loading,
            failed = failed,
            onBack = onBack,
            onRetry = onRetry,
            onOpenGroup = onOpenGroup,
        )

        UiMode.Miuix -> GroupAssistantMiuix(
            groups = groups,
            loading = loading,
            failed = failed,
            onBack = onBack,
            onRetry = onRetry,
            onOpenGroup = onOpenGroup,
        )
    }
}
