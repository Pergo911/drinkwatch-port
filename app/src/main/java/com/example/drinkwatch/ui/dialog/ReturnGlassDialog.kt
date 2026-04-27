package com.example.drinkwatch.ui.dialog

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.drinkwatch.viewmodel.TakenGlassUiState

@Composable
fun ReturnGlassDialog(
    glass: TakenGlassUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = { Text("Return Glass?") },
        text = {
            Text(
                "Return glass ${glass.glassGroup}${glass.glassNumber} " +
                    "taken by ${glass.playerName}?"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Return") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
