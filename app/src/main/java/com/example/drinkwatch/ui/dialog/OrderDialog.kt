package com.example.drinkwatch.ui.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.R
import com.example.drinkwatch.data.model.Drink
import com.example.drinkwatch.data.model.DrinkType
import com.example.drinkwatch.ui.component.SearchField
import com.example.drinkwatch.ui.theme.Dimens
import com.example.drinkwatch.util.findActivity
import com.example.drinkwatch.viewmodel.MainViewModel
import com.example.drinkwatch.viewmodel.SessionViewModel
import kotlinx.coroutines.delay

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

    // Animate in on entry; animate out before dismissing.
    var visible by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(dismissing) {
        if (dismissing) {
            visible = false
            delay(300)
            onDismiss()
        }
    }
    val animatedDismiss: () -> Unit = { if (!dismissing) dismissing = true }

    // Lower-priority fallback: dismiss dialog with animation when back is pressed on Step 1.
    BackHandler { animatedDismiss() }
    // Higher-priority: back on Step 2 returns to Step 1.
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

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(300)) { it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200)),
    ) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(stringResource(if (step == Step.DRINK) R.string.order_dialog_step_drink else R.string.order_dialog_step_glass))
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (step == Step.GLASS) step = Step.DRINK else animatedDismiss()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                            )
                        }
                    },
                    actions = {
                        when (step) {
                            Step.DRINK ->
                                IconButton(
                                    onClick = { step = Step.GLASS },
                                    enabled = selectedDrink != null,
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = stringResource(R.string.cd_next),
                                    )
                                }
                            Step.GLASS ->
                                QueueSplitButton(
                                    onQueue = {
                                        mainVm.addToQueue(
                                            playerId,
                                            selectedDrink!!.id,
                                            selectedGlassGroup,
                                            if (selectedGlassGroup != null) selectedGlassNumber else null,
                                        )
                                        animatedDismiss()
                                    },
                                    onConfirmNow = {
                                        mainVm.commitOrderNow(
                                            playerId,
                                            selectedDrink!!.id,
                                            selectedGlassGroup,
                                            if (selectedGlassGroup != null) selectedGlassNumber else null,
                                        )
                                        animatedDismiss()
                                    },
                                    enabled = canConfirm,
                                )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                StepProgressBar(step = step)
            }
        },
    ) { innerPadding ->
        when (step) {
            Step.DRINK ->
                DrinkPickerContent(
                    drinks = drinks,
                    selectedDrinkId = selectedDrinkId,
                    onSelectDrink = { id ->
                        selectedDrinkId = id
                        selectedGlassGroup = null
                        selectedGlassNumber = 1
                        step = Step.GLASS
                    },
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
    } // AnimatedVisibility
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
            Text(stringResource(R.string.empty_no_drinks_configured))
            return
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val typeOrder = listOf(DrinkType.SHOT, DrinkType.LONG_DRINK, DrinkType.NON_ALCOHOLIC)
    val filteredDrinks = remember(drinks, searchQuery) {
        if (searchQuery.isBlank()) drinks
        else drinks.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val grouped = remember(filteredDrinks) { filteredDrinks.groupBy { it.type } }

    Column(modifier = modifier) {
        SearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = stringResource(R.string.search_drinks_placeholder),
            expanded = isSearchExpanded,
            onExpand = { isSearchExpanded = true },
            onCollapse = { isSearchExpanded = false; searchQuery = "" },
            collapsedHorizontalPadding = Dimens.DetailHorizontalPadding,
            collapsedVerticalPadding = 8.dp,
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.cd_clear_search),
                        )
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
        )

        if (filteredDrinks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    )
                    Text(
                        text = stringResource(R.string.no_drinks_match, searchQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 8.dp, start = Dimens.DetailHorizontalPadding, end = Dimens.DetailHorizontalPadding),
                modifier = Modifier.fillMaxSize(),
            ) {
                typeOrder.forEach { type ->
                    val typeItems = grouped[type] ?: return@forEach
                    item(key = "header_$type") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp),
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
                                    DrinkType.SHOT          -> stringResource(R.string.drink_type_shots_header)
                                    DrinkType.LONG_DRINK    -> stringResource(R.string.drink_type_long_drinks_header)
                                    DrinkType.NON_ALCOHOLIC -> stringResource(R.string.drink_type_non_alcoholic_header)
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
                                .padding(8.dp, 0.dp)
                                .clip(MaterialTheme.shapes.medium)
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
    val glassPickerEnabled = selectedGlassGroup != null
    var showGlassPickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(horizontal = Dimens.DetailHorizontalPadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.order_dialog_glass_group_label), style = MaterialTheme.typography.labelMedium)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedGlassGroup == null,
                onClick = { onSelectGlassGroup(null) },
                label = { Text(stringResource(R.string.order_dialog_glass_none)) },
            )
            glassGroupLetters.forEach { letter ->
                FilterChip(
                    selected = selectedGlassGroup == letter,
                    onClick = { onSelectGlassGroup(letter) },
                    label = { Text(stringResource(R.string.order_dialog_glass_group, letter.toString())) },
                )
            }
        }

        Text(stringResource(R.string.order_dialog_glass_number_label), style = MaterialTheme.typography.labelMedium)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (glassPickerEnabled) 1f else 0.38f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .clickable(enabled = glassPickerEnabled) { showGlassPickerDialog = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.order_dialog_glass_hash), style = MaterialTheme.typography.labelMedium)
                Text(
                    text = if (glassPickerEnabled) selectedGlassNumber.toString() else "–",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.cd_change_glass_number),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        if (glassIsAlreadyTaken) {
            Text(
                text = stringResource(R.string.order_dialog_glass_taken, selectedGlassNumber),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }

    if (showGlassPickerDialog) {
        GlassNumberPickerDialog(
            initialNumber = selectedGlassNumber,
            takenNumbers = takenGlassNumbersForGroup,
            onDismiss = { showGlassPickerDialog = false },
            onConfirm = { number ->
                onGlassNumberChange(number)
                showGlassPickerDialog = false
            },
        )
    }
}

