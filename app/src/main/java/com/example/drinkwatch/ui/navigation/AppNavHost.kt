package com.example.drinkwatch.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.example.drinkwatch.ui.dialog.OrderDialogContent
import com.example.drinkwatch.ui.main.MainScreen
import com.example.drinkwatch.ui.player.PlayerDetailScreen
import com.example.drinkwatch.ui.settings.SettingsScreen
import com.example.drinkwatch.util.findActivity

@Composable
fun AppNavHost() {
    val activity = LocalContext.current.findActivity()

    val backStack = remember { mutableStateListOf<AppRoute>(Main) }

    val popOrFinish: () -> Unit = {
        if (backStack.size <= 1) activity.finish()
        else backStack.removeLastOrNull()
    }

    val dialogStrategy = remember { DialogSceneStrategy<AppRoute>() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = popOrFinish,
            sceneStrategies = listOf(dialogStrategy),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {

                entry<Main> {
                    MainScreen(
                        onNavigateToSettings = dropUnlessResumed { backStack.add(Settings) },
                        onNavigateToPlayerDetail = { id -> backStack.add(PlayerDetail(id)) },
                        onNavigateToOrderDialog = { id ->
                            if (backStack.none { it is OrderDialog }) {
                                backStack.add(OrderDialog(id))
                            }
                        },
                    )
                }

                entry<Settings> {
                    SettingsScreen(onBack = dropUnlessResumed { backStack.removeLastOrNull() })
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
}
