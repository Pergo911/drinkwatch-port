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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.ui.session.players.PlayerDialog
import com.example.drinkwatch.ui.theme.Dimens
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

    val derivedState by viewModel.derivedState.collectAsStateWithLifecycle()
    val timeoutMillisRemaining by viewModel.timeoutMillisRemaining.collectAsStateWithLifecycle()
    val activeDrinkHighlight by viewModel.activeDrinkHighlight.collectAsStateWithLifecycle()
    val orderHistory by viewModel.orderHistory.collectAsStateWithLifecycle()
    val timeoutHistory by viewModel.timeoutHistory.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }

    val playerName = derivedState?.player?.name ?: ""

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Player Details",
                            style = MaterialTheme.typography.titleLarge
                        ); if (playerName.isNotBlank()) Text(
                        playerName,
                        style = MaterialTheme.typography.labelMedium
                    )
                    }
                },

                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditDialog = true },
                        enabled = derivedState != null,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit player",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
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

        val state = derivedState!!
        val player = state.player
        val isUnderTimeout = timeoutMillisRemaining != null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = Dimens.DetailHorizontalPadding,
                    vertical = Dimens.DetailVerticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box {
                    Surface(
                        shape = CircleShape,
                        color = if (player.isDisabled)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(128.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = if (player.isDisabled)
                                    MaterialTheme.colorScheme.onErrorContainer
                                else
                                    MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    if (player.isDisabled) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.BottomEnd),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onError,
                                )
                            }
                        }
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.displayMedium,
                    )
                    if (player.isDisabled) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    text = "Disabled",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }

            /* Info card, shown if either phone or note is available */
            if (player.phone.isNotBlank() || player.note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Info",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = player.phone.ifBlank { "---" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = player.note.ifBlank { "---" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // ── Stats row ─────────────────────────────────────────────────────
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = Dimens.ScreenHorizontalPadding,
                            vertical = Dimens.DetailVerticalPadding
                        ),
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
                        icon = Icons.Filled.Liquor,
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
                        icon = Icons.Filled.LocalBar,
                        iconColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            // ── Totals row ────────────────────────────────────────────────────
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = Dimens.ScreenHorizontalPadding,
                            vertical = Dimens.DetailVerticalPadding
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatCell(
                        label = "Total drinks",
                        value = state.totalDrinks.toString(),
                        icon = Icons.Filled.Liquor,
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
                        if (item.glassGroup != null && item.glassNumber != null) {
                            val label = "${item.glassGroup}${item.glassNumber}"
                            if (item.glassReturned == false) "$label (out)" else label
                        } else "—",
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

    if (showEditDialog) {
        PlayerDialog(
            player = derivedState?.player,
            onSave = { name, phone, note, isDisabled ->
                viewModel.savePlayer(name, phone, note, isDisabled)
                showEditDialog = false
            },
            onDelete = {
                viewModel.deletePlayer()
                showEditDialog = false
                onBack()
            },
            onDismiss = { showEditDialog = false },
        )
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
            modifier = Modifier.size(28.dp),
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
