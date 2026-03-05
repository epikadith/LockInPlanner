package com.example.lockinplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.BottomAppBar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lockinplanner.data.local.AppDatabase
import com.example.lockinplanner.data.repository.TaskRepository
import com.example.lockinplanner.data.repository.NotesRepository
import com.example.lockinplanner.ui.screens.TimelineScreen
import com.example.lockinplanner.ui.screens.SearchScreen
import com.example.lockinplanner.ui.screens.CalendarScreen
import com.example.lockinplanner.ui.screens.ChecklistsScreen
import com.example.lockinplanner.ui.screens.NotesScreen
import com.example.lockinplanner.ui.theme.LockInPlannerTheme
import com.example.lockinplanner.ui.utils.performLightHapticFeedback
import com.example.lockinplanner.ui.viewmodel.CalendarViewModelFactory
import com.example.lockinplanner.ui.viewmodel.SearchViewModelFactory
import com.example.lockinplanner.ui.viewmodel.TimelineViewModelFactory
import com.example.lockinplanner.ui.viewmodel.NotesViewModelFactory

import androidx.compose.runtime.collectAsState
import com.example.lockinplanner.data.repository.UserPreferencesRepository
import com.example.lockinplanner.domain.model.UserPreferences
import com.example.lockinplanner.ui.screens.SettingsScreen
import com.example.lockinplanner.ui.viewmodel.SettingsViewModelFactory
import com.example.lockinplanner.domain.notification.AlarmScheduler

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission Granted: We can set up the notification channels or other logic if needed immediately.
            // Channels are already created in BootReceiver/App init usually, but good to know.
        } else {
            // Permission Denied: Functionality will degraded (no notifications).
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
            
        // Create Notification Channels
        com.example.lockinplanner.domain.notification.NotificationManagerHelper(this).createNotificationChannels()

        // Request Permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val database = AppDatabase.getDatabase(this)
        val userPreferencesRepository = UserPreferencesRepository(this)
        val alarmScheduler = AlarmScheduler(this)
        
        val taskRepository = TaskRepository(database.taskDao(), alarmScheduler, userPreferencesRepository)
        val checklistRepository = com.example.lockinplanner.data.repository.ChecklistRepository(database.checklistDao())
        val notesRepository = NotesRepository(database.bookDao(), database.chapterDao(), database.pageDao(), database.shortDao())
        
        val viewModelFactory = TimelineViewModelFactory(taskRepository)
        val settingsViewModelFactory = SettingsViewModelFactory(userPreferencesRepository, taskRepository, checklistRepository, notesRepository)
        val searchViewModelFactory = SearchViewModelFactory(taskRepository, checklistRepository)
        val notesViewModelFactory = NotesViewModelFactory(notesRepository)

        setContent {
            val userPreferences by userPreferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
            
            LockInPlannerTheme(appTheme = userPreferences.theme, userPreferences = userPreferences) {
                LockInPlannerApp(
                    viewModelFactory = viewModelFactory, 
                    repository = taskRepository, 
                    settingsViewModelFactory = settingsViewModelFactory, 
                    searchViewModelFactory = searchViewModelFactory,
                    notesViewModelFactory = notesViewModelFactory,
                    userPreferences = userPreferences
                )
            }
        }
    }
}

@Composable
fun LockInPlannerApp(
    viewModelFactory: TimelineViewModelFactory, 
    repository: TaskRepository,
    settingsViewModelFactory: SettingsViewModelFactory,
    searchViewModelFactory: SearchViewModelFactory,
    notesViewModelFactory: NotesViewModelFactory,
    userPreferences: UserPreferences
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.TIMELINE) }
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val itemWidth = configuration.screenWidthDp.dp / 5f

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                ) {
                    AppDestinations.entries.filter { it != AppDestinations.ANALYTICS }.forEach { destination ->
                        Row(modifier = Modifier.width(itemWidth)) {
                            NavigationBarItem(
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) },
                                selected = destination == currentDestination,
                                onClick = { 
                                    view.performLightHapticFeedback(userPreferences.hapticsEnabled)
                                    currentDestination = destination 
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when (currentDestination) {
            AppDestinations.TIMELINE -> TimelineScreen(
                viewModel = viewModel(factory = viewModelFactory),
                userPreferences = userPreferences,
                modifier = Modifier.padding(innerPadding)
            )
            AppDestinations.CALENDAR -> CalendarScreen(
                viewModel = viewModel(factory = CalendarViewModelFactory(repository)),
                userPreferences = userPreferences,
                modifier = Modifier.padding(innerPadding)
            )
            AppDestinations.CHECKLISTS -> ChecklistsScreen(
                userPreferences = userPreferences,
                modifier = Modifier.padding(innerPadding)
            )
            AppDestinations.SEARCH -> SearchScreen(
                viewModel = viewModel(factory = searchViewModelFactory),
                userPreferences = userPreferences,
                modifier = Modifier.padding(innerPadding)
            )
            AppDestinations.NOTES -> NotesScreen(
                viewModel = viewModel(factory = notesViewModelFactory),
                settingsViewModel = viewModel(factory = settingsViewModelFactory),
                userPreferences = userPreferences,
                modifier = Modifier.padding(innerPadding)
            )
            AppDestinations.ANALYTICS -> {
                val analyticsViewModelFactory = com.example.lockinplanner.ui.viewmodel.AnalyticsViewModelFactory(
                    repository,
                    com.example.lockinplanner.data.repository.ChecklistRepository(com.example.lockinplanner.data.local.AppDatabase.getDatabase(androidx.compose.ui.platform.LocalContext.current).checklistDao()),
                    com.example.lockinplanner.data.repository.NotesRepository(
                        com.example.lockinplanner.data.local.AppDatabase.getDatabase(androidx.compose.ui.platform.LocalContext.current).bookDao(),
                        com.example.lockinplanner.data.local.AppDatabase.getDatabase(androidx.compose.ui.platform.LocalContext.current).chapterDao(),
                        com.example.lockinplanner.data.local.AppDatabase.getDatabase(androidx.compose.ui.platform.LocalContext.current).pageDao(),
                        com.example.lockinplanner.data.local.AppDatabase.getDatabase(androidx.compose.ui.platform.LocalContext.current).shortDao()
                    )
                )
                com.example.lockinplanner.ui.screens.AnalyticsScreen(
                    viewModel = viewModel(factory = analyticsViewModelFactory),
                    onBack = { currentDestination = AppDestinations.SETTINGS },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppDestinations.SETTINGS -> SettingsScreen(
                viewModel = viewModel(factory = settingsViewModelFactory),
                onNavigateToAnalytics = { currentDestination = AppDestinations.ANALYTICS },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    TIMELINE("Timeline", Icons.Default.List),
    CALENDAR("Calendar", Icons.Default.DateRange),
    CHECKLISTS("Lists", Icons.Default.Check),
    SEARCH("Search", Icons.Default.Search),
    NOTES("Notes", Icons.Default.Edit),
    SETTINGS("Settings", Icons.Default.Settings),
    ANALYTICS("Analytics", Icons.Default.List),
}