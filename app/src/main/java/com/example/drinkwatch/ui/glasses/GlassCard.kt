package com.example.drinkwatch.ui.glasses

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        ListItem(
            headlineContent = { Text("${uiState.glassGroup}${uiState.glassNumber}") },
            supportingContent = { Text(uiState.drinkName) },
            trailingContent = { Text(uiState.playerName) },
        )
    }
}
