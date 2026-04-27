package com.example.drinkwatch.ui.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.data.model.Drink
import com.example.drinkwatch.data.model.DrinkType
import com.example.drinkwatch.ui.component.GlassNumberPicker
import com.example.drinkwatch.util.findActivity
import com.example.drinkwatch.viewmodel.MainViewModel
import com.example.drinkwatch.viewmodel.SessionViewModel

private enum class Step { DRINK, GLASS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDialogContent(
    playerId: Long,
    onDismiss: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication
    val activity = LocalContext.current.findActivity()

    // Entry-scoped: purely reactive, no shared state required.
    val sessionVm: SessionViewModel = viewModel(factory = app.sessionViewModelFactory)
    val drinks by sessionVm.drinks.collectAsStateWithLifecycle()
    val glassGroups by sessionVm.glassGroups.collectAsStateWithLifecycle()

    // Activity-scoped: shares the in-memory queue with MainScreen.
    val mainVm: MainViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = app.mainViewModelFactory,
    )
    val takenGlasses by mainVm.takenGlasses.collectAsStateWithLifecycle()
    val queue by mainVm.queue.collectAsStateWithLifecycle()

    var step by remember { mutableStateOf(Step.DRINK) }
    var selectedDrinkId by remember { mutableStateOf<Long?>(null) }
    var selectedGlassGroup by remember { mutableStateOf<Char?>(null) }
    var selectedGlassNumber by remember { mutableIntStateOf(1) }

    // System back: Step 2 → Step 1; Step 1 → dismiss (handled by Nav3 default).
    BackHandler(enabled = step == Step.GLASS) {
        step = Step.DRINK
    }

    // ── Derived validation ────────────────────────────────────────────────────

    // Resolve against live list so buttons disable if the drink is removed/disabled mid-session.
    val selectedDrink = drinks.firstOrNull { it.id == selectedDrinkId && !it.isDisabled }

    val takenGlassNumbersForGroup: Set<Int> = if (selectedGlassGroup != null) {
        (takenGlasses.filter { it.glassGroup == selectedGlassGroup }.map { it.glassNumber } +
            queue.filter { it.glassGroup == selectedGlassGroup && it.playerId != playerId }
                .mapNotNull { it.glassNumber })
            .toSet()
    } else emptySet()

    val glassIsAlreadyTaken = selectedGlassGroup != null && selectedGlassNumber in takenGlassNumbersForGroup

    // Confirm is allowed when the drink is valid and either no glass is selected, or the
    // selected glass number is not already taken.
    val canConfirm = selectedDrink != null && (selectedGlassGroup == null || !glassIsAlreadyTaken)

    // ── Layout ────────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (step == Step.DRINK) "Choose Drink" else "Choose Glass")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step == Step.GLASS) step = Step.DRINK else onDismiss()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        bottomBar = {
            when (step) {
                Step.DRINK ->
                    Button(
                        onClick = { step = Step.GLASS },
                        enabled = selectedDrink != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text("Next") }

                Step.GLASS ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Button(
                            onClick = {
                                mainVm.addToQueue(
                                    playerId,
                                    selectedDrink!!.id,
                                    selectedGlassGroup,
                                    if (selectedGlassGroup != null) selectedGlassNumber else null,
                                )
                                onDismiss()
                            },
                            enabled = canConfirm,
                            modifier = Modifier.weight(1f),
                        ) { Text("Queue") }
                        OutlinedButton(
                            onClick = {
                                mainVm.commitOrderNow(
                                    playerId,
                                    selectedDrink!!.id,
                                    selectedGlassGroup,
                                    if (selectedGlassGroup != null) selectedGlassNumber else null,
                                )
                                onDismiss()
                            },
                            enabled = canConfirm,
                        ) { Text("Confirm Now") }
                    }
            }
        },
    ) { innerPadding ->
        when (step) {
            Step.DRINK ->
                DrinkPickerContent(
                    drinks = drinks,
                    selectedDrinkId = selectedDrinkId,
                    onSelectDrink = { selectedDrinkId = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )

            Step.GLASS ->
                GlassPickerContent(
                    glassGroupLetters = glassGroups.map { it.letter },
                    selectedGlassGroup = selectedGlassGroup,
                    onSelectGlassGroup = { letter ->
                        selectedGlassGroup = letter
                        if (letter != null) {
                            val takenForGroup = (
                                takenGlasses.filter { it.glassGroup == letter }.map { it.glassNumber } +
                                    queue.filter { it.glassGroup == letter && it.playerId != playerId }
                                        .mapNotNull { it.glassNumber }
                            ).toSet()
                            selectedGlassNumber = (1..99).firstOrNull { it !in takenForGroup } ?: 1
                        } else {
                            selectedGlassNumber = 1
                        }
                    },
                    selectedGlassNumber = selectedGlassNumber,
                    onGlassNumberChange = { selectedGlassNumber = it },
                    takenGlassNumbersForGroup = takenGlassNumbersForGroup,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
        }
    }
}

// ── Step 1: Drink picker ──────────────────────────────────────────────────────

@Composable
private fun DrinkPickerContent(
    drinks: List<Drink>,
    selectedDrinkId: Long?,
    onSelectDrink: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (drinks.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No drinks configured. Add drinks in Session settings.")
        }
        return
    }

    val typeOrder = listOf(DrinkType.SHOT, DrinkType.LONG_DRINK, DrinkType.NON_ALCOHOLIC)
    val grouped = remember(drinks) { drinks.groupBy { it.type } }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 8.dp),
        modifier = modifier,
    ) {
        typeOrder.forEach { type ->
            val typeItems = grouped[type] ?: return@forEach
            item(key = "header_$type") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = when (type) {
                            DrinkType.SHOT          -> Icons.Filled.LocalDrink
                            DrinkType.LONG_DRINK    -> Icons.Filled.SportsBar
                            DrinkType.NON_ALCOHOLIC -> Icons.Filled.LocalCafe
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when (type) {
                            DrinkType.SHOT          -> "Shots"
                            DrinkType.LONG_DRINK    -> "Long Drinks"
                            DrinkType.NON_ALCOHOLIC -> "Non-Alcoholic"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            items(typeItems, key = { it.id }) { drink ->
                ListItem(
                    headlineContent = { Text(drink.name) },
                    leadingContent = {
                        Icon(
                            imageVector = when (drink.type) {
                                DrinkType.SHOT          -> Icons.Filled.LocalDrink
                                DrinkType.LONG_DRINK    -> Icons.Filled.SportsBar
                                DrinkType.NON_ALCOHOLIC -> Icons.Filled.LocalCafe
                            },
                            contentDescription = null,
                            tint = if (drink.id == selectedDrinkId)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = if (drink.id == selectedDrinkId) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .alpha(if (drink.isDisabled) 0.38f else 1f)
                        .then(
                            if (!drink.isDisabled)
                                Modifier.clickable { onSelectDrink(drink.id) }
                            else
                                Modifier,
                        ),
                )
            }
        }
    }
}

// ── Step 2: Glass picker ──────────────────────────────────────────────────────

@Composable
private fun GlassPickerContent(
    glassGroupLetters: List<Char>,
    selectedGlassGroup: Char?,
    onSelectGlassGroup: (Char?) -> Unit,
    selectedGlassNumber: Int,
    onGlassNumberChange: (Int) -> Unit,
    takenGlassNumbersForGroup: Set<Int>,
    modifier: Modifier = Modifier,
) {
    val glassIsAlreadyTaken = selectedGlassGroup != null && selectedGlassNumber in takenGlassNumbersForGroup

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Glass Group", style = MaterialTheme.typography.labelMedium)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedGlassGroup == null,
                onClick = { onSelectGlassGroup(null) },
                label = { Text("None") },
            )
            glassGroupLetters.forEach { letter ->
                FilterChip(
                    selected = selectedGlassGroup == letter,
                    onClick = { onSelectGlassGroup(letter) },
                    label = { Text("Group $letter") },
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            key(selectedGlassGroup) {
                GlassNumberPicker(
                    selectedNumber = selectedGlassNumber,
                    takenNumbers = takenGlassNumbersForGroup,
                    onNumberChange = onGlassNumberChange,
                    enabled = selectedGlassGroup != null,
                )
            }
        }
        if (glassIsAlreadyTaken) {
            Text(
                text = "Glass $selectedGlassNumber is already taken.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
