package com.example.drinkwatch.ui.session.overview

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drinkwatch.data.model.Session
import com.example.drinkwatch.viewmodel.SessionViewModel

@Composable
fun OverviewTab(viewModel: SessionViewModel) {
    val session by viewModel.currentSession.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showNewSessionConfirm by rememberSaveable { mutableStateOf(false) }
    var showImportConfirm by rememberSaveable { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.let { stream ->
                viewModel.importSession(stream)
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                viewModel.exportSession(stream)
            }
        }
    }

    when (val s = session) {
        null -> NoSessionContent(
            onImport = { importLauncher.launch(arrayOf("application/json")) },
            onCreateNew = { showCreateDialog = true },
        )
        else -> SessionLoadedContent(
            session = s,
            onExport = { exportLauncher.launch("${s.name}.json") },
            onImport = { showImportConfirm = true },
            onStartNew = { showNewSessionConfirm = true },
            onNameChange = { viewModel.updateSessionName(it) },
        )
    }

    if (showCreateDialog) {
        NameInputDialog(
            title = "Create New Session",
            label = "Session name",
            confirmText = "Create",
            onConfirm = { name ->
                viewModel.createSession(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Replace Current Session?") },
            text = { Text("Importing will permanently replace the current session and all its data.") },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showNewSessionConfirm) {
        AlertDialog(
            onDismissRequest = { showNewSessionConfirm = false },
            title = { Text("Start New Session?") },
            text = { Text("All current session data will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showNewSessionConfirm = false
                    showCreateDialog = true
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showNewSessionConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun NoSessionContent(
    onImport: () -> Unit,
    onCreateNew: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("Import From File")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCreateNew, modifier = Modifier.fillMaxWidth()) {
            Text("Create New")
        }
    }
}

@Composable
private fun SessionLoadedContent(
    session: Session,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onStartNew: () -> Unit,
    onNameChange: (String) -> Unit,
) {
    var name by rememberSaveable(session.id) { mutableStateOf(session.name) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Session Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        val trimmed = name.trim()
                        when {
                            trimmed.isBlank() -> name = session.name
                            trimmed != session.name -> onNameChange(trimmed)
                        }
                    }
                },
        )
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Text("Export to File")
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("Import From File")
        }
        OutlinedButton(onClick = onStartNew, modifier = Modifier.fillMaxWidth()) {
            Text("Start New Session")
        }
    }
}

@Composable
private fun NameInputDialog(
    title: String,
    label: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
