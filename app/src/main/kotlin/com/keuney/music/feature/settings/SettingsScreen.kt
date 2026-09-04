package com.keuney.music.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keuney.music.R
import com.keuney.music.core.settings.CacheLimit
import com.keuney.music.core.settings.ThemePreference
import com.keuney.music.ui.format.formatBytes

/**
 * 설정 화면. PRD가 정한 네 가지만 둔다: 화면 색, 캐시 상한, 캐시 지우기, 재생 기록.
 *
 * WiFi 전용 재생은 KM-137에서 만들어 재생 화면에 임시로 두었던 것을 여기로 옮겼다. 재생 중에
 * 곧바로 적용되는 반복 설정은 재생 화면에 남는다. 그것은 지금 듣는 것을 바꾸는 조작이다.
 *
 * 쓰이지 않는 설정은 두지 않는다. 여기 있는 네 가지는 모두 동작에 닿는다.
 */
@Composable
internal fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val wifiOnly by viewModel.wifiOnlyPlayback.collectAsStateWithLifecycle()
    val historyEnabled by viewModel.historyEnabled.collectAsStateWithLifecycle()
    val cacheLimit by viewModel.cacheLimit.collectAsStateWithLifecycle()
    val cacheUsed by viewModel.cacheUsedBytes.collectAsStateWithLifecycle()
    // 캐시 크기는 알려 오지 않으므로 화면에 들어올 때 한 번 읽는다.
    LaunchedEffect(Unit) { viewModel.refreshCacheUsage() }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.player_back))
            }
            Text(stringResource(R.string.destination_settings), style = MaterialTheme.typography.titleLarge)
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingTitle(R.string.theme_label)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreference.entries.forEach { option ->
                    FilterChip(
                        selected = theme == option,
                        onClick = { viewModel.setTheme(option) },
                        label = { Text(stringResource(themeLabelRes(option))) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SwitchRow(
                labelRes = R.string.wifi_only_label,
                noteRes = R.string.wifi_only_note,
                checked = wifiOnly,
                onCheckedChange = viewModel::setWifiOnlyPlayback,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SwitchRow(
                labelRes = R.string.history_enabled_label,
                noteRes = R.string.history_enabled_note,
                checked = historyEnabled,
                onCheckedChange = viewModel::setHistoryEnabled,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingTitle(R.string.cache_limit_label)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CacheLimit.entries.forEach { option ->
                    FilterChip(
                        selected = cacheLimit == option,
                        onClick = { viewModel.setCacheLimit(option) },
                        label = { Text(formatBytes(option.bytes)) },
                    )
                }
            }
            Note(
                stringResource(
                    R.string.cache_usage,
                    formatBytes(cacheUsed),
                    formatBytes(viewModel.activeCacheLimitBytes),
                ),
            )
            // 고른 값과 지금 걸린 값이 다를 때만 말한다. 늘 붙어 있으면 읽지 않게 된다.
            if (cacheLimit.bytes != viewModel.activeCacheLimitBytes) {
                Note(stringResource(R.string.cache_limit_restart))
            }
            OutlinedButton(onClick = viewModel::clearCache) {
                Text(stringResource(R.string.cache_clear))
            }
            Note(stringResource(R.string.cache_clear_note))
        }
    }
}

@Composable
private fun SettingTitle(titleRes: Int) {
    Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
}

/** 켜고 끄는 설정 한 줄. 무엇이 달라지는지 아래에 한 줄로 말한다. */
@Composable
private fun SwitchRow(
    labelRes: Int,
    noteRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SettingTitle(labelRes)
            Note(stringResource(noteRes))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun themeLabelRes(theme: ThemePreference): Int = when (theme) {
    ThemePreference.System -> R.string.theme_system
    ThemePreference.Light -> R.string.theme_light
    ThemePreference.Dark -> R.string.theme_dark
}
