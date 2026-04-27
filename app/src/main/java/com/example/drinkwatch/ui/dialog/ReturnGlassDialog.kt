package com.example.drinkwatch.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.drinkwatch.viewmodel.TakenGlassUiState

@Composable
fun ReturnGlassDialog(
    glass: TakenGlassUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
