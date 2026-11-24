package com.material3tim.ui.screen.profile

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.repository.ChatRepository
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.groupedCardItems
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 「编辑资料」页（Miuix 风格）。
 * 昵称 / 个性签名 / 性别 / 生日 / 地区均为本地状态，仅用于示例展示。
 */
@Composable
internal fun EditProfileMiuix(onBack: () -> Unit) {
    val context = LocalContext.current
    val profile = ChatRepository.profile
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val savedMessage = stringResource(R.string.edit_profile_saved)
    val doneLabel = stringResource(R.string.done)
    val genderItems = listOf(
        stringResource(R.string.gender_male),
        stringResource(R.string.gender_female),
        stringResource(R.string.gender_secret),
    )
    val birthdayValue = stringResource(R.string.edit_profile_birthday_value)
    val regionValue = stringResource(R.string.edit_profile_region_value)

    var nickname by remember { mutableStateOf(profile.name) }
    var signature by remember { mutableStateOf(profile.signature) }
    var genderIndex by remember { mutableIntStateOf(genderItems.lastIndex) }
    val showComingSoon: () -> Unit = {
        Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
    }

    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberPageBackdrop(enableBlur)

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.edit_profile),
                    isContentScrolled = listState.canScrollBackward,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { HyperBackIcon(onClick = onBack) },
                    actions = {
                        TextButton(
                            text = doneLabel,
                            onClick = {
                                Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
                                onBack()
                            },
                        )
                    },
                )
            },
            // popupHost 使用 Scaffold 默认 MiuixPopupHost：GlassDropdownPreference 的玻璃下拉需要它。
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
                // HyperOS4 分组卡片：头像入口单张卡片。
                groupedCardItems(
                    keyPrefix = "editAvatar",
                    outerTopPadding = 12.dp,
                    outerBottomPadding = 6.dp,
                    items = listOf(
                        CardItem("avatar") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = showComingSoon)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ProfileAvatar(
                                    name = nickname,
                                    avatarSeed = profile.avatarSeed,
                                    isOnline = profile.isOnline,
                                    size = 72.dp,
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.edit_profile_avatar),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.onSurface,
                                    )
                                    Text(
                                        text = stringResource(R.string.edit_profile_avatar_hint),
                                        fontSize = 13.sp,
                                        color = colorScheme.onSurfaceVariantSummary,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        },
                    ),
                )
                // 表单：昵称 / 签名 / 性别 各成一段 squircle 卡片。
                groupedCardItems(
                    keyPrefix = "editForm",
                    outerTopPadding = 0.dp,
                    outerBottomPadding = 6.dp,
                    items = listOf(
                        CardItem("nickname") {
                            EditProfileFieldMiuix(
                                title = stringResource(R.string.edit_profile_nickname),
                                value = nickname,
                                onValueChange = { nickname = it },
                                placeholder = "",
                            )
                        },
                        CardItem("signature") {
                            EditProfileFieldMiuix(
                                title = stringResource(R.string.edit_profile_signature),
                                value = signature,
                                onValueChange = { signature = it },
                                placeholder = stringResource(R.string.profile_signature),
                            )
                        },
                        CardItem("gender") {
                            // Phase 2f：backdrop 可用时用 miuix-glass 玻璃下拉，否则回退旧 Overlay 弹层。
                            EditProfileDropdownPreference(
                                title = stringResource(R.string.edit_profile_gender),
                                items = genderItems,
                                selectedIndex = genderIndex,
                                backdrop = backdrop,
                                onSelectedIndexChange = { genderIndex = it },
                            )
                        },
                    ),
                )
                groupedCardItems(
                    keyPrefix = "editExtra",
                    outerTopPadding = 0.dp,
                    outerBottomPadding = 6.dp,
                    items = listOf(
                        CardItem("birthday") {
                            ArrowPreference(
                                title = stringResource(R.string.edit_profile_birthday),
                                summary = birthdayValue,
                                onClick = showComingSoon,
                            )
                        },
                        CardItem("region") {
                            ArrowPreference(
                                title = stringResource(R.string.edit_profile_region),
                                summary = regionValue,
                                onClick = showComingSoon,
                            )
                        },
                    ),
                )
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

/** Miuix 风格可编辑文本行：标题在上、输入内容在下，点击整行聚焦输入框。 */
@Composable
private fun EditProfileFieldMiuix(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { focusRequester.requestFocus() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(colorScheme.primary),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 16.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

/**
 * 编辑资料下拉设置项（Phase 2f）：
 * - backdrop 可用（模糊开启 + 设备支持 runtime shader）→ miuix-glass [GlassDropdownPreference]；
 * - 否则回退旧 [OverlayDropdownPreference]（Overlay 窗口弹层，兼容关闭模糊/低端机）。
 * 两者共用同一份 items/selectedIndex/回调，性别选中逻辑不变。
 */
@Composable
private fun EditProfileDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    backdrop: Backdrop?,
    onSelectedIndexChange: (Int) -> Unit,
) {
    if (backdrop != null && isRuntimeShaderSupported()) {
        GlassDropdownPreference(
            title = title,
            items = items,
            selectedIndex = selectedIndex,
            backdrop = backdrop,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    } else {
        OverlayDropdownPreference(
            title = title,
            items = items,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}
