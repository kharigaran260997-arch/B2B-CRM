package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.components.CustomPieChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    viewModel: CrmViewModel,
    onNavigateToDealDetail: (String) -> Unit
) {
    val enrichedDeals by viewModel.enrichedDeals.collectAsState()
    val teamMembers by viewModel.teamMembers.collectAsState()

    // Aggregate statistics per member
    val leaderboardList = teamMembers.map { member ->
        val memberDeals = enrichedDeals.filter { it.deal.owner == member }
        val rawValueTotal = memberDeals.sumOf { it.deal.dealValue }
        val wonCount = memberDeals.count { it.deal.stage == Stage.WON }
        val totalDealsCount = memberDeals.size

        MemberStats(
            name = member,
            allDealsCount = totalDealsCount,
            wonCount = wonCount,
            pipelineValue = rawValueTotal,
            deals = memberDeals
        )
    }.sortedByDescending { it.pipelineValue }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Leaderboard", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Top-Performing Enterprise Sales Agents", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // Render Leaderboard Cards
            leaderboardList.forEachIndexed { index, m ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (index) {
                                                0 -> Color(0xFFFFD700) // Gold
                                                1 -> Color(0xFFC0C0C0) // Silver
                                                2 -> Color(0xFFCD7F32) // Bronze
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                Column {
                                    Text(m.name, fontWeight = FontWeight.Bold)
                                    Text("Owned: ${m.allDealsCount} (Won: ${m.wonCount})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${String.format("%,.0f", m.pipelineValue)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Pipeline Managed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        if (m.deals.isNotEmpty()) {
                            // Mini horizontal listing of member deals
                            Text("Active Member Agreements:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(m.deals) { enriched ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                        modifier = Modifier
                                            .clickable { onNavigateToDealDetail(enriched.deal.id) }
                                            .width(180.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(enriched.deal.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Text("₹${String.format("%,.0f", enriched.deal.dealValue)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Stage: ${enriched.deal.stage}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pie chart for pipe distribution by owner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pipeline Wealth Distribution by Owner", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val ownerValueDistribution = leaderboardList.associate { it.name to it.allDealsCount }
                    CustomPieChart(
                        data = ownerValueDistribution,
                        colors = listOf(
                            Color(0xFF378ADD),
                            Color(0xFF7F77DD),
                            Color(0xFF1D9E75),
                            Color(0xFFEF9F27),
                            Color(0xFFCD537E)
                        )
                    )
                }
            }

            Box(modifier = Modifier.height(60.dp))
        }
    }
}

data class MemberStats(
    val name: String,
    val allDealsCount: Int,
    val wonCount: Int,
    val pipelineValue: Double,
    val deals: List<EnrichedDeal>
)
