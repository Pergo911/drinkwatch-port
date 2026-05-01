package com.example.drinkwatch.ui.main

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.ui.glasses.GlassesTab
import com.example.drinkwatch.ui.order.OrderTab
import com.example.drinkwatch.ui.session.SessionTab
import com.example.drinkwatch.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Suppress("OPT_IN_USAGE")
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayerDetail: (Long) -> Unit,
    onNavigateToOrderDialog: (Long) -> Unit,
) {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication
    val activity = LocalActivity.current as ComponentActivity
    val viewModel: MainViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = app.mainViewModelFactory,
    )

    val sessionName           by viewModel.sessionName.collectAsStateWithLifecycle()
    val sessionLoadState      by viewModel.sessionLoadState.collectAsStateWithLifecycle()
    val takenGlasses          by viewModel.takenGlasses.collectAsStateWithLifecycle()
    val playerUiStates        by viewModel.playerUiStates.collectAsStateWithLifecycle()
    val activeDrinkHighlight  by viewModel.activeDrinkHighlight.collectAsStateWithLifecycle()
    val defaultTimeoutSeconds by viewModel.defaultTimeoutSeconds.collectAsStateWithLifecycle()
    val queue                 by viewModel.queue.collectAsStateWithLifecycle()

    // Derive hasSession from the single sessionLoadState so that "ready?" and "session exists?"
    // always reflect the same upstream emission and can never be observed out of sync.
    val hasSession = (sessionLoadState as? MainViewModel.SessionLoadState.Loaded)?.sessionId != null

    // Default to ORDER.  Only switch to SESSION once sessionLoadState is Loaded *and* there is
    // no active session.  Keyed by sessionLoadState so the state reinitialises correctly when
    // the session changes — both conditions come from the same atomic value, preventing jerk.
    var selectedTab by rememberSaveable(sessionLoadState, stateSaver = MainTabSaver) {
        val loaded = sessionLoadState as? MainViewModel.SessionLoadState.Loaded
        mutableStateOf(if (loaded != null && loaded.sessionId == null) MainTab.SESSION else MainTab.ORDER)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is MainViewModel.UiEvent.GlassAlreadyTaken ->
                    snackbarHostState.showSnackbar("That glass is already taken.")
            }
        }
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                sessionName = sessionName,
                onSettingsClick = onNavigateToSettings,
            )
        },
        bottomBar = {
            MainBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                takenGlassCount = takenGlasses.size,
                hasSession = hasSession,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            contentAlignment = Alignment.TopStart,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                slideInHorizontally(tween(250)) { it * direction / 6 } + fadeIn(tween(250)) togetherWith
                    slideOutHorizontally(tween(150)) { -it * direction / 6 } + fadeOut(tween(150))
            },
            label = "MainTabContent",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { tab ->
            when (tab) {
                MainTab.SESSION ->
                    SessionTab(
                        onShowSnackbar = { msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                MainTab.ORDER ->
                    OrderTab(
                        playerUiStates = playerUiStates,
                        activeDrinkHighlight = activeDrinkHighlight,
                        defaultTimeoutSeconds = defaultTimeoutSeconds,
                        onNavigateToOrderDialog = onNavigateToOrderDialog,
                        onNavigateToPlayerDetail = onNavigateToPlayerDetail,
                        onStartTimeout = viewModel::startTimeout,
                        onCancelQueuedOrder = viewModel::cancelQueuedOrder,
                        onCommitQueue = viewModel::commitQueue,
                        queueSize = queue.size,
                        onShowSnackbar = { msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                MainTab.GLASSES ->
                    GlassesTab(
                        takenGlasses = takenGlasses,
                        onReturnGlass = viewModel::returnGlass,
                        modifier = Modifier.fillMaxSize(),
                    )
            }
        }
    }
}
