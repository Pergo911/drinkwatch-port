package com.example.drinkwatch.ui.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.drinkwatch.ui.component.GlassNumberPicker

@Composable
fun GlassNumberPickerDialog(
    initialNumber: Int,
    takenNumbers: Set<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var pickedNumber by remember { mutableIntStateOf(initialNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.LocalBar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = { Text("Select Glass Number") },
        text = {
            GlassNumberPicker(
                selectedNumber = pickedNumber,
                takenNumbers = takenNumbers,
                onNumberChange = { pickedNumber = it },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickedNumber) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
