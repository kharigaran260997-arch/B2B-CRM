package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.ui.CrmViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PipelineScreen(
    viewModel: CrmViewModel,
    onNavigateToDealDetail: (String) -> Unit,
    onNavigateToAddDeal: () -> Unit
) {
    val filteredDeals by viewModel.filteredDeals.collectAsState()
    val teamMembers by viewModel.teamMembers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val selectedStageFilter by viewModel.selectedStageFilter.collectAsState()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsState()
    val selectedPriorityFilter by viewModel.selectedPriorityFilter.collectAsState()
    val selectedOwnerFilter by viewModel.selectedOwnerFilter.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()

    var isKanbanMode by remember { mutableStateOf(false) }

    // Bulk action states
    var isBulkEditMode by remember { mutableStateOf(false) }
    val selectedDealIds = remember { mutableStateListOf<String>() }

    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deal Pipeline", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { isKanbanMode = !isKanbanMode }) {
                        Icon(
                            imageVector = if (isKanbanMode) Icons.Default.ViewList else Icons.Default.ViewColumn,
                            contentDescription = "Toggle Layout"
                        )
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Open Filters")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (!isBulkEditMode) {
                FloatingActionButton(
                    onClick = onNavigateToAddDeal,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Deal")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search input line
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search by pipeline, company, or mail...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // Inline filter view status/stage indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedStageFilter != null) {
                        InputChip(
                            selected = true,
                            onClick = { viewModel.selectedStageFilter.value = null },
                            label = { Text("Stage: ${selectedStageFilter!!.name}") },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                    }
                    if (selectedStatusFilter != null) {
                        InputChip(
                            selected = true,
                            onClick = { viewModel.selectedStatusFilter.value = null },
                            label = { Text("Status: ${selectedStatusFilter!!.name}") },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                    }
                    if (selectedPriorityFilter != null) {
                        InputChip(
                            selected = true,
                            onClick = { viewModel.selectedPriorityFilter.value = null },
                            label = { Text("Priority: ${selectedPriorityFilter!!.name}") },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                    }
                }

                if (isBulkEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${selectedDealIds.size} deals selected", fontWeight = FontWeight.Bold)
                        TextButton(onClick = {
                            isBulkEditMode = false
                            selectedDealIds.clear()
                        }) {
                            Text("Cancel Bulk")
                        }
                    }
                }

                if (isKanbanMode) {
                    KanbanLayout(
                        deals = filteredDeals,
                        isBulkEditMode = isBulkEditMode,
                        selectedDealIds = selectedDealIds,
                        onDealClick = { onNavigateToDealDetail(it) },
                        onDealSelect = { id ->
                            if (selectedDealIds.contains(id)) selectedDealIds.remove(id) else selectedDealIds.add(id)
                        },
                        onLongClick = { id ->
                            isBulkEditMode = true
                            selectedDealIds.add(id)
                        }
                    )
                } else {
                    ListLayout(
                        deals = filteredDeals,
                        isBulkEditMode = isBulkEditMode,
                        selectedDealIds = selectedDealIds,
                        onDealClick = { onNavigateToDealDetail(it) },
                        onDealSelect = { id ->
                            if (selectedDealIds.contains(id)) selectedDealIds.remove(id) else selectedDealIds.add(id)
                        },
                        onLongClick = { id ->
                            isBulkEditMode = true
                            selectedDealIds.add(id)
                        },
                        onQuickCallLog = { id ->
                            viewModel.logActivity(
                                Activity(
                                    dealId = id,
                                    type = ActivityType.CALL,
                                    text = "Quick Outgoing telephone action completed."
                                )
                            )
                        },
                        onQuickLost = { id ->
                            viewModel.updateDealStageInline(id, Stage.LOST)
                        }
                    )
                }
            }

            // Bulk edit bottom actions
            if (isBulkEditMode && selectedDealIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        IconButton(onClick = {
                            selectedDealIds.forEach { viewModel.updateDealStageInline(it, Stage.WON) }
                            selectedDealIds.clear()
                            isBulkEditMode = false
                        }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Won Selected", tint = Color(0xFF1D9E75))
                        }

                        IconButton(onClick = {
                            selectedDealIds.forEach { viewModel.updateDealStageInline(it, Stage.LOST) }
                            selectedDealIds.clear()
                            isBulkEditMode = false
                        }) {
                            Icon(Icons.Default.Cancel, contentDescription = "Lost Selected", tint = Color.Red)
                        }

                        IconButton(onClick = {
                            selectedDealIds.forEach { viewModel.deleteDeal(it) }
                            selectedDealIds.clear()
                            isBulkEditMode = false
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color.LightGray)
                        }
                    }
                }
            }

            // Dynamic filter modal
            if (showFilterSheet) {
                AlertDialog(
                    onDismissRequest = { showFilterSheet = false },
                    title = { Text("Filter & Sort Solutions") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Stage Filter Selection
                            Column {
                                Text("Filter Stage", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Stage.values().forEach { stage ->
                                        FilterChip(
                                            selected = selectedStageFilter == stage,
                                            onClick = { viewModel.selectedStageFilter.value = if (selectedStageFilter == stage) null else stage },
                                            label = { Text(stage.name, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            // Status Filter Selection
                            Column {
                                Text("Filter Health Status", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    DealStatus.values().forEach { status ->
                                        FilterChip(
                                            selected = selectedStatusFilter == status,
                                            onClick = { viewModel.selectedStatusFilter.value = if (selectedStatusFilter == status) null else status },
                                            label = { Text(status.name, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            // Selection Sort Parameters
                            Column {
                                Text("Sort Pipeline Listings", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                val sorts = listOf("Priority Score", "Deal Value", "Staleness", "Close Date")
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    sorts.forEach { opt ->
                                        FilterChip(
                                            selected = sortBy == opt,
                                            onClick = { viewModel.sortBy.value = opt },
                                            label = { Text(opt, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showFilterSheet = false }) {
                            Text("Dismiss Setup")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListLayout(
    deals: List<EnrichedDeal>,
    isBulkEditMode: Boolean,
    selectedDealIds: List<String>,
    onDealClick: (String) -> Unit,
    onDealSelect: (String) -> Unit,
    onLongClick: (String) -> Unit,
    onQuickCallLog: (String) -> Unit,
    onQuickLost: (String) -> Unit
) {
    if (deals.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions match current filters", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(deals, key = { it.deal.id }) { enriched ->
            val isSelected = selectedDealIds.contains(enriched.deal.id)
            PipelineDealCard(
                enriched = enriched,
                isSelected = isSelected,
                isBulkEditMode = isBulkEditMode,
                onClick = {
                    if (isBulkEditMode) onDealSelect(enriched.deal.id) else onDealClick(enriched.deal.id)
                },
                onLongClick = { onLongClick(enriched.deal.id) },
                onQuickCallLog = { onQuickCallLog(enriched.deal.id) },
                onQuickLost = { onQuickLost(enriched.deal.id) }
            )
        }
        item { Box(modifier = Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KanbanLayout(
    deals: List<EnrichedDeal>,
    isBulkEditMode: Boolean,
    selectedDealIds: List<String>,
    onDealClick: (String) -> Unit,
    onDealSelect: (String) -> Unit,
    onLongClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Stage.values().forEach { stage ->
            val columnDeals = deals.filter { it.deal.stage == stage }

            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                // Header of Column
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stage.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Badge { Text(columnDeals.size.toString()) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(columnDeals) { enriched ->
                        val isSelected = selectedDealIds.contains(enriched.deal.id)
                        PipelineDealCard(
                            enriched = enriched,
                            isSelected = isSelected,
                            isBulkEditMode = isBulkEditMode,
                            onClick = {
                                if (isBulkEditMode) onDealSelect(enriched.deal.id) else onDealClick(enriched.deal.id)
                            },
                            onLongClick = { onLongClick(enriched.deal.id) },
                            onQuickCallLog = {},
                            onQuickLost = {},
                            simpleKanbanStyle = true
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PipelineDealCard(
    enriched: EnrichedDeal,
    isSelected: Boolean,
    isBulkEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQuickCallLog: () -> Unit,
    onQuickLost: () -> Unit,
    simpleKanbanStyle: Boolean = false
) {
    val statusColor = when (enriched.status) {
        DealStatus.CRITICAL -> Color(0xFFFF4D4D)
        DealStatus.STALE -> Color(0xFFFFA44D)
        DealStatus.ACTIONLESS -> Color(0xFF6A6A6A)
        DealStatus.HEALTHY -> Color(0xFF1D9E75)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Status Side Border Color Identifier
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .height(if (simpleKanbanStyle) 120.dp else 145.dp)
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        enriched.deal.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    if (isBulkEditMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() },
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        val priorityColor = when (enriched.deal.priority) {
                            Priority.HIGH -> Color(0xFFFF4D4D)
                            Priority.MEDIUM -> Color(0xFFFFA44D)
                            Priority.LOW -> Color(0xFF378ADD)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(priorityColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(enriched.deal.priority.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = priorityColor)
                        }
                    }
                }

                Text(enriched.deal.company, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Value", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("₹${String.format("%,.0f", enriched.deal.dealValue)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Weighted", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("₹${String.format("%,.0f", enriched.weighted)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (!simpleKanbanStyle) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Text("Stale: ${enriched.daysStale}d", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.Yellow)
                            Text("Momentum: ${enriched.momentum}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Text("Owner: ${enriched.deal.owner}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }

                    // Swipe/Quick Action shortcuts for simple list layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onQuickCallLog, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Call, contentDescription = "Log call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = onQuickLost, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Mark Lost", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
