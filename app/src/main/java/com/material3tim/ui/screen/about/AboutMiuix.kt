package com.material3tim.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.BuildConfig
import com.material3tim.R
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.util.ExternalLink
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
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun AboutMiuix(onBack: () -> Unit) {
    // 与消息气泡链接一致：走 ExternalLink（Custom Tabs/默认浏览器），并标记外跳预期，
    // 避免 TIM 的「即将离开 TIM 打开其他应用」确认框。
    val context = LocalContext.current
    val githubUrl = stringResource(R.string.about_github_url)
    val telegramUrl = stringResource(R.string.about_telegram_url)
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val enableBlur = LocalEnableBlur.current
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/无 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.about),
                    isContentScrolled = listState.canScrollBackward,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { HyperBackIcon(onClick = onBack) },
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
                item(key = "header") {
                    AboutHeaderMiuix(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                }
                // HyperOS4 分组卡片：介绍 / GitHub 入口 / 第三方库各成一组 squircle 分段卡片。
                groupedCardItems(
                    keyPrefix = "aboutDescription",
                    outerTopPadding = 0.dp,
                    outerBottomPadding = 6.dp,
                    items = listOf(
                        CardItem("description") {
                            Text(
                                text = stringResource(R.string.about_description),
                                fontSize = 14.sp,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(16.dp),
                            )
                        },
                    ),
                )
                groupedCardItems(
                    keyPrefix = "aboutGithub",
                    outerTopPadding = 0.dp,
                    outerBottomPadding = 6.dp,
                    items = listOf(
                        CardItem("github") {
                            ArrowPreference(
                                title = stringResource(R.string.about_github),
                                summary = githubUrl,
                                onClick = { ExternalLink.open(context, githubUrl) },
                            )
                        },
                        CardItem("telegram") {
                            ArrowPreference(
                                title = stringResource(R.string.about_telegram),
                                summary = telegramUrl,
                                onClick = { ExternalLink.open(context, telegramUrl) },
                            )
                        },
                    ),
                )
                item(key = "license_header") {
                    Text(
                        text = stringResource(R.string.about_license),
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 6.dp),
                    )
                }
                groupedCardItems(
                    keyPrefix = "aboutThirdParty",
                    outerTopPadding = 0.dp,
                    outerBottomPadding = 6.dp,
                    items = listOf(
                        CardItem("kernelsu") {
                            ThirdPartyRowMiuix(
                                title = stringResource(R.string.about_third_party_kernelsu),
                                license = stringResource(R.string.about_license_gpl3),
                            )
                        },
                        CardItem("miuix") {
                            ThirdPartyRowMiuix(
                                title = stringResource(R.string.about_third_party_miuix),
                                license = stringResource(R.string.about_license_apache),
                            )
                        },
                        CardItem("kolor") {
                            ThirdPartyRowMiuix(
                                title = stringResource(R.string.about_third_party_kolor),
                                license = stringResource(R.string.about_license_mit),
                            )
                        },
                        CardItem("googleSans") {
                            ThirdPartyRowMiuix(
                                title = stringResource(R.string.about_third_party_google_sans),
                                license = stringResource(R.string.about_license_ofl),
                            )
                        },
                    ),
                )
                item(key = "credits") {
                    Text(
                        text = stringResource(R.string.about_credits),
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
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
            contentDescription = null,
            tint = colorScheme.onSurface,
        )
    }
}

@Composable
private fun AboutHeaderMiuix(modifier: Modifier = Modifier) {
    val uiMode = LocalUiMode.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                // 新图标前景是白色 M3/TIM 文字，预览底改为深色保证对比度。
                .background(Color(0xFF1C1B1F)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.about_app_name),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.about_version_format,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            ),
            fontSize = 13.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.about_ui_style_format, uiMode.name),
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun ThirdPartyRowMiuix(title: String, license: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        Text(
            text = license,
            fontSize = 13.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
    }
}
