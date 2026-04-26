package com.example.drinkwatch.ui.session.glassgroups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drinkwatch.viewmodel.SessionViewModel

@Composable
fun GlassGroupsTab(viewModel: SessionViewModel) {
    val glassGroups by viewModel.glassGroups.collectAsStateWithLifecycle()
    val activeLetters = glassGroups.map { it.letter }.toSet()
    val letters = ('A'..'Z').toList()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(72.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(letters) { letter ->
            FilterChip(
                selected = letter in activeLetters,
                onClick = { viewModel.toggleGlassGroup(letter) },
                label = { Text(letter.toString()) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
