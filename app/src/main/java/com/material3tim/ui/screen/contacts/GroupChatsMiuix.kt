package com.material3tim.ui.screen.contacts

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.ChatItem
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.groupedCardItems
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 群聊页（Miuix 风格）：
 * 群聊数量汇总 + 创建群聊按钮 + 群聊 HyperOS4 分段卡片列表，点击进入会话。
 */
@Composable
fun GroupChatsMiuix(groupChats: List<ChatItem>) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val enableBlur = LocalEnableBlur.current
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/无 runtime shader 时为 null → 玻璃控件纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)
    val showComingSoon: () -> Unit = {
        Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
    }

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.group_chats_title),
                    isContentScrolled = listState.canScrollBackward,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { HyperBackIcon(onClick = { navigator.pop() }) },
                )
            },
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    // 采样层要早于 inset/padding：记录不透明底 + 列表内容（不含玻璃顶栏自身）。
                    .then(
                        if (backdrop != null) {
                            Modifier
                                .layerBackdrop(backdrop)
                                .background(colorScheme.surface)
                        } else {
                            Modifier
                        },
                    ),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item(key = "summary") {
                    GroupChatsSummaryMiuix(
                        count = groupChats.size,
                        onCreateGroup = showComingSoon,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp, bottom = 6.dp),
                    )
                }
                if (groupChats.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 72.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.group_chats_empty),
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                } else {
                    // HyperOS4 分组卡片：群聊行合并为 squircle 分段卡片（首/尾圆角自动处理）。
                    groupedCardItems(
                        keyPrefix = "groupChats",
                        outerTopPadding = 0.dp,
                        outerBottomPadding = 6.dp,
                        items = groupChats.map { chat ->
                            CardItem(chat.id) {
                                GroupChatRowMiuix(
                                    chat = chat,
                                    onClick = { navigator.push(Route.ChatDetail(chat.id)) },
                                )
                            }
                        },
                    )
                }
                item(key = "bottom_padding") {
                    Spacer(
                        Modifier.height(
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                    24.dp
                        )
                    )
                }
            }
        }
    }
}

/** 顶栏返回按钮：跟随顶栏风格（HyperTopBarGlassButton → Glass 圆钮 / Original 普通 IconButton）。 */
@Composable
private fun HyperBackIcon(onClick: () -> Unit) {
    HyperTopBarGlassButton(onClick = onClick) {
        Icon(
            imageVector = MiuixIcons.Back,
            contentDescription = stringResource(R.string.back),
            tint = colorScheme.onSurface,
        )
    }
}

@Composable
private fun GroupChatsSummaryMiuix(
    count: Int,
    onCreateGroup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.group_chats_summary, count),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
        TextButton(
            text = stringResource(R.string.group_chats_create),
            onClick = onCreateGroup,
        )
    }
}

/** 单条群聊行：48dp 群头像 + 群名 +（人数 ·）最后一条消息 + 箭头。 */
@Composable
private fun GroupChatRowMiuix(
    chat: ChatItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatAvatar(
            name = chat.name,
            seed = chat.avatarSeed,
            size = 48.dp,
            isGroup = true,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            // 最近会话接口拿不到群人数（memberCount=0）时只显示最后一条消息摘要，
            // 避免出现「0 人 · …」这种不准确的文案。
            val memberPrefix = if (chat.memberCount > 0) {
                stringResource(R.string.group_chats_members, chat.memberCount) + " · "
            } else {
                ""
            }
            Text(
                text = memberPrefix + chat.lastMessage,
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = MiuixIcons.ChevronForward,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariantSummary,
        )
    }
}
