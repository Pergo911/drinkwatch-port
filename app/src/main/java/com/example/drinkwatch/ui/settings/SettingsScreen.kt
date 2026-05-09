package com.example.drinkwatch.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.R
import com.example.drinkwatch.data.model.Theme
import com.example.drinkwatch.ui.dialog.HighlightThresholdDialog
import com.example.drinkwatch.ui.dialog.TimeoutDialog
import com.example.drinkwatch.ui.theme.Dimens
import com.example.drinkwatch.util.formatDuration
import com.example.drinkwatch.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication
    val viewModel: SettingsViewModel = viewModel(factory = app.settingsViewModelFactory)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val durationFormat = stringResource(R.string.duration_format)

    var showHighlightDialog by rememberSaveable { mutableStateOf(false) }
    var showTimeoutDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(
                    horizontal = Dimens.ScreenHorizontalPadding,
                    vertical = Dimens.ScreenVerticalPadding
                ),
        ) {
            // ── Appearance ─────────────────────────────────────────────────────
            SettingsSectionHeader(icon = Icons.Filled.DarkMode, title = stringResource(R.string.settings_appearance))
            SettingsPreferenceGroup {
                // Theme
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_theme_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    ) {
                        listOf(Theme.LIGHT to stringResource(R.string.settings_theme_light), Theme.SYSTEM to stringResource(R.string.settings_theme_system), Theme.DARK to stringResource(R.string.settings_theme_dark))
                            .forEachIndexed { index, (theme, label) ->
                                SegmentedButton(
                                    selected = settings.theme == theme,
                                    onClick = { viewModel.setTheme(theme) },
                                    shape = SegmentedButtonDefaults.itemShape(index, 3),
                                ) { Text(label) }
                            }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                // Language
                val currentLocales = AppCompatDelegate.getApplicationLocales()
                val currentLang = if (currentLocales.isEmpty) "system" else currentLocales[0]?.language ?: "system"
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_language_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    ) {
                        listOf(
                            "system" to stringResource(R.string.settings_language_system),
                            "en"     to stringResource(R.string.settings_language_english),
                            "hu"     to stringResource(R.string.settings_language_hungarian),
                        ).forEachIndexed { index, (tag, label) ->
                            SegmentedButton(
                                selected = currentLang == tag,
                                onClick = {
                                    val localeList = if (tag == "system") LocaleListCompat.getEmptyLocaleList()
                                                     else LocaleListCompat.forLanguageTags(tag)
                                    AppCompatDelegate.setApplicationLocales(localeList)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, 3),
                            ) { Text(label) }
                        }
                    }
                }
            }

            // ── Gameplay ───────────────────────────────────────────────────────
            SettingsSectionHeader(icon = Icons.Filled.Tune, title = stringResource(R.string.settings_gameplay))
            SettingsPreferenceGroup {
                SettingsPreferenceRow(
                    title = stringResource(R.string.settings_highlight_title),
                    subtitle = stringResource(R.string.settings_highlight_subtitle),
                    value = settings.activeDrinkHighlight.toString(),
                    onClick = { showHighlightDialog = true },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsPreferenceRow(
                    title = stringResource(R.string.settings_timeout_title),
                    subtitle = stringResource(R.string.settings_timeout_subtitle),
                    value = formatDuration(settings.defaultTimeoutSeconds, durationFormat),
                    onClick = { showTimeoutDialog = true },
                )
            }
        }
    }

    if (showHighlightDialog) {
        HighlightThresholdDialog(
            current = settings.activeDrinkHighlight,
            onDismiss = { showHighlightDialog = false },
            onConfirm = { count ->
                viewModel.setActiveDrinkHighlight(count)
                showHighlightDialog = false
            },
        )
    }
    if (showTimeoutDialog) {
        TimeoutDialog(
            defaultSeconds = settings.defaultTimeoutSeconds,
            onDismiss = { showTimeoutDialog = false },
            onConfirm = { seconds ->
                viewModel.setDefaultTimeoutSeconds(seconds)
                showTimeoutDialog = false
            },
        )
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SettingsPreferenceGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsPreferenceRow(
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}