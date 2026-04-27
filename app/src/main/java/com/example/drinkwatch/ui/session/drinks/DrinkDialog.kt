package com.example.drinkwatch.ui.session.drinks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.drinkwatch.data.model.Drink
import com.example.drinkwatch.data.model.DrinkType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrinkDialog(
    drink: Drink?,
    onSave: (name: String, type: DrinkType) -> Unit,
    onDelete: () -> Unit,
    onToggleDisabled: () -> Unit = {},
    onDismiss: () -> Unit,
){
    var name by rememberSaveable { mutableStateOf(drink?.name ?: "") }
    var selectedType by rememberSaveable { mutableStateOf(drink?.type ?: DrinkType.LONG_DRINK) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val drinkTypeOptions = listOf(
        DrinkType.SHOT          to "Shot",
        DrinkType.LONG_DRINK    to "Long drink",
        DrinkType.NON_ALCOHOLIC to "Non-alcoholic",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (drink != null) Icons.Filled.Edit else Icons.Filled.LocalBar,
                                contentDescription = null,
                            )
                            Text(if (drink != null) "Edit Drink" else "Add Drink")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onSave(name.trim(), selectedType) },
                            enabled = name.isNotBlank(),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Save")
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    leadingIcon = { Icon(Icons.Filled.LocalBar, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Type", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    drinkTypeOptions.forEachIndexed { index, (type, label) ->
                        SegmentedButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            shape = SegmentedButtonDefaults.itemShape(index, drinkTypeOptions.size),
                        ) { Text(label) }
                    }
                }

                if (drink != null) {
                    Spacer(Modifier.height(8.dp))
                    if (drink.isDisabled) {
                        OutlinedButton(
                            onClick = {
                                onToggleDisabled()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Enable Drink")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                onToggleDisabled()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Disable Drink")
                        }
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Delete Drink")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Drink?") },
            text = { Text("This will permanently remove ${drink?.name}.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
