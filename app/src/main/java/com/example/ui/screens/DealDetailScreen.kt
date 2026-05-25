package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.ui.CrmViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealDetailScreen(
    dealId: String,
    viewModel: CrmViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val enrichedDealState = viewModel.getEnrichedDeal(dealId).collectAsState(initial = null)
    val activities by viewModel.getActivities(dealId).collectAsState(initial = emptyList())
    val tasks by viewModel.getTasks(dealId).collectAsState(initial = emptyList())
    val comments by viewModel.getComments(dealId).collectAsState(initial = emptyList())

    val aiResults by viewModel.aiCoachingResults.collectAsState()
    val aiLoadings by viewModel.aiCoachingLoading.collectAsState()

    val intelResults by viewModel.contactIntelResults.collectAsState()
    val intelLoadings by viewModel.contactIntelLoading.collectAsState()

    var showCallScriptSheet by remember { mutableStateOf(false) }
    var showAddTaskSheet by remember { mutableStateOf(false) }

    // Comment input state
    var newCommentText by remember { mutableStateOf("") }

    // Task input states
    var newTaskText by remember { mutableStateOf("") }
    var newTaskDueDate by remember { mutableStateOf(LocalDate.now().plusDays(3).toString()) }

    val enriched = enrichedDealState.value
    if (enriched == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val deal = enriched.deal

    // Trigger AI analysis on layout boot
    LaunchedEffect(dealId) {
        if (!aiResults.containsKey(dealId)) {
            viewModel.requestDealCoachAnalysis(enriched, activities, tasks)
        }
        if (!intelResults.containsKey(deal.name)) {
            viewModel.requestContactEnrichment(deal.name, deal.company)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deal.name, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(deal.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Contact and Deal")
                    }
                    IconButton(onClick = {
                        viewModel.deleteDeal(deal.id)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Deal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Contact Block
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = deal.name.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Column {
                                Text(deal.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${deal.title} at ${deal.company}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Email: ${deal.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Phone: ${deal.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Deal Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(label = "Raw Value", value = "₹${String.format("%,.0f", deal.dealValue)}", modifier = Modifier.weight(1f))
                    StatBox(label = "Weighted", value = "₹${String.format("%,.0f", enriched.weighted)}", modifier = Modifier.weight(1f))
                    StatBox(label = "Momentum", value = "${enriched.momentum}", modifier = Modifier.weight(1f))
                    StatBox(label = "Days Stale", value = "${enriched.daysStale}d", modifier = Modifier.weight(1f))
                }
            }

            // Pipeline Stage Timeline Selector
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pipeline Workflow Timeline", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Stage.values().forEach { stage ->
                                val isPassed = stage.ordinal <= deal.stage.ordinal
                                val tintColor = if (isPassed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(tintColor.copy(alpha = 0.15f))
                                        .border(2.dp, tintColor, CircleShape)
                                        .clickable { viewModel.updateDealStageInline(deal.id, stage) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (stage.ordinal + 1).toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isPassed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Inline movement button shortcuts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val nextOrdinal = deal.stage.ordinal + 1
                            if (nextOrdinal < Stage.values().size) {
                                val nextStage = Stage.values()[nextOrdinal]
                                Button(
                                    onClick = { viewModel.updateDealStageInline(deal.id, nextStage) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Advance to ${nextStage.name}", fontSize = 11.sp)
                                }
                            } else {
                                Box {}
                            }

                            OutlinedButton(
                                onClick = { viewModel.updateDealStageInline(deal.id, Stage.LOST) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark Lost", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Communication Hub Row (2x2 action grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Communication Hub Templates", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${deal.email}")
                                    putExtra(Intent.EXTRA_SUBJECT, "Next Steps: RCC Sales Alignment - ${deal.company}")
                                    putExtra(Intent.EXTRA_TEXT, "Hi ${deal.name},\n\nHope you are doing well. I wanted to touch base regarding our enterprise solutions proposal. Let us configure a session next week.")
                                }
                                context.startActivity(Intent.createChooser(intent, "Select Mail Client"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Email Template", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                val msgPrefix = "Hi ${deal.name}, Kari here from sales. Regarding our alignment discussed for ${deal.company}, are you free today?"
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://wa.me/${deal.phone}?text=${Uri.encode(msgPrefix)}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 10.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showCallScriptSheet = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Duo, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call Script", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${deal.phone}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Call Logs", fontSize = 10.sp)
                        }
                    }
                }
            }

            // AI Deal Coach Cards (Calls Gemini Model)
            item {
                val isLoading = aiLoadings[dealId] ?: false
                val coachResult = aiResults[dealId]

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Gemini AI Deal Coach", fontWeight = FontWeight.Bold)
                            }

                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                TextButton(
                                    onClick = { viewModel.requestDealCoachAnalysis(enriched, activities, tasks) }
                                ) {
                                    Text("Refresh", fontSize = 11.sp)
                                }
                            }
                        }

                        if (coachResult != null) {
                            // Display Score and Risks
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Deal Health Score", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${coachResult.score}/100", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Risk Matrix", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    val rColor = when (coachResult.risk.uppercase()) {
                                        "HIGH" -> Color.Red
                                        "MEDIUM" -> Color.Yellow
                                        else -> Color.Green
                                    }
                                    Text(coachResult.risk, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = rColor)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🚨 RECOMMENDED NEXT ACTION", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                                    Text(coachResult.nextAction, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }

                            Text("Reasoning Analysis:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Text(coachResult.reasoning, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                            Text("Suggested Sales Tactics:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            coachResult.tactics.forEach { play ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("⚡", fontSize = 12.sp)
                                    Text(play, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        } else if (!isLoading) {
                            Button(
                                onClick = { viewModel.requestDealCoachAnalysis(enriched, activities, tasks) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Trigger AI Strategy Coach")
                            }
                        }
                    }
                }
            }

            // Contact Intelligence / Enrichment Section
            item {
                val isLoading = intelLoadings[deal.name] ?: false
                val intel = intelResults[deal.name]

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Text("Intelligence & Firmographics", fontWeight = FontWeight.Bold)
                            }
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            }
                        }

                        if (intel != null) {
                            Text("Profile: ${intel.title}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text("Company Size", fontSize = 10.sp)
                                    Text(intel.companySize, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Column {
                                    Text("Estimated Revenue", fontSize = 10.sp)
                                    Text(intel.annualRevenue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }

                            Text("Buying Signals & Tech Triggers:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Text(intel.buyingSignals, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                            Text("Personalized Hook Points (Conversational Icebreakers):", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            intel.talkingPoints.forEach { point ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🎯", fontSize = 11.sp)
                                    Text(point, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        } else if (!isLoading) {
                            Button(
                                onClick = { viewModel.requestContactEnrichment(deal.name, deal.company) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("Retrieve Corporate Intel Profile")
                            }
                        }
                    }
                }
            }

            // Task list section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Actions Plan Tasks", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showAddTaskSheet = true }) {
                                Icon(Icons.Default.AddTask, contentDescription = "Add Task")
                            }
                        }

                        if (tasks.isEmpty()) {
                            Text("No pending milestone tasks are set.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        } else {
                            tasks.forEach { task ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Checkbox(
                                            checked = task.isDone,
                                            onCheckedChange = { viewModel.toggleTask(task.id, it) }
                                        )
                                        Column {
                                            Text(
                                                task.text,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (task.isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
                                            )
                                            if (!task.dueDate.isNullOrBlank()) {
                                                Text("Due: ${task.dueDate}", fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Custom fields sections (Displays user-defined custom metadata)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Custom Enterprise Metadata", fontWeight = FontWeight.Bold)

                        if (deal.customFields.isEmpty()) {
                            Text("No custom specifications mapped to this entity. Edit Deal to define.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        } else {
                            deal.customFields.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(key, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(value, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Comments Feed Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Discussion & Log Activities Comments", fontWeight = FontWeight.Bold)

                        // Input field to add comments
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Log collaborative notes here...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("comment_input_box"),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (newCommentText.isNotBlank()) {
                                            viewModel.addComment(
                                                Comment(
                                                    dealId = deal.id,
                                                    author = viewModel.teamMembers.value.firstOrNull() ?: "Self",
                                                    text = newCommentText
                                                )
                                            )
                                            newCommentText = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send")
                                }
                            }
                        )

                        // Comments display lists
                        if (comments.isEmpty()) {
                            Text("No shared comments logged yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        } else {
                            comments.forEach { r ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(r.author, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(r.createdAt.take(16).replace("T", " "), fontSize = 9.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(r.text, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom space
            item { Box(modifier = Modifier.height(80.dp)) }
        }

        // Call Script Bottom Alert
        if (showCallScriptSheet) {
            AlertDialog(
                onDismissRequest = { showCallScriptSheet = false },
                title = { Text("RCC Cold Alignment Script") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Step 1: Introduction", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("\"Hi ${deal.name}, Kari G. calling from Enterprise Systems. I saw your recent distributive regional expansion program...\"", fontSize = 11.sp)
                        Text("Step 2: Connect Pain Points", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("\"Our pipeline monitoring software resolves exact supply latency patterns you mentioned...\"", fontSize = 11.sp)
                        Text("Step 3: Secure Appointment", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("\"Are you available for a 10 min alignment sync next Tuesday?\"", fontSize = 11.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = { showCallScriptSheet = false }) {
                        Text("Understood")
                    }
                }
            )
        }

        // Add Task Alert dialog
        if (showAddTaskSheet) {
            AlertDialog(
                onDismissRequest = { showAddTaskSheet = false },
                title = { Text("Log Next Milestone Task") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newTaskText,
                            onValueChange = { newTaskText = it },
                            label = { Text("Task description") },
                            modifier = Modifier.fillMaxWidth().testTag("add_task_text_field")
                        )

                        OutlinedTextField(
                            value = newTaskDueDate,
                            onValueChange = { newTaskDueDate = it },
                            label = { Text("Due Date (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTaskText.isNotBlank()) {
                                viewModel.addTask(
                                    Task(
                                        dealId = dealId,
                                        text = newTaskText,
                                        dueDate = newTaskDueDate
                                    )
                                )
                                newTaskText = ""
                                showAddTaskSheet = false
                            }
                        }
                    ) {
                        Text("Create Task")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTaskSheet = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
