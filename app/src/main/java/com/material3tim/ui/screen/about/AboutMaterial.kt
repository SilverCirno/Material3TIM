package com.material3tim.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.material3tim.BuildConfig
import com.material3tim.R
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SegmentedColumn
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

@Composable
fun AboutMaterial(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val githubUrl = stringResource(R.string.about_github_url)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                AboutHeaderMaterial()
            }
            item(key = "description") {
                TonalCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.about_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
            item(key = "github") {
                SegmentedColumn(
                    content = listOf(
                        {
                            SegmentedListItem(
                                onClick = { uriHandler.openUri(githubUrl) },
                                headlineContent = { Text(stringResource(R.string.about_github)) },
                                supportingContent = { Text(githubUrl) },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                    )
                                },
                            )
                        },
                    )
                )
            }
            item(key = "license_header") {
                Text(
                    text = stringResource(R.string.about_license),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                )
            }
            item(key = "third_party") {
                SegmentedColumn(
                    content = listOf(
                        {
                            ThirdPartyItemMaterial(
                                title = stringResource(R.string.about_third_party_kernelsu),
                                license = stringResource(R.string.about_license_gpl3),
                            )
                        },
                        {
                            ThirdPartyItemMaterial(
                                title = stringResource(R.string.about_third_party_miuix),
                                license = stringResource(R.string.about_license_apache),
                            )
                        },
                        {
                            ThirdPartyItemMaterial(
                                title = stringResource(R.string.about_third_party_kolor),
                                license = stringResource(R.string.about_license_mit),
                            )
                        },
                        {
                            ThirdPartyItemMaterial(
                                title = stringResource(R.string.about_third_party_google_sans),
                                license = stringResource(R.string.about_license_ofl),
                            )
                        },
                    )
                )
            }
            item(key = "credits") {
                Text(
                    text = stringResource(R.string.about_credits),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AboutHeaderMaterial() {
    val uiMode = LocalUiMode.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp),
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
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.about_version_format,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.about_ui_style_format, uiMode.name),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThirdPartyItemMaterial(title: String, license: String) {
    SegmentedListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(license) },
    )
}
