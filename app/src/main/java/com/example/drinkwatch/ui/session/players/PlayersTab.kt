package com.example.drinkwatch.ui.session.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drinkwatch.data.model.Player
import com.example.drinkwatch.viewmodel.SessionViewModel

@Composable
fun PlayersTab(viewModel: SessionViewModel) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(players, key = { it.id }) { player ->
                PlayerListItem(
                    player = player,
                    onEdit = { editingPlayer = player },
                )
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Player")
        }
    }

    if (showAddDialog) {
        PlayerDialog(
            player = null,
            onSave = { name, phone, note ->
                viewModel.addPlayer(name, phone, note)
                showAddDialog = false
            },
            onDelete = {},
            onDismiss = { showAddDialog = false },
        )
    }

    editingPlayer?.let { player ->
        PlayerDialog(
            player = player,
            onSave = { name, phone, note ->
                viewModel.updatePlayer(player.copy(name = name, phone = phone, note = note))
                editingPlayer = null
            },
            onDelete = {
                viewModel.deletePlayer(player)
                editingPlayer = null
            },
            onDismiss = { editingPlayer = null },
        )
    }
}

@Composable
private fun PlayerListItem(
    player: Player,
    onEdit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(player.name, style = MaterialTheme.typography.bodyLarge)
                if (player.phone.isNotBlank()) {
                    Text(player.phone, style = MaterialTheme.typography.bodyMedium)
                }
                if (player.note.isNotBlank()) {
                    Text(
                        player.note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
        }
    }
}
