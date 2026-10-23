package com.swimtrainer.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.swimtrainer.app.ui.screens.DrillScreen
import com.swimtrainer.app.ui.screens.DrylandScreen
import com.swimtrainer.app.ui.screens.GuideScreen
import com.swimtrainer.app.ui.screens.LibraryScreen
import com.swimtrainer.app.ui.screens.PlanScreen
import com.swimtrainer.app.ui.screens.PlayerScreen
import com.swimtrainer.app.ui.screens.ProfileScreen
import com.swimtrainer.app.ui.screens.ProgressScreen
import com.swimtrainer.app.ui.screens.QuickSetScreen
import com.swimtrainer.app.ui.screens.TodayScreen
import com.swimtrainer.app.ui.screens.ToolsScreen
import com.swimtrainer.app.ui.screens.WorkoutScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", Icons.Filled.Pool),
    PLAN("plan", "Plan", Icons.Filled.CalendarMonth),
    QUICK("quick", "Quick set", Icons.Filled.Bolt),
    LIBRARY("library", "Library", Icons.AutoMirrored.Filled.MenuBook),
    PROGRESS("progress", "Progress", Icons.Filled.Insights),
}

@Composable
fun SwimApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Ready -> {
            val profile = s.data.profile
            if (profile == null) {
                ProfileScreen(initial = null, onboarding = true, hasPlan = false, onBack = null) { p, rebuild, weeks ->
                    vm.saveProfile(p, rebuild, weeks)
                }
            } else {
                MainNav(vm)
            }
        }
    }
}

@Composable
private fun MainNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = Tab.entries.any { it.route == route }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.navigateTab(tab.route) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        AppNavHost(nav, vm, Modifier.padding(padding))
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, vm: AppViewModel, modifier: Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()
    val quick by vm.quick.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val data = (state as? UiState.Ready)?.data ?: return
    val profile = data.profile ?: return

    NavHost(nav, startDestination = Tab.TODAY.route, modifier = modifier) {
        composable(Tab.TODAY.route) {
            TodayScreen(
                data = data, profile = profile, vm = vm,
                onOpenWorkout = { nav.navigate("workout") },
                onQuick = { nav.navigateTab(Tab.QUICK.route) },
                onPlan = { nav.navigateTab(Tab.PLAN.route) },
                onTools = { nav.navigate("tools") },
                onProfile = { nav.navigate("profile") },
            )
        }
        composable(Tab.PLAN.route) {
            PlanScreen(data, profile, vm, busy, onOpenWorkout = { nav.navigate("workout") })
        }
        composable(Tab.QUICK.route) {
            QuickSetScreen(profile, quick, vm, onStart = { nav.navigate("workout") })
        }
        composable(Tab.LIBRARY.route) {
            LibraryScreen(
                level = profile.level,
                onDrill = { nav.navigate("drill/$it") },
                onGuide = { nav.navigate("guide/$it") },
                onDryland = { nav.navigate("dryland/$it") },
            )
        }
        composable(Tab.PROGRESS.route) { ProgressScreen(data, profile, vm) }
        composable("workout") {
            val done = active?.planRef?.key?.let { it in data.completed } ?: false
            WorkoutScreen(active, done, vm, onBack = { nav.popBackStack() }, onStart = { nav.navigate("player") })
        }
        composable("player") {
            PlayerScreen(active, vm, onBack = { nav.popBackStack() }, onFinished = {
                nav.popBackStack(Tab.TODAY.route, inclusive = false)
                nav.navigateTab(Tab.PROGRESS.route)
            })
        }
        composable("tools") { ToolsScreen(profile, data.plan != null, vm, onBack = { nav.popBackStack() }) }
        composable("profile") {
            ProfileScreen(initial = profile, onboarding = false, hasPlan = data.plan != null, onBack = { nav.popBackStack() }) { p, rebuild, weeks ->
                vm.saveProfile(p, rebuild, weeks)
                nav.popBackStack()
            }
        }
        composable("drill/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            DrillScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable("guide/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            GuideScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }, onDrill = { nav.navigate("drill/$it") })
        }
        composable("dryland/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            DrylandScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
        }
    }
}
