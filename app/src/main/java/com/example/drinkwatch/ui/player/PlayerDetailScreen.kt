package com.example.drinkwatch.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.util.formatCountdown
import com.example.drinkwatch.util.formatDuration
import com.example.drinkwatch.util.formatTimestamp
import com.example.drinkwatch.viewmodel.PlayerDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDetailScreen(
    playerId: Long,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication
    // Entry-scoped ViewModel — do NOT use viewModelStoreOwner = activity here.
    val viewModel: PlayerDetailViewModel = viewModel(
        factory = app.playerDetailViewModelFactory(playerId),
    )

    val derivedState          by viewModel.derivedState.collectAsStateWithLifecycle()
    val timeoutMillisRemaining by viewModel.timeoutMillisRemaining.collectAsStateWithLifecycle()
    val activeDrinkHighlight  by viewModel.activeDrinkHighlight.collectAsStateWithLifecycle()
    val orderHistory          by viewModel.orderHistory.collectAsStateWithLifecycle()
    val timeoutHistory        by viewModel.timeoutHistory.collectAsStateWithLifecycle()

    val playerName = derivedState?.player?.name ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playerName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (derivedState == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Player no longer available.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        val state  = derivedState!!
        val player = state.player
        val isUnderTimeout = timeoutMillisRemaining != null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Text(
                text = player.name,
                style = MaterialTheme.typography.headlineLarge,
            )
            if (player.phone.isNotBlank()) {
                Text(
                    text = player.phone,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (player.note.isNotBlank()) {
                Text(
                    text = player.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Stats row ─────────────────────────────────────────────────────
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val drinkHighlightColor =
                        if (state.activeDrinkCount >= activeDrinkHighlight)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.secondary

                    StatCell(
                        label = "Active drinks",
                        value = state.activeDrinkCount.toString(),
                        valueColor = drinkHighlightColor,
                        icon = Icons.Filled.LocalBar,
                        iconColor = drinkHighlightColor,
                    )
                    StatCell(
                        label = "Timeout",
                        value = if (isUnderTimeout)
                            formatCountdown(timeoutMillisRemaining!!)
                        else
                            "—",
                        icon = Icons.Filled.Timer,
                        iconColor = if (isUnderTimeout)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StatCell(
                        label = "Glasses out",
                        value = state.unreturnedGlassCount.toString(),
                        icon = Icons.Filled.WineBar,
                        iconColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            // ── Totals row ────────────────────────────────────────────────────
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatCell(
                        label = "Total drinks",
                        value = state.totalDrinks.toString(),
                        icon = Icons.Filled.LocalBar,
                        iconColor = MaterialTheme.colorScheme.secondary,
                    )
                    StatCell(
                        label = "Total timeout",
                        value = formatDuration(state.totalTimeoutSeconds),
                        icon = Icons.Filled.Schedule,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Drink history ─────────────────────────────────────────────────
            ExpandableEventTable(
                title = "Drinks",
                columns = listOf("Time", "Drink", "Glass"),
                rows = orderHistory.map { item ->
                    listOf(
                        formatTimestamp(item.timestampMs),
                        item.drinkName,
                        if (item.glassGroup != null && item.glassNumber != null)
                            "${item.glassGroup}${item.glassNumber}"
                        else
                            "—",
                    )
                },
            )

            // ── Timeout history ───────────────────────────────────────────────
            ExpandableEventTable(
                title = "Timeouts",
                columns = listOf("Time", "Duration"),
                rows = timeoutHistory.map { item ->
                    listOf(
                        formatTimestamp(item.timestampMs),
                        if (item.durationSeconds == 0) "Cancelled"
                        else formatDuration(item.durationSeconds),
                    )
                },
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
