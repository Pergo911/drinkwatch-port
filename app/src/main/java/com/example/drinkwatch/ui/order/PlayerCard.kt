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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.drinkwatch.R
import com.example.drinkwatch.util.formatCountdown
import com.example.drinkwatch.viewmodel.PlayerUiState

@Composable
fun PlayerCard(
    playerUiState: PlayerUiState,
    activeDrinkHighlight: Int,
    onAddDrink: () -> Unit,
    onTimeout: () -> Unit,
    onClearTimeout: () -> Unit,
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
        .alpha(if (player.isDisabled) 0.5f else 1f)

    val cardContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // ── Avatar ──────────────────────────────────────────────────────
            PlayerAvatar()

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
                        style = MaterialTheme.typography.titleLarge.copy(
                            textDecoration = if (player.isDisabled) TextDecoration.LineThrough else null
                        ),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    when {
                        player.isDisabled -> SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.player_status_disabled)) },
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            Icons.Filled.Liquor,
                                            contentDescription = null,
                                            modifier = Modifier.size(ButtonDefaults.IconSize),
                                        )
                                        Text(
                                            "${derived.activeDrinkCount}",
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        if (isTimeout) {
                            OutlinedButton(
                                onClick = onClearTimeout,
                            ) {
                                Icon(
                                    Icons.Filled.TimerOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.btn_clear_timeout))
                            }
                        } else {
                            OutlinedButton(
                                onClick = onTimeout,
                                enabled = !player.isDisabled,
                            ) {
                                Icon(
                                    Icons.Filled.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.btn_timeout))
                            }
                        }
                        Button(
                            onClick = onAddDrink,
                            enabled = !isTimeout && !player.isDisabled,
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.btn_drink))
                        }
                    }
                }
            }
        }
    }

    ElevatedCard(onClick = onCardClick, modifier = cardModifier) { cardContent() }
}

@Composable
fun PlayerAvatar(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(26.dp),
        )
    }
}
