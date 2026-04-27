package com.example.drinkwatch.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.example.drinkwatch.util.formatCountdown
import com.example.drinkwatch.viewmodel.PlayerUiState

@Composable
fun PlayerCard(
    playerUiState: PlayerUiState,
    activeDrinkHighlight: Int,
    onAddDrink: () -> Unit,
    onTimeout: () -> Unit,
    onCardClick: () -> Unit,
    onCancelOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val derived   = playerUiState.derived
    val player    = derived.player
    val isTimeout = playerUiState.isUnderTimeout
    val queued    = playerUiState.queuedOrder

    val cardModifier = modifier
        .fillMaxWidth()
        .alpha(if (player.isDisabled) 0.38f else 1f)

    val cardContent: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Header: name + status badge ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                when {
                    isTimeout -> Text(
                        text = formatCountdown(playerUiState.timeoutMillisRemaining ?: 0L),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    !player.isDisabled -> {
                        val countColor = if (derived.activeDrinkCount >= activeDrinkHighlight) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            text = derived.activeDrinkCount.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = countColor,
                        )
                    }
                }
            }

            // ── Action area ───────────────────────────────────────────────
            if (!player.isDisabled) {
                Spacer(Modifier.height(8.dp))
                if (queued != null) {
                    val glassLabel = if (queued.glassGroup != null && queued.glassNumber != null) {
                        "${queued.glassGroup}${queued.glassNumber}"
                    } else null
                    QueueOverlay(
                        drinkName  = playerUiState.queuedDrinkName ?: "",
                        glassLabel = glassLabel,
                        onCancel   = onCancelOrder,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAddDrink,
                            enabled = !isTimeout,
                        ) {
                            Text("Add Drink")
                        }
                        OutlinedButton(onClick = onTimeout) {
                            Text("Timeout")
                        }
                    }
                }
            }
        }
    }

    if (player.isDisabled) {
        ElevatedCard(modifier = cardModifier) { cardContent() }
    } else {
        ElevatedCard(onClick = onCardClick, modifier = cardModifier) { cardContent() }
    }
}
