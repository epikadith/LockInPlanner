package com.example.lockinplanner.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import com.example.lockinplanner.R
import com.example.lockinplanner.data.local.entity.ShortEntity
import kotlin.math.abs

@Composable
fun ShortsList(
    shorts: List<ShortEntity>,
    displayMode: Int,
    onShortClick: (ShortEntity) -> Unit,
    onEditShort: (ShortEntity) -> Unit,
    onDeleteShort: (ShortEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (shorts.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.notespagedefault),
                contentDescription = "No shorts yet",
                modifier = Modifier.size(400.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    } else {
        when (displayMode) {
            0 -> {
                // Circular layout (Varying sizes)
                val currentViewConfig = LocalViewConfiguration.current
                val customViewConfig = remember(currentViewConfig) {
                    object : ViewConfiguration by currentViewConfig {
                        override val longPressTimeoutMillis: Long
                            get() = maxOf(100L, currentViewConfig.longPressTimeoutMillis - 200L)
                    }
                }

                var activeMenuShortId by remember { mutableStateOf<String?>(null) }
                val isAnyActive = activeMenuShortId != null

                CompositionLocalProvider(LocalViewConfiguration provides customViewConfig) {
                    Box(modifier = modifier
                        .fillMaxSize()
                        .background(if (isAnyActive) Color.Black.copy(alpha = 0.5f) else Color.Transparent)
                    ) {
                        SimpleFlowRow(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            shorts.forEach { shortItem ->
                                key(shortItem.id) {
                                    var showMenu by remember { mutableStateOf(false) }
                                    if (activeMenuShortId != shortItem.id && showMenu) {
                                        showMenu = false
                                    }
                                val isActive = activeMenuShortId == shortItem.id
                                val sizeDp = 80 + (abs(shortItem.id.hashCode() % 60)) // Safely extract positive offset
                                
                                Box(
                                    modifier = Modifier
                                        .size(sizeDp.dp)
                                        .alpha(if (isAnyActive && !isActive) 0.3f else 1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                        .border(4.dp, Color(shortItem.colorArgb), CircleShape)
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Invisible tap surface over the whole box
                                    Box(modifier = Modifier
                                        .matchParentSize()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    showMenu = true
                                                    activeMenuShortId = shortItem.id
                                                },
                                                onTap = { onShortClick(shortItem) }
                                            )
                                        }
                                    )
                                
                                    // Dropdown Menu positioned within the Box
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = {
                                            showMenu = false
                                            if (activeMenuShortId == shortItem.id) activeMenuShortId = null
                                        }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                showMenu = false
                                                activeMenuShortId = null
                                                onEditShort(shortItem)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete") },
                                            onClick = {
                                                showMenu = false
                                                activeMenuShortId = null
                                                onDeleteShort(shortItem)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                        )
                                    }
                                    
                                    Text(
                                        text = shortItem.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Grid (2 columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = modifier.fillMaxSize()
                ) {
                    items(shorts) { shortItem ->
                        ShortCard(
                            short = shortItem, 
                            onClick = { onShortClick(shortItem) },
                            onEdit = { onEditShort(shortItem) },
                            onDelete = { onDeleteShort(shortItem) }
                        )
                    }
                }
            }
            2 -> {
                // List (1 column)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = modifier.fillMaxSize()
                ) {
                    items(shorts) { shortItem ->
                        ShortCard(
                            short = shortItem, 
                            onClick = { onShortClick(shortItem) },
                            onEdit = { onEditShort(shortItem) },
                            onDelete = { onDeleteShort(shortItem) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShortCard(
    short: ShortEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Color(short.colorArgb), MaterialTheme.shapes.medium)
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = short.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                var expanded by remember { mutableStateOf(false) }
                
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                expanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                expanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleFlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val horizontalSpacingPx = horizontalArrangement.spacing.roundToPx()
        val verticalSpacingPx = verticalArrangement.spacing.roundToPx()

        var currentRowWidth = 0
        var currentRowHeight = 0
        var maxRowWidth = 0
        var totalHeight = 0

        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()
        val placeablesList = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        var currentPlaceables = mutableListOf<androidx.compose.ui.layout.Placeable>()

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            if (currentPlaceables.isNotEmpty() && currentRowWidth + horizontalSpacingPx + placeable.width > constraints.maxWidth) {
                rowWidths.add(currentRowWidth)
                rowHeights.add(currentRowHeight)
                maxRowWidth = maxOf(maxRowWidth, currentRowWidth)
                totalHeight += currentRowHeight + verticalSpacingPx
                placeablesList.add(currentPlaceables)

                currentPlaceables = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }

            currentPlaceables.add(placeable)
            currentRowWidth += if (currentRowWidth == 0) placeable.width else horizontalSpacingPx + placeable.width
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }

        if (currentPlaceables.isNotEmpty()) {
            rowWidths.add(currentRowWidth)
            rowHeights.add(currentRowHeight)
            maxRowWidth = maxOf(maxRowWidth, currentRowWidth)
            totalHeight += currentRowHeight
            placeablesList.add(currentPlaceables)
        }

        layout(maxRowWidth, totalHeight) {
            var yPosition = 0
            placeablesList.forEachIndexed { index, placeables ->
                var xPosition = 0
                val rowHeight = rowHeights[index]
                placeables.forEach { placeable ->
                    val yOffset = (rowHeight - placeable.height) / 2
                    placeable.placeRelative(xPosition, yPosition + yOffset)
                    xPosition += placeable.width + horizontalSpacingPx
                }
                yPosition += rowHeight + verticalSpacingPx
            }
        }
    }
}
