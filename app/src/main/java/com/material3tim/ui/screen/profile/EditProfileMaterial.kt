package com.material3tim.ui.screen.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.repository.ChatRepository
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SegmentedColumn
import com.material3tim.ui.component.material.SegmentedDropdownItem
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.SegmentedTextField
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/**
 * 「编辑资料」页（Material 3 Expressive 风格）。
 * 昵称 / 个性签名 / 性别 / 生日 / 地区均为本地状态，仅用于示例展示。
 */
@Composable
internal fun EditProfileMaterial(onBack: () -> Unit) {
    val context = LocalContext.current
    val profile = ChatRepository.profile
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val savedMessage = stringResource(R.string.edit_profile_saved)
    val doneLabel = stringResource(R.string.done)
    val nicknameLabel = stringResource(R.string.edit_profile_nickname)
    val signatureLabel = stringResource(R.string.edit_profile_signature)
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.edit_profile)) },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                actions = {
                    TextButton(
                        onClick = {
                            Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                    ) {
                        Text(doneLabel)
                    }
                },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            TonalCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = showComingSoon,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProfileAvatar(
                        name = nickname,
                        avatarSeed = profile.avatarSeed,
                        isOnline = profile.isOnline,
                        size = 72.dp,
                        avatarPath = profile.avatarPath,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.edit_profile_avatar),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.edit_profile_avatar_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SegmentedColumn(
                content = listOf(
                    {
                        SegmentedTextField(
                            label = nicknameLabel,
                            value = nickname,
                            onValueChange = { nickname = it },
                            singleLine = true,
                            placeholder = { Text(nicknameLabel) },
                        )
                    },
                    {
                        SegmentedTextField(
                            label = signatureLabel,
                            value = signature,
                            onValueChange = { signature = it },
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.profile_signature)) },
                        )
                    },
                    {
                        SegmentedDropdownItem(
                            title = stringResource(R.string.edit_profile_gender),
                            items = genderItems,
                            selectedIndex = genderIndex,
                            onItemSelected = { genderIndex = it },
                        )
                    },
                )
            )

            SegmentedColumn(
                content = listOf(
                    {
                        EditProfileInfoRowMaterial(
                            title = stringResource(R.string.edit_profile_birthday),
                            value = birthdayValue,
                            onClick = showComingSoon,
                        )
                    },
                    {
                        EditProfileInfoRowMaterial(
                            title = stringResource(R.string.edit_profile_region),
                            value = regionValue,
                            onClick = showComingSoon,
                        )
                    },
                )
            )

            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                )
            )
        }
    }
}

@Composable
private fun EditProfileInfoRowMaterial(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        headlineContent = { Text(title) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                )
            }
        },
    )
}
