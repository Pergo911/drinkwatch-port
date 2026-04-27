package com.example.drinkwatch.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.data.model.Theme
import com.example.drinkwatch.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication
    val viewModel: SettingsViewModel = viewModel(factory = app.settingsViewModelFactory)
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Initialised once at composition from the StateFlow's current value. Plain rememberSaveable
    // (no key) avoids jank from intermediate DataStore saves resetting text during typing.
    var highlightText by rememberSaveable {
        mutableStateOf(settings.activeDrinkHighlight.toString())
    }
    var timeoutHoursText by rememberSaveable {
        mutableStateOf((settings.defaultTimeoutSeconds / 3600).toString())
    }
    var timeoutMinutesText by rememberSaveable {
        mutableStateOf(((settings.defaultTimeoutSeconds % 3600) / 60).toString())
    }

    val highlightError  = highlightText.toIntOrNull()?.let { it < 1 } ?: true
    val timeoutMinutes  = timeoutMinutesText.toIntOrNull() ?: 0
    val timeoutMinError = timeoutMinutes > 59

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // ── Theme ──────────────────────────────────────────────────────────
            SectionHeader(icon = Icons.Filled.DarkMode, title = "Theme")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(Theme.LIGHT to "Light", Theme.SYSTEM to "System", Theme.DARK to "Dark")
                    .forEachIndexed { index, (theme, label) ->
                        SegmentedButton(
                            selected = settings.theme == theme,
                            onClick  = { viewModel.setTheme(theme) },
                            shape    = SegmentedButtonDefaults.itemShape(index, 3),
                        ) {
                            Text(label)
                        }
                    }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Active drink highlight ─────────────────────────────────────────
            SectionHeader(icon = Icons.Filled.Notifications, title = "Active Drink Highlight")
            OutlinedTextField(
                value = highlightText,
                onValueChange = { new ->
                    highlightText = new.filter(Char::isDigit).trimStart('0').ifEmpty { "0" }
                    highlightText.toIntOrNull()?.takeIf { it >= 1 }
                        ?.let { viewModel.setActiveDrinkHighlight(it) }
                },
                label           = { Text("Threshold") },
                supportingText  = {
                    if (highlightError) Text("Must be \u2265 1")
                    else Text("Highlight players with \u2265 N active drinks")
                },
                isError         = highlightError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Default timeout ────────────────────────────────────────────────
            SectionHeader(icon = Icons.Filled.Timer, title = "Default Timeout")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = timeoutHoursText,
                    onValueChange = { new ->
                        timeoutHoursText = new.filter(Char::isDigit).trimStart('0').ifEmpty { "0" }
                        val h = timeoutHoursText.toIntOrNull() ?: 0
                        val m = timeoutMinutesText.toIntOrNull() ?: 0
                        if (m <= 59) viewModel.setDefaultTimeoutSeconds(h * 3600 + m * 60)
                    },
                    label           = { Text("Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine      = true,
                    modifier        = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = timeoutMinutesText,
                    onValueChange = { new ->
                        timeoutMinutesText = new.filter(Char::isDigit).trimStart('0').ifEmpty { "0" }
                        val h = timeoutHoursText.toIntOrNull() ?: 0
                        val m = timeoutMinutesText.toIntOrNull() ?: 0
                        if (m <= 59) viewModel.setDefaultTimeoutSeconds(h * 3600 + m * 60)
                    },
                    label           = { Text("Minutes") },
                    isError         = timeoutMinError,
                    supportingText  = if (timeoutMinError) { { Text("0\u201359") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine      = true,
                    modifier        = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}