package com.studymate.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studymate.app.ui.chat.ChatScreen
import com.studymate.app.ui.study.StudyScreen
import com.studymate.app.ui.theme.StudyMateTheme

/** Sealed description of each bottom-nav destination. */
private sealed class Tab(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Chat : Tab("chat", "Chat", { Icon(Icons.Filled.Chat, contentDescription = null) })
    object Study : Tab("study", "Study Assistant", { Icon(Icons.Filled.MenuBook, contentDescription = null) })
}

private val tabs = listOf(Tab.Chat, Tab.Study)

/**
 * Root Compose entry point: theme + bottom-nav scaffold + NavHost with the two tabs.
 */
@Composable
fun StudyMateApp() {
    StudyMateTheme {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val current = backStack?.destination

        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = current?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    // Single-top + save/restore state for standard tab behavior.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = tab.icon,
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { inner: PaddingValues ->
            NavHost(
                navController = navController,
                startDestination = Tab.Chat.route,
                modifier = Modifier.padding(inner)
            ) {
                composable(Tab.Chat.route) { ChatScreen() }
                composable(Tab.Study.route) { StudyScreen() }
            }
        }
    }
}