// ── Step progress indicator ───────────────────────────────────────────────────

@Composable
private fun StepProgressBar(step: Step, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = if (step == Step.DRINK) 0.5f else 1f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "stepProgress",
    )
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.order_dialog_step_progress, if (step == Step.DRINK) 1 else 2),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = Dimens.ScreenHorizontalPadding, top = 4.dp, bottom = 4.dp),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Split button (Step 2 action) ──────────────────────────────────────────────

@Composable
private fun QueueSplitButton(
    onQueue: () -> Unit,
    onConfirmNow: () -> Unit,
    enabled: Boolean,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled) menuExpanded = false
    }

    // Leading button inner (right) corners: 4dp (closed, unified-pill) → 12dp (open).
    val leadingEndCorner by animateDpAsState(
        targetValue = if (menuExpanded) 12.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "leadingEndCorner",
    )
    // Trailing button inner (left) corners: 4dp (closed, matches leading) → 20dp (open = full circle).
    val trailingStartCorner by animateDpAsState(
        targetValue = if (menuExpanded) 20.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "trailingStartCorner",
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (menuExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chevronRotation",
    )

    val trailingContainerColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }
    val trailingContentColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(end = 8.dp),
    ) {
        Button(
            onClick = onQueue,
            enabled = enabled,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = leadingEndCorner,
                bottomEnd = leadingEndCorner,
                bottomStart = 20.dp,
            ),
            contentPadding = PaddingValues(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.btn_queue))
        }
        // Wrap trailing button + menu in its own Box so the dropdown anchors below
        // the trailing button, not the entire split button group.
        // Use Surface instead of Button so we have full control over size
        // (Button has an internal defaultMinSize of 58dp that can't be overridden).
        Box {
            Surface(
                onClick = { menuExpanded = !menuExpanded },
                enabled = enabled,
                // Closed: 4dp inner corners match leading → reads as one pill.
                // Open: 20dp on all corners → full circle.
                shape = RoundedCornerShape(
                    topStart = trailingStartCorner,
                    topEnd = 20.dp,
                    bottomEnd = 20.dp,
                    bottomStart = trailingStartCorner,
                ),
                color = trailingContainerColor,
                contentColor = trailingContentColor,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.cd_more_actions),
                        modifier = Modifier.size(22.dp).rotate(chevronRotation),
                    )
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = MaterialTheme.shapes.large,
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.btn_confirm_now)) },
                    onClick = { menuExpanded = false; onConfirmNow() },
                    leadingIcon = { Icon(Icons.Filled.Bolt, contentDescription = null) },
                    enabled = enabled,
                )
            }
        }
    }
}
