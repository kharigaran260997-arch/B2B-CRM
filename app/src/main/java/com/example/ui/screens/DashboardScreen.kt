package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
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
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CrmViewModel,
    onNavigateToDealDetail: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAddDeal: () -> Unit
) {
    val enrichedDeals by viewModel.enrichedDeals.collectAsState()
    val pipelineMetrics by viewModel.pipelineMetrics.collectAsState()
    val revenueTarget by viewModel.revenueTarget.collectAsState()

    // Determine alerts
    val alertDeals = enrichedDeals.filter {
        it.status == DealStatus.CRITICAL ||
                it.status == DealStatus.STALE ||
                (!it.hasActiveTask && it.deal.stage != Stage.WON && it.deal.stage != Stage.LOST)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revenue Command Center", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                if (alertDeals.isNotEmpty()) {
                                    Badge { Text(alertDeals.size.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Alerts Hub")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddDeal,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Deal")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Deal", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // KPI Grid (2x2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiCard(
                            label = "Weighted Pipeline",
                            value = "₹${String.format("%.1f", pipelineMetrics.weightedTotal / 100000.0)}L",
                            subText = "Raw: ₹${String.format("%.1f", pipelineMetrics.rawTotal / 100000.0)}L",
                            icon = Icons.Default.BarChart,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = "Realization %",
                            value = "${String.format("%.1f", pipelineMetrics.realization)}%",
                            subText = "Weighted ÷ Raw",
                            icon = Icons.Default.OfflineBolt,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val gapString = if (pipelineMetrics.gap <= 0) {
                            "Target Met! 🎉"
                        } else {
                            "₹${String.format("%.1f", pipelineMetrics.gap / 100000.0)}L"
                        }
                        KpiCard(
                            label = "Target Gap",
                            value = gapString,
                            subText = "Goal: ₹${String.format("%.1f", revenueTarget / 100000.0)}L",
                            icon = Icons.Default.WorkspacePremium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = "Slipping Deals",
                            value = "${alertDeals.size}",
                            subText = "Stale or Actionless",
                            icon = Icons.Default.Warning,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Target Progress Bar
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target Progress Tracker", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            val progressPercent = (pipelineMetrics.weightedTotal / revenueTarget).coerceIn(0.0, 1.0) * 100.0
                            Text("${String.format("%.1f", progressPercent)}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val progress = (pipelineMetrics.weightedTotal / revenueTarget).toFloat().coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.background
                        )
                    }
                }
            }

            // Alert Banner list
            if (alertDeals.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Active Alerts Hub", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(alertDeals) { enriched ->
                                AlertDealChip(
                                    enriched = enriched,
                                    onClick = { onNavigateToDealDetail(enriched.deal.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Custom Charts Panel
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("Pipeline Stage Value Distribution", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        // Compute data for stages bar chart
                        val stageValuesMap = Stage.values().associate { stage ->
                            stage.name to enrichedDeals.filter { it.deal.stage == stage }.sumOf { it.deal.dealValue }
                        }
                        CustomHorizontalBarChart(stageValuesMap, MaterialTheme.colorScheme.primary)

                        HorizontalDivider(color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f))

                        Text("Deals Volume Distribution", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        // Compute stage deal counts
                        val stageCountsMap = Stage.values().filter { stage ->
                            enrichedDeals.any { it.deal.stage == stage }
                        }.associate { stage ->
                            stage.name to enrichedDeals.count { it.deal.stage == stage }
                        }
                        CustomPieChart(
                            data = stageCountsMap,
                            colors = listOf(
                                Color(0xFF7F77DD),
                                Color(0xFF378ADD),
                                Color(0xFFEF9F27),
                                Color(0xFFD4537E),
                                Color(0xFF1D9E75),
                                Color(0xFF444444)
                            )
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f))

                        Text("Pipeline Added Trend (Last 6 Months)", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        // Trend computation
                        val trendLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
                        val trendValues = listOf(1200000.0, 1500000.0, 2400000.0, 1800000.0, 3100000.0, pipelineMetrics.rawTotal)
                        CustomLineChart(
                            data = trendValues,
                            labels = trendLabels,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.height(140.dp)
                        )
                    }
                }
            }

            // Top Deals List
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Top 5 Pipeline Agreements", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    val topDeals = enrichedDeals.sortedByDescending { it.deal.dealValue }.take(5)
                    if (topDeals.isEmpty()) {
                        Text("No pipeline deals located", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            topDeals.forEach { enriched ->
                                TopDealRow(
                                    enriched = enriched,
                                    onClick = { onNavigateToDealDetail(enriched.deal.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Margin bottom
            item { Box(modifier = Modifier.height(60.dp)) }
        }
    }
}

@Composable
fun KpiCard(
    label: String,
    value: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(subText, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 10.sp)
        }
    }
}

@Composable
fun AlertDealChip(
    enriched: EnrichedDeal,
    onClick: () -> Unit
) {
    val borderColor = when (enriched.status) {
        DealStatus.CRITICAL -> Color(0xFFFF4D4D)
        DealStatus.STALE -> Color(0xFFFFA44D)
        else -> Color(0xFF6A6A6A)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .widthIn(max = 240.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(borderColor, RoundedCornerShape(4.dp))
            )
            Column {
                Text(enriched.deal.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("Stale: ${enriched.daysStale} days | ${enriched.deal.stage}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun TopDealRow(
    enriched: EnrichedDeal,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(enriched.deal.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(enriched.deal.company, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${String.format("%,.0f", enriched.deal.dealValue)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Stage: ${enriched.deal.stage}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
