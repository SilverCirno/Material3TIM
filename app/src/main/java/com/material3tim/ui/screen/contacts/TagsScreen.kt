package com.material3tim.ui.screen.contacts

import androidx.compose.runtime.Composable
import com.material3tim.data.repository.ChatRepository
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator

/**
 * 标签页。根据 [LocalUiMode] 分发到 Material / Miuix 两套实现。
 */
@Composable
fun TagsScreen() {
    val navigator = LocalNavigator.current
    val onBack: () -> Unit = { navigator.pop() }
    when (LocalUiMode.current) {
        UiMode.Material -> TagsMaterial(onBack = onBack)
        UiMode.Miuix -> TagsMiuix(onBack = onBack)
    }
}

/**
 * 标签成员数据只有姓名，没有头像种子；按姓名从联系人数据中匹配头像种子，
 * 匹配不到时用姓名的 hash 兜底，保证头像至少有稳定配色。
 */
internal fun tagMemberAvatarSeed(name: String): Int =
    ChatRepository.contacts.firstOrNull { it.name == name }?.avatarSeed
        ?: (name.hashCode() and 0x7FFFFFFF)
