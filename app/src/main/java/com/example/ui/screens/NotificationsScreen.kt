package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class LocalAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val dealId: String,
    val dealName: String,
    val type: AlertType,
    val message: String
)

enum class AlertType { CRITICAL, STALE, TASK_OVERDUE, REMINDER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: CrmViewModel,
    onNavigateToDealDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val enrichedDeals by viewModel.enrichedDeals.collectAsState()

    // Internal state to manage swipe dismiss updates
    val alertsList = remember(enrichedDeals) {
        val list = mutableListOf<LocalAlert>()
        enrichedDeals.forEach { d ->
            when {
                d.status == DealStatus.CRITICAL -> {
                    list.add(
                        LocalAlert(
                            dealId = d.deal.id,
                            dealName = d.deal.name,
                            type = AlertType.CRITICAL,
                            message = "🚨 CRITICAL: Deal is stalling! (${d.daysStale} days stale)"
                        )
                    )
                }
                d.status == DealStatus.STALE -> {
                    list.add(
                        LocalAlert(
                            dealId = d.deal.id,
                            dealName = d.deal.name,
                            type = AlertType.STALE,
                            message = "⚠️ WARNING: No activities for ${d.daysStale} days."
                        )
                    )
                }
                !d.hasActiveTask && d.deal.stage != Stage.WON && d.deal.stage != Stage.LOST -> {
                    list.add(
                        LocalAlert(
                            dealId = d.deal.id,
                            dealName = d.deal.name,
                            type = AlertType.TASK_OVERDUE,
                            message = "📋 ACTION REQUIRED: Active sales tasks are missing."
                        )
                    )
                }
            }

            if (!d.deal.nextFollowUp.isNullOrBlank()) {
                list.add(
                    LocalAlert(
                        dealId = d.deal.id,
                        dealName = d.deal.name,
                        type = AlertType.REMINDER,
                        message = "📆 REMINDER: Follow up scheduled for ${d.deal.nextFollowUp}."
                    )
                )
            }
        }
        list
    }

    val activeAlertStates = remember { mutableStateListOf<LocalAlert>() }

    LaunchedEffect(alertsList) {
        activeAlertStates.clear()
        activeAlertStates.addAll(alertsList)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Alerts Desk", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Navigate Back")
                    }
                },
                actions = {
                    TextButton(onClick = { activeAlertStates.clear() }) {
                        Text("Dismiss All", color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (activeAlertStates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Green
                    )
                    Text("Horizon is clear. No pipeline slippage logged!", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        } else {
            val groupedAlerts = activeAlertStates.groupBy { it.type }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedAlerts.forEach { (type, alerts) ->
                    item {
                        Text(
                            text = when (type) {
                                AlertType.CRITICAL -> "🚨 CRITICAL PIPELINE SLIPPAGE"
                                AlertType.STALE -> "⏳ STALE NEGOTIATIONS"
                                AlertType.TASK_OVERDUE -> "📋 GAPING MILESTONES"
                                AlertType.REMINDER -> "📆 CALENDAR TIMELINES"
                            },
                            fontWeight = FontWeight.Bold,
                            color = when (type) {
                                AlertType.CRITICAL -> MaterialTheme.colorScheme.error
                                AlertType.STALE -> Color(0xFFFFA44D)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(alerts, key = { it.id }) { alert ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDealDetail(alert.dealId) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(alert.dealName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(alert.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }

                                IconButton(onClick = { activeAlertStates.remove(alert) }) {
                                    Icon(Icons.Default.Check, contentDescription = "Dismiss", tint = Color.LightGray)
                                }
                            }
                        }
                    }
                }
                item { Box(modifier = Modifier.height(60.dp)) }
            }
        }
    }
}
