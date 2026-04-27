package com.example.drinkwatch.ui.glasses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.drinkwatch.ui.dialog.ReturnGlassDialog
import com.example.drinkwatch.viewmodel.TakenGlassUiState

@Composable
fun GlassesTab(
    takenGlasses: List<TakenGlassUiState>,
    onReturnGlass: (Char, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var returnTarget by remember { mutableStateOf<TakenGlassUiState?>(null) }

    if (takenGlasses.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No glasses out.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(
                items = takenGlasses,
                key = { "${it.glassGroup}${it.glassNumber}" },
            ) { uiState ->
                GlassCard(
                    uiState = uiState,
                    onClick = { returnTarget = uiState },
                )
            }
        }
    }

    returnTarget?.let { target ->
        ReturnGlassDialog(
            glass = target,
            onConfirm = {
                onReturnGlass(target.glassGroup, target.glassNumber)
                returnTarget = null
            },
            onDismiss = { returnTarget = null },
        )
    }
}
