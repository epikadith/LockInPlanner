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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lockinplanner.data.local.AppDatabase
import com.example.lockinplanner.data.repository.TaskRepository
import com.example.lockinplanner.ui.screens.TimelineScreen
import com.example.lockinplanner.ui.screens.CalendarScreen
import com.example.lockinplanner.ui.screens.ChecklistsScreen
import com.example.lockinplanner.ui.theme.LockInPlannerTheme
import com.example.lockinplanner.ui.viewmodel.CalendarViewModelFactory
import com.example.lockinplanner.ui.viewmodel.TimelineViewModelFactory

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
        
        val viewModelFactory = TimelineViewModelFactory(taskRepository)
        val settingsViewModelFactory = SettingsViewModelFactory(userPreferencesRepository, taskRepository, checklistRepository)

        setContent {
            val userPreferences by userPreferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
            
            LockInPlannerTheme(appTheme = userPreferences.theme) {
                LockInPlannerApp(viewModelFactory, taskRepository, settingsViewModelFactory, userPreferences)
            }
        }
    }
}

@Composable
fun LockInPlannerApp(
    viewModelFactory: TimelineViewModelFactory, 
    repository: TaskRepository,
    settingsViewModelFactory: SettingsViewModelFactory,
    userPreferences: UserPreferences
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.TIMELINE) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                AppDestinations.SETTINGS -> SettingsScreen(
                    viewModel = viewModel(factory = settingsViewModelFactory),
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    TIMELINE("Timeline", Icons.Default.List),
    CALENDAR("Calendar", Icons.Default.DateRange),
    CHECKLISTS("Checklists", Icons.Default.Check),
    SETTINGS("Settings", Icons.Default.Settings),
}