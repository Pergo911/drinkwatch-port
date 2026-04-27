package com.example.drinkwatch.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver

enum class MainTab { SESSION, ORDER, GLASSES }

val MainTabSaver: Saver<MainTab, String> = Saver(
    save    = { it.name },
    restore = { saved -> MainTab.entries.firstOrNull { it.name == saved } ?: MainTab.ORDER },
)

@Composable
fun MainBottomNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    takenGlassCount: Int,
    hasSession: Boolean,
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == MainTab.SESSION,
            onClick = { onTabSelected(MainTab.SESSION) },
            icon = { Icon(Icons.Filled.Dataset, contentDescription = null) },
            label = { Text("Session") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor    = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.ORDER,
            onClick = { onTabSelected(MainTab.ORDER) },
            enabled = hasSession,
            icon = { Icon(Icons.Filled.Liquor, contentDescription = null) },
            label = { Text("Order") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor    = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.GLASSES,
            onClick = { onTabSelected(MainTab.GLASSES) },
            enabled = hasSession,
            icon = {
                BadgedBox(
                    badge = {
                        if (takenGlassCount > 0) {
                            Badge { Text(takenGlassCount.toString()) }
                        }
                    },
                ) {
                    Icon(Icons.Filled.LocalBar, contentDescription = null)
                }
            },
            label = { Text("Glasses") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor    = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
    }
}
