package com.example.drinkwatch.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // ── Avatar ──────────────────────────────────────────────────────
            PlayerAvatar(name = player.name)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // ── Name + status ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    when {
                        player.isDisabled -> SuggestionChip(
                            onClick = {},
                            label = { Text("Disabled") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        )
                        isTimeout -> AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    formatCountdown(playerUiState.timeoutMillisRemaining ?: 0L),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor     = MaterialTheme.colorScheme.onErrorContainer,
                                leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        )
                        else -> {
                            val drinkHighlight = derived.activeDrinkCount >= activeDrinkHighlight
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "${derived.activeDrinkCount} 🍹",
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (drinkHighlight)
                                        MaterialTheme.colorScheme.errorContainer
                                    else
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = if (drinkHighlight)
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            )
                        }
                    }
                }

                // ── Action area ────────────────────────────────────────────
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
                                Icon(
                                    Icons.Filled.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text("Add Drink")
                            }
                            OutlinedButton(onClick = onTimeout) {
                                Icon(
                                    Icons.Filled.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text("Timeout")
                            }
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

@Composable
fun PlayerAvatar(
    name: String,
    modifier: Modifier = Modifier,
) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}
