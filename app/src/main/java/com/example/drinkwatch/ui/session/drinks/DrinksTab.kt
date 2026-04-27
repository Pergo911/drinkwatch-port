package com.example.drinkwatch.ui.session.drinks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drinkwatch.data.model.Drink
import com.example.drinkwatch.data.model.DrinkType
import com.example.drinkwatch.viewmodel.SessionViewModel

@Composable
fun DrinksTab(viewModel: SessionViewModel) {
    val drinks by viewModel.drinks.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingDrink by remember { mutableStateOf<Drink?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (drinks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalBar,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No drinks yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(drinks, key = { it.id }) { drink ->
                    DrinkListItem(
                        drink = drink,
                        onEdit = { editingDrink = drink },
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Drink")
        }
    }

    if (showAddDialog) {
        DrinkDialog(
            drink = null,
            onSave = { name, type ->
                viewModel.addDrink(name, type)
                showAddDialog = false
            },
            onDelete = {},
            onDismiss = { showAddDialog = false },
        )
    }

    editingDrink?.let { drink ->
        DrinkDialog(
            drink = drink,
            onSave = { name, type ->
                viewModel.updateDrink(drink.copy(name = name, type = type))
                editingDrink = null
            },
            onDelete = {
                viewModel.deleteDrink(drink)
                editingDrink = null
            },
            onToggleDisabled = {
                if (drink.isDisabled) viewModel.enableDrink(drink) else viewModel.disableDrink(drink)
                editingDrink = null
            },
            onDismiss = { editingDrink = null },
        )
    }
}

private fun drinkTypeIcon(type: DrinkType): ImageVector = when (type) {
    DrinkType.SHOT          -> Icons.Filled.LocalDrink
    DrinkType.LONG_DRINK    -> Icons.Filled.SportsBar
    DrinkType.NON_ALCOHOLIC -> Icons.Filled.WaterDrop
}

@Composable
private fun DrinkListItem(
    drink: Drink,
    onEdit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().alpha(if (drink.isDisabled) 0.38f else 1f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Drink type icon circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Icon(
                    imageVector = drinkTypeIcon(drink.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(drink.name, style = MaterialTheme.typography.bodyLarge)
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            when (drink.type) {
                                DrinkType.SHOT -> "Shot"
                                DrinkType.LONG_DRINK -> "Long drink"
                                DrinkType.NON_ALCOHOLIC -> "Non-alcoholic"
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        labelColor     = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    modifier = Modifier.height(24.dp),
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
        }
    }
}
