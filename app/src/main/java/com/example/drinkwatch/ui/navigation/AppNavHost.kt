package com.example.drinkwatch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.ui.about.AboutScreen
import com.example.drinkwatch.ui.dialog.OrderDialogContent
import com.example.drinkwatch.ui.main.MainScreen
import com.example.drinkwatch.ui.player.PlayerDetailScreen
import com.example.drinkwatch.ui.session.SessionScreen
import com.example.drinkwatch.ui.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication

    val backStack = remember { mutableStateListOf<AppRoute>(Main) }

    LaunchedEffect(Unit) {
        if (!app.sessionRepository.hasActiveSession()) {
            backStack.clear()
            backStack.add(Session)
        }
    }

    val dialogStrategy = remember { DialogSceneStrategy<AppRoute>() }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(dialogStrategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {

            entry<Main> {
                MainScreen(
                    onNavigateToSession  = dropUnlessResumed { backStack.add(Session) },
                    onNavigateToSettings = dropUnlessResumed { backStack.add(Settings) },
                    onNavigateToAbout    = dropUnlessResumed { backStack.add(About) },
                    onNavigateToPlayerDetail = { id -> backStack.add(PlayerDetail(id)) },
                    // dropUnlessResumed wraps () -> Unit only; (Long) -> Unit callbacks are
                    // guarded at the call site when screens are implemented in Phases 6–9.
                    onNavigateToOrderDialog  = { id -> backStack.add(OrderDialog(id)) },
                )
            }

            entry<Session> {
                SessionScreen(
                    onNavigateToMain = dropUnlessResumed {
                        backStack.clear()
                        backStack.add(Main)
                    },
                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                )
            }

            entry<Settings> {
                SettingsScreen(onBack = dropUnlessResumed { backStack.removeLastOrNull() })
            }

            entry<About> {
                AboutScreen(onBack = dropUnlessResumed { backStack.removeLastOrNull() })
            }

            entry<PlayerDetail> { key ->
                PlayerDetailScreen(
                    playerId = key.playerId,
                    onBack   = dropUnlessResumed { backStack.removeLastOrNull() },
                )
            }

            entry<OrderDialog>(
                metadata = DialogSceneStrategy.dialog(
                    DialogProperties(usePlatformDefaultWidth = false)
                )
            ) { key ->
                OrderDialogContent(
                    playerId  = key.playerId,
                    onDismiss = dropUnlessResumed { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
