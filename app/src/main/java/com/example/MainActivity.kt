package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.StoryEntity
import com.example.ui.screens.AuthLockScreen
import com.example.ui.screens.CreateStudioScreen
import com.example.ui.screens.DirectMessagesScreen
import com.example.ui.screens.ExploreSearchScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StoryViewerScreen
import com.example.ui.theme.PrismAmber
import com.example.ui.theme.PrismTheme
import com.example.ui.viewmodel.RistamViewModel

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val mainTabs = listOf(
    BottomNavTab("feed", "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home, "tab_feed"),
    BottomNavTab("explore", "استكشاف", Icons.Filled.Search, Icons.Outlined.Search, "tab_explore"),
    BottomNavTab("create", "إنشاء", Icons.Filled.AddBox, Icons.Outlined.AddBox, "tab_create"),
    BottomNavTab("reels", "المقاطع", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, "tab_reels"),
    BottomNavTab("profile", "حسابي", Icons.Filled.Person, Icons.Outlined.Person, "tab_profile")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: RistamViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()

            PrismTheme(
                themePreset = prefs.themePreset,
                accentPreset = prefs.accentPreset,
                cornerDensity = prefs.cornerDensity
            ) {
                RistamAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun RistamAppRoot(viewModel: RistamViewModel) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
    val rankedPosts by viewModel.rankedFeedPosts.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val chatThreads by viewModel.chatThreads.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val usageStats by viewModel.usageStats.collectAsStateWithLifecycle()
    val supportTickets by viewModel.supportTickets.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnosticReport.collectAsStateWithLifecycle()
    val sortMode by viewModel.feedSortMode.collectAsStateWithLifecycle()
    val selectedCollectionId by viewModel.selectedCollectionFilter.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val productivityAlert by viewModel.productivityAlert.collectAsStateWithLifecycle()
    val sessionSeconds by viewModel.sessionSeconds.collectAsStateWithLifecycle()

    if (isLocked) {
        Box(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            AuthLockScreen(
                errorMessage = authError,
                onUnlock = { viewModel.unlockWithMasterPassword(it) }
            )
        }
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "feed"
    val snackbarHostState = remember { SnackbarHostState() }
    var activeStory by remember { mutableStateOf<StoryEntity?>(null) }

    LaunchedEffect(currentRoute) {
        viewModel.onActiveTabChanged(currentRoute)
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    val unreadNotifsCount = remember(notifications) { notifications.count { !it.isRead } }
    val unreadMessagesCount = remember(chatThreads) { chatThreads.sumOf { it.unreadCount } }
    val showMainChrome = currentRoute in mainTabs.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showMainChrome && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        mainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo("feed") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showMainChrome && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        mainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo("feed") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Productivity Break Alert Banner
                    AnimatedVisibility(
                        visible = productivityAlert != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        productivityAlert?.let { alertMsg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrismAmber.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = alertMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.dismissProductivityAlert() }) {
                                        Text("حسناً")
                                    }
                                }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "feed",
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        composable("feed") {
                            HomeFeedScreen(
                                viewModel = viewModel,
                                currentUser = currentUser,
                                rankedPosts = rankedPosts,
                                stories = stories,
                                collections = collections,
                                prefs = prefs,
                                sortMode = sortMode,
                                selectedCollectionId = selectedCollectionId,
                                unreadNotificationsCount = unreadNotifsCount,
                                unreadMessagesCount = unreadMessagesCount,
                                onOpenNotifications = { navController.navigate("notifications") },
                                onOpenMessages = { navController.navigate("messages") },
                                onOpenCreate = { navController.navigate("create") },
                                onOpenStory = { story -> activeStory = story }
                            )
                        }
                        composable("explore") {
                            ExploreSearchScreen(
                                viewModel = viewModel,
                                rankedPosts = rankedPosts,
                                prefs = prefs,
                                onOpenSettingsAlgorithm = { navController.navigate("settings") }
                            )
                        }
                        composable("create") {
                            CreateStudioScreen(
                                viewModel = viewModel,
                                currentUser = currentUser,
                                collections = collections,
                                onPublished = {
                                    navController.navigate("feed") {
                                        popUpTo("feed") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("reels") {
                            ReelsScreen(
                                viewModel = viewModel,
                                allPosts = allPosts,
                                distractionFree = prefs.distractionFreeMode
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                viewModel = viewModel,
                                currentUser = currentUser,
                                allPosts = allPosts,
                                collections = collections,
                                prefs = prefs,
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenReports = { navController.navigate("reports") }
                            )
                        }
                        composable("messages") {
                            DirectMessagesScreen(
                                viewModel = viewModel,
                                threads = chatThreads,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("notifications") {
                            NotificationsScreen(
                                viewModel = viewModel,
                                notifications = notifications,
                                prefs = prefs,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                prefs = prefs,
                                diagnostics = diagnostics,
                                supportTickets = supportTickets,
                                onOpenReports = { navController.navigate("reports") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("reports") {
                            ReportsScreen(
                                viewModel = viewModel,
                                usageStats = usageStats,
                                prefs = prefs,
                                sessionSeconds = sessionSeconds,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full-screen Story Viewer overlay
    activeStory?.let { story ->
        StoryViewerScreen(
            stories = stories,
            initialStoryId = story.id,
            onClose = { activeStory = null },
            onMarkViewed = { viewModel.markStoryViewed(it) },
            onSendReply = { authorId, authorHandle, replyText ->
                viewModel.replyToStoryAsDm(authorId, authorHandle, replyText)
            }
        )
    }
}
