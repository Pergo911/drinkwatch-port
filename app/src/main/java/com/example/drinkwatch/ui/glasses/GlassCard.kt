package com.example.drinkwatch.ui.glasses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.drinkwatch.viewmodel.TakenGlassUiState

@Composable
fun GlassCard(
    uiState: TakenGlassUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.LocalBar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp),
            )

            Spacer(Modifier.width(8.dp))

            // Glass text badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
            ) {

                Text(
                    text = "${uiState.glassGroup}${uiState.glassNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.drinkName,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = uiState.playerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Return glass",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
