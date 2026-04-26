package com.example.drinkwatch.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.drinkwatch.ui.dialog.TimeoutDialog
import com.example.drinkwatch.viewmodel.PlayerUiState

@Composable
fun OrderTab(
    playerUiStates: List<PlayerUiState>,
    activeDrinkHighlight: Int,
    defaultTimeoutSeconds: Int,
    onNavigateToOrderDialog: (Long) -> Unit,
    onNavigateToPlayerDetail: (Long) -> Unit,
    onStartTimeout: (Long, Int) -> Unit,
    onCancelQueuedOrder: (Long) -> Unit,
    onCommitQueue: () -> Unit,
    queueSize: Int,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var timeoutDialogPlayerId by remember { mutableStateOf<Long?>(null) }

    Box(modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = if (queueSize > 0) 88.dp else 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(playerUiStates, key = { it.derived.player.id }) { uiState ->
                PlayerCard(
                    playerUiState = uiState,
                    activeDrinkHighlight = activeDrinkHighlight,
                    onAddDrink = {
                        if (uiState.queuedOrder != null) {
                            onShowSnackbar("Cancel or send the queue first.")
                        } else {
                            onNavigateToOrderDialog(uiState.derived.player.id)
                        }
                    },
                    onTimeout = { timeoutDialogPlayerId = uiState.derived.player.id },
                    onCardClick = { onNavigateToPlayerDetail(uiState.derived.player.id) },
                    onCancelOrder = { onCancelQueuedOrder(uiState.derived.player.id) },
                )
            }
        }

        if (queueSize > 0) {
            ExtendedFloatingActionButton(
                onClick = onCommitQueue,
                icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                text = { Text("Send ($queueSize)") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }

    timeoutDialogPlayerId?.let { playerId ->
        TimeoutDialog(
            defaultSeconds = defaultTimeoutSeconds,
            onDismiss = { timeoutDialogPlayerId = null },
            onConfirm = { seconds ->
                onStartTimeout(playerId, seconds)
                timeoutDialogPlayerId = null
            },
        )
    }
}
