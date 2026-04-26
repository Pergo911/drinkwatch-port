package com.example.drinkwatch.ui.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.ui.session.drinks.DrinksTab
import com.example.drinkwatch.ui.session.glassgroups.GlassGroupsTab
import com.example.drinkwatch.ui.session.overview.OverviewTab
import com.example.drinkwatch.ui.session.players.PlayersTab
import com.example.drinkwatch.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    openedAsGuard: Boolean,
    onNavigateToMain: () -> Unit,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication
    val viewModel: SessionViewModel = viewModel(factory = app.sessionViewModelFactory)

    val session by viewModel.currentSession.collectAsStateWithLifecycle()
    val uiError by viewModel.uiError.collectAsStateWithLifecycle()

    LaunchedEffect(session) {
        if (openedAsGuard && session != null) {
            onNavigateToMain()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiError) {
        val msg = uiError
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val tabTitles = listOf("Overview", "Players", "Drinks", "Glass Groups")
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        enabled = index == 0 || session != null,
                        text = { Text(title) },
                    )
                }
            }
            when (selectedTab) {
                0 -> OverviewTab(viewModel = viewModel)
                1 -> if (session != null) PlayersTab(viewModel = viewModel)
                2 -> if (session != null) DrinksTab(viewModel = viewModel)
                3 -> if (session != null) GlassGroupsTab(viewModel = viewModel)
            }
        }
    }
}
