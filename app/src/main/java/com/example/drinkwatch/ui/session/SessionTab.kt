package com.example.drinkwatch.ui.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTab(
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication
    val viewModel: SessionViewModel = viewModel(factory = app.sessionViewModelFactory)

    val session by viewModel.currentSession.collectAsStateWithLifecycle()
    val uiError by viewModel.uiError.collectAsStateWithLifecycle()

    LaunchedEffect(uiError) {
        val msg = uiError
        if (msg != null) {
            onShowSnackbar(msg)
            viewModel.clearError()
        }
    }

    val tabTitles = listOf("Overview", "Players", "Drinks", "Glasses")
    val tabIcons = listOf(
        Icons.Filled.Dataset,
        Icons.Filled.People,
        Icons.Filled.Liquor,
        Icons.Filled.LocalBar,
    )
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        PrimaryTabRow(selectedTabIndex = pagerState.currentPage, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            tabTitles.forEachIndexed { index, title ->
                val enabled = index == 0 || session != null
                val contentColor = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    enabled = enabled,
                    text = { Text(title, color = contentColor) },
                    icon = {
                        Icon(
                            imageVector = tabIcons[index],
                            contentDescription = null,
                            tint = contentColor,
                        )
                    },
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = session != null,
        ) { page ->
            when (page) {
                0 -> OverviewTab(viewModel = viewModel)
                1 -> if (session != null) PlayersTab(viewModel = viewModel, isActive = pagerState.currentPage == 1)
                2 -> if (session != null) DrinksTab(viewModel = viewModel, isActive = pagerState.currentPage == 2)
                3 -> if (session != null) GlassGroupsTab(viewModel = viewModel)
            }
        }
    }
}
