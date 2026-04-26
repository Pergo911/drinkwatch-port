package com.example.drinkwatch.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.viewmodel.MainViewModel

@Composable
fun MainScreen(
    onNavigateToSession: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPlayerDetail: (Long) -> Unit,
    onNavigateToOrderDialog: (Long) -> Unit,
) {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication
    val viewModel: MainViewModel = viewModel(factory = app.mainViewModelFactory)

    val sessionName by viewModel.sessionName.collectAsStateWithLifecycle()
    val sessionId   by viewModel.sessionId.collectAsStateWithLifecycle()

    // Key by sessionId so the tab resets to ORDER whenever the session is replaced.
    var selectedTab by rememberSaveable(sessionId, stateSaver = MainTabSaver) {
        mutableStateOf(MainTab.ORDER)
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
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when (selectedTab) {
                    MainTab.ORDER   -> "Order tab (stub)"
                    MainTab.GLASSES -> "Glasses tab (stub)"
                }
            )
        }
    }
}
