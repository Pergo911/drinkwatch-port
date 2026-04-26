package com.example.drinkwatch.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.ui.order.OrderTab
import com.example.drinkwatch.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onNavigateToSession: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPlayerDetail: (Long) -> Unit,
    onNavigateToOrderDialog: (Long) -> Unit,
) {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication
    val activity = LocalContext.current as ComponentActivity
    val viewModel: MainViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = app.mainViewModelFactory,
    )

    val sessionName           by viewModel.sessionName.collectAsStateWithLifecycle()
    val sessionId             by viewModel.sessionId.collectAsStateWithLifecycle()
    val takenGlasses          by viewModel.takenGlasses.collectAsStateWithLifecycle()
    val playerUiStates        by viewModel.playerUiStates.collectAsStateWithLifecycle()
    val activeDrinkHighlight  by viewModel.activeDrinkHighlight.collectAsStateWithLifecycle()
    val defaultTimeoutSeconds by viewModel.defaultTimeoutSeconds.collectAsStateWithLifecycle()
    val queue                 by viewModel.queue.collectAsStateWithLifecycle()

    // Key by sessionId so the tab resets to ORDER whenever the session is replaced.
    var selectedTab by rememberSaveable(sessionId, stateSaver = MainTabSaver) {
        mutableStateOf(MainTab.ORDER)
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
                onNavigateToSession = onNavigateToSession,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToAbout = onNavigateToAbout,
            )
        },
        bottomBar = {
            MainBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                takenGlassCount = takenGlasses.size,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (selectedTab) {
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            MainTab.GLASSES ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Glasses tab (stub)")
                }
        }
    }
}
