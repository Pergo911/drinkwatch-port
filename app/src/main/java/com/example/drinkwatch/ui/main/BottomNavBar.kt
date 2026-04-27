package com.example.drinkwatch.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.People
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

enum class MainTab { ORDER, GLASSES }

val MainTabSaver: Saver<MainTab, Int> = Saver(
    save    = { it.ordinal },
    restore = { MainTab.entries[it] },
)

@Composable
fun MainBottomNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    takenGlassCount: Int,
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == MainTab.ORDER,
            onClick = { onTabSelected(MainTab.ORDER) },
            icon = { Icon(Icons.Filled.People, contentDescription = null) },
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
