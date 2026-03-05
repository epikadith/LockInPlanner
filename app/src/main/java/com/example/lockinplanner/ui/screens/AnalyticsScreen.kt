package com.example.lockinplanner.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lockinplanner.ui.viewmodel.AnalyticsState
import com.example.lockinplanner.ui.viewmodel.AnalyticsViewModel
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onBack()
    }

    val tabs = listOf("Tasks", "Checklists", "Notes")

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // App Bar
        TopAppBar(
            title = { Text("Analytics") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Tabs
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> TasksAnalytics(state, viewModel)
                1 -> ChecklistsAnalytics(state)
                2 -> NotesAnalytics(state)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksAnalytics(state: AnalyticsState, viewModel: AnalyticsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Filters Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type Filter
                var expandedType by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = state.taskTypeFilter,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // We use an invisible box overlay to intercept clicks
                    Box(modifier = Modifier.matchParentSize().clickable { expandedType = true })
                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        listOf("All", "Single", "Daily", "Custom").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    viewModel.setTaskTypeFilter(type)
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                // Tag Filter
                var expandedTag by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = state.taskTagFilter ?: "All",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tag") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTag) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expandedTag = true })
                    DropdownMenu(
                        expanded = expandedTag,
                        onDismissRequest = { expandedTag = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All", fontWeight = FontWeight.Bold) },
                            onClick = {
                                viewModel.setTaskTagFilter(null)
                                expandedTag = false
                            }
                        )
                        state.availableTags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag) },
                                onClick = {
                                    viewModel.setTaskTagFilter(tag)
                                    expandedTag = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Metrics
        item { MetricCard(title = "Total Tasks", value = state.totalTasks.toString(), subtitle = "Filtered total active count") }
        item { MetricCard(title = "Past Tasks", value = state.pastTasks.toString(), subtitle = "Single occurrence tasks past their deadline") }
        item { MetricCard(title = "Upcoming Tasks", value = state.upcomingTasks.toString(), subtitle = "Single occurrence tasks scheduled in the future") }
        item { MetricCard(title = "Average Task Length", value = "${state.avgTaskLengthMins} mins", subtitle = "Average scheduled duration per task") }
    }
}

@Composable
fun ChecklistsAnalytics(state: AnalyticsState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { MetricCard(title = "Total Checklists", value = state.totalChecklists.toString()) }
        item { MetricCard(title = "Completed Checklists", value = state.completedChecklists.toString(), subtitle = "Number of checklists with 100% components done") }
        item { MetricCard(title = "Avg Completion Rate", value = String.format("%.1f%%", state.avgCompletionPercentage), subtitle = "Global completion ratio across all objectives") }
    }
}

@Composable
fun NotesAnalytics(state: AnalyticsState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { MetricCard(title = "Total Shorts", value = state.totalShorts.toString()) }
        item { MetricCard(title = "Total Books", value = state.totalBooks.toString()) }
        item { MetricCard(title = "Average Chapters", value = String.format("%.1f", state.avgChaptersPerBook), subtitle = "Average chapters per book") }
        item { MetricCard(title = "Average Pages (Per Chapter)", value = String.format("%.1f", state.avgPagesPerChapter), subtitle = "Average thickness of each given chapter") }
        item { MetricCard(title = "Average Pages (Per Book)", value = String.format("%.1f", state.avgPagesPerBook), subtitle = "Average absolute length of the books") }
    }
}

@Composable
fun MetricCard(title: String, value: String, subtitle: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
