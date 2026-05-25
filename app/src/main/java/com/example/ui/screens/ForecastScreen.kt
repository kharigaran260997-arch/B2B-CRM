package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.ui.CrmViewModel
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    viewModel: CrmViewModel
) {
    val enrichedDeals by viewModel.enrichedDeals.collectAsState()
    val pipelineMetrics by viewModel.pipelineMetrics.collectAsState()

    var activeSubTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pipeline Metrics", "Monthly Projects", "Industry Analytics")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projections & Forecast", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Tab row
            TabRow(selectedTabIndex = activeSubTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeSubTab == index,
                        onClick = { activeSubTab = index },
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (activeSubTab) {
                    0 -> PipelineForecastTab(deals = enrichedDeals, metrics = pipelineMetrics)
                    1 -> MonthlyForecastTab(deals = enrichedDeals)
                    2 -> IndustryForecastTab(deals = enrichedDeals)
                }
            }
        }
    }
}

@Composable
fun PipelineForecastTab(
    deals: List<EnrichedDeal>,
    metrics: PipelineMetrics
) {
    val totalCount = deals.size
    val averageValue = if (totalCount > 0) deals.sumOf { it.deal.dealValue } / totalCount else 0.0
    val wonRevenue = deals.filter { it.deal.stage == Stage.WON }.sumOf { it.deal.dealValue }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KPI Summary cards list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                label = "Total Agreements",
                value = "$totalCount",
                subText = "Active Pipings",
                icon = Icons.Default.Inventory,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = "Win Rate %",
                value = "${String.format("%.1f", metrics.winRate)}%",
                subText = "Successful vs Closed",
                icon = Icons.Default.CheckCircle,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                label = "Average Deal Value",
                value = "₹${String.format("%.1f", averageValue / 100000.0)}L",
                subText = "Mean corporate size",
                icon = Icons.Default.Analytics,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = "Won Revenue Value",
                value = "₹${String.format("%.1f", wonRevenue / 100000.0)}L",
                subText = "Booked assets",
                icon = Icons.Default.MonetizationOn,
                color = Color.Green,
                modifier = Modifier.weight(1f)
            )
        }

        // Weighted Pipeline bar chart by stage
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Weighted Pipeline Projects Value by Stage", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                val maps = Stage.values().associate { stage ->
                    stage.name to deals.filter { it.deal.stage == stage }.sumOf { it.weighted }
                }
                CustomHorizontalBarChart(maps, MaterialTheme.colorScheme.primary)
            }
        }

        // Priority donut
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Segmentation Value Priority Distributions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))

                val maps = Priority.values().associate { priority ->
                    priority.name to deals.filter { it.deal.priority == priority }.sumOf { it.deal.dealValue }
                }
                CustomDonutChart(
                    data = maps,
                    colors = listOf(
                        Color(0xFFFF4D4D), // High
                        Color(0xFFFFA44D), // Medium
                        Color(0xFF378ADD)  // Low
                    )
                )
            }
        }
    }
}

@Composable
fun MonthlyForecastTab(
    deals: List<EnrichedDeal>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Deals Volume Added Trends (6 Months history)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                CustomLineChart(
                    data = listOf(2.0, 3.0, 4.0, 3.0, 6.0, deals.size.toDouble()),
                    labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Current"),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(150.dp)
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Won Deals Cumulative Billings Output", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))

                val maps = mapOf(
                    "Jan Contract" to 450000.0,
                    "Feb Contract" to 1200000.0,
                    "March Contract" to 850000.0,
                    "Current Signed" to deals.filter { it.deal.stage == Stage.WON }.sumOf { it.deal.dealValue }
                )
                CustomHorizontalBarChart(maps, Color.Green)
            }
        }
    }
}

@Composable
fun IndustryForecastTab(
    deals: List<EnrichedDeal>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Industry Vertical Distributions Map", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))

                val maps = deals.filter { it.deal.industry.isNotBlank() }
                    .groupBy { it.deal.industry }
                    .mapValues { it.value.size }
                CustomPieChart(
                    data = maps,
                    colors = listOf(
                        Color(0xFF378ADD),
                        Color(0xFF7F77DD),
                        Color(0xFF1D9E75),
                        Color(0xFFEF9F27),
                        Color(0xFFD4537E),
                        Color.Gray
                    )
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Lead Ingestion Channels Splits", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))

                val maps = mapOf(
                    "Inbound Marketing" to 15,
                    "Referral Network" to 6,
                    "Outbound Outreach" to 11,
                    "Partner Integration" to 4
                )
                CustomPieChart(
                    data = maps,
                    colors = listOf(
                        Color(0xFF1D9E75),
                        Color(0xFF378ADD),
                        Color(0xFF7F77DD),
                        Color(0xFFEF9F27)
                    )
                )
            }
        }
    }
}
