package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.ui.CrmViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDealScreen(
    dealId: String?,
    viewModel: CrmViewModel,
    onNavigateBack: () -> Unit
) {
    val enrichedState = if (dealId != null) {
        viewModel.getEnrichedDeal(dealId).collectAsState(initial = null).value
    } else {
        null
    }

    val teamMembers by viewModel.teamMembers.collectAsState()
    val customFieldsList by viewModel.customFields.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Contact Info", "Deal Metrics", "Intel & Custom")

    // Form states
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var dealValue by remember { mutableStateOf("0.0") }
    var currency by remember { mutableStateOf("₹") }
    var stage by remember { mutableStateOf(Stage.LEAD) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var industry by remember { mutableStateOf("Technology") }
    var source by remember { mutableStateOf("Inbound") }
    var owner by remember { mutableStateOf("Self") }
    var isDecisionMaker by remember { mutableStateOf(false) }
    var isBudgetConfirmed by remember { mutableStateOf(false) }
    var competitors by remember { mutableStateOf("") }
    var painPoints by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var nextFollowUp by remember { mutableStateOf(LocalDate.now().plusDays(2).toString()) }
    var closingDate by remember { mutableStateOf(LocalDate.now().plusWeeks(4).toString()) }
    var rating by remember { mutableIntStateOf(3) }
    var annualRevenue by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var linkedin by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // Dynamic custom fields maps values
    val customFieldValues = remember { mutableStateMapOf<String, String>() }

    // Hydrate form if editing
    LaunchedEffect(enrichedState) {
        enrichedState?.deal?.let { deal ->
            name = deal.name
            company = deal.company
            email = deal.email
            phone = deal.phone
            title = deal.title
            dealValue = deal.dealValue.toString()
            currency = deal.currency
            stage = deal.stage
            priority = deal.priority
            industry = deal.industry
            source = deal.source
            owner = deal.owner
            isDecisionMaker = deal.isDecisionMaker
            isBudgetConfirmed = deal.isBudgetConfirmed
            competitors = deal.competitors
            painPoints = deal.painPoints
            notes = deal.notes
            trialDate { nextFollowUp = deal.nextFollowUp ?: "" }
            trialDate { closingDate = deal.closingDate ?: "" }
            rating = deal.rating
            annualRevenue = deal.annualRevenue
            website = deal.website
            linkedin = deal.linkedin
            address = deal.address
            deal.customFields.forEach { (k, v) -> customFieldValues[k] = v }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (dealId == null) "Create Deal Pipeline" else "Modify Deal Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Navigate Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                val finalCustomFields = customFieldValues.toMap()
                                val finalizedDeal = Deal(
                                    id = dealId ?: java.util.UUID.randomUUID().toString(),
                                    name = name,
                                    company = company,
                                    email = email,
                                    phone = phone,
                                    title = title,
                                    dealValue = dealValue.toDoubleOrNull() ?: 0.0,
                                    currency = currency,
                                    stage = stage,
                                    priority = priority,
                                    industry = industry,
                                    source = source,
                                    owner = if (owner == "Self") (teamMembers.firstOrNull() ?: "Self") else owner,
                                    isDecisionMaker = isDecisionMaker,
                                    isBudgetConfirmed = isBudgetConfirmed,
                                    competitors = competitors,
                                    painPoints = painPoints,
                                    notes = notes,
                                    nextFollowUp = nextFollowUp.takeIf { it.isNotBlank() },
                                    closingDate = closingDate.takeIf { it.isNotBlank() },
                                    rating = rating,
                                    annualRevenue = annualRevenue,
                                    website = website,
                                    linkedin = linkedin,
                                    address = address,
                                    customFields = finalCustomFields,
                                    createdAt = enrichedState?.deal?.createdAt ?: java.time.LocalDateTime.now().toString(),
                                    isSynced = false
                                )
                                viewModel.saveDeal(finalizedDeal)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("submit_save_deal_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                },
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
            // Tab Header Row
            TabRow(selectedTabIndex = activeTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Tabs container columns
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    0 -> ContactTab(
                        name = name, onNameChange = { name = it },
                        company = company, onCompanyChange = { company = it },
                        email = email, onEmailChange = { email = it },
                        phone = phone, onPhoneChange = { phone = it },
                        title = title, onTitleChange = { title = it },
                        website = website, onWebsiteChange = { website = it },
                        linkedin = linkedin, onLinkedinChange = { linkedin = it },
                        address = address, onAddressChange = { address = it },
                        annualRevenue = annualRevenue, onAnnualRevenueChange = { annualRevenue = it },
                        industry = industry, onIndustryChange = { industry = it },
                        source = source, onSourceChange = { source = it },
                        owner = owner, onOwnerChange = { owner = it },
                        teamMembers = teamMembers
                    )
                    1 -> DealTab(
                        stage = stage, onStageChange = { stage = it },
                        priority = priority, onPriorityChange = { priority = it },
                        currency = currency, onCurrencyChange = { currency = it },
                        dealValue = dealValue, onDealValueChange = { dealValue = it },
                        nextFollowUp = nextFollowUp, onNextFollowUpChange = { nextFollowUp = it },
                        closingDate = closingDate, onClosingDateChange = { closingDate = it },
                        rating = rating, onRatingChange = { rating = it },
                        isDecisionMaker = isDecisionMaker, onDecisionMakerChange = { isDecisionMaker = it },
                        isBudgetConfirmed = isBudgetConfirmed, onBudgetConfirmedChange = { isBudgetConfirmed = it }
                    )
                    2 -> IntelTab(
                        competitors = competitors, onCompetitorsChange = { competitors = it },
                        painPoints = painPoints, onPainPointsChange = { painPoints = it },
                        notes = notes, onNotesChange = { notes = it },
                        customFieldsList = customFieldsList,
                        customFieldValues = customFieldValues
                    )
                }
            }
        }
    }
}

// Utility extension helper to run inline blocks safely
inline fun trialDate(block: () -> Unit) {
    try { block() } catch (e: Exception) {}
}

@Composable
fun ContactTab(
    name: String, onNameChange: (String) -> Unit,
    company: String, onCompanyChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit,
    website: String, onWebsiteChange: (String) -> Unit,
    linkedin: String, onLinkedinChange: (String) -> Unit,
    address: String, onAddressChange: (String) -> Unit,
    annualRevenue: String, onAnnualRevenueChange: (String) -> Unit,
    industry: String, onIndustryChange: (String) -> Unit,
    source: String, onSourceChange: (String) -> Unit,
    owner: String, onOwnerChange: (String) -> Unit,
    teamMembers: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Contact Client Name* (Required)") },
            modifier = Modifier.fillMaxWidth().testTag("deal_form_name_field"),
            singleLine = true
        )

        OutlinedTextField(
            value = company,
            onValueChange = onCompanyChange,
            label = { Text("Enterprise Company Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Corporate Designation / Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = website,
            onValueChange = onWebsiteChange,
            label = { Text("Corporate Website") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = linkedin,
            onValueChange = onLinkedinChange,
            label = { Text("LinkedIn Profile URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = annualRevenue,
            onValueChange = onAnnualRevenueChange,
            label = { Text("Annual Organization Revenue Budget") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Industry Spinner / Dropdown selection
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Tech", "Finance", "Logistics", "Retail").forEach { ind ->
                FilterChip(
                    selected = industry == ind,
                    onClick = { onIndustryChange(ind) },
                    label = { Text(ind, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Physical Headquarter Address") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )

        // Assignee selection
        Column {
            Text("Assign Pipeline Owner", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                teamMembers.take(4).forEach { member ->
                    FilterChip(
                        selected = owner == member,
                        onClick = { onOwnerChange(member) },
                        label = { Text(member, fontSize = 10.sp) }
                    )
                }
            }
        }
    }
}

@Composable
fun DealTab(
    stage: Stage, onStageChange: (Stage) -> Unit,
    priority: Priority, onPriorityChange: (Priority) -> Unit,
    currency: String, onCurrencyChange: (String) -> Unit,
    dealValue: String, onDealValueChange: (String) -> Unit,
    nextFollowUp: String, onNextFollowUpChange: (String) -> Unit,
    closingDate: String, onClosingDateChange: (String) -> Unit,
    rating: Int, onRatingChange: (Int) -> Unit,
    isDecisionMaker: Boolean, onDecisionMakerChange: (Boolean) -> Unit,
    isBudgetConfirmed: Boolean, onBudgetConfirmedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stage Spinner / selector chips
        Column {
            Text("Deal Pipeline Stage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Stage.values().take(3).forEach { st ->
                    FilterChip(
                        selected = stage == st,
                        onClick = { onStageChange(st) },
                        label = { Text(st.name, fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Stage.values().drop(3).forEach { st ->
                    FilterChip(
                        selected = stage == st,
                        onClick = { onStageChange(st) },
                        label = { Text(st.name, fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Priority chips options
        Column {
            Text("Deal Priority", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.values().forEach { pr ->
                    FilterChip(
                        selected = priority == pr,
                        onClick = { onPriorityChange(pr) },
                        label = { Text(pr.name, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Value Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Currency Selector Dropdown
            Box(modifier = Modifier.width(80.dp)) {
                OutlinedTextField(
                    value = currency,
                    onValueChange = onCurrencyChange,
                    label = { Text("Curr") }
                )
            }
            OutlinedTextField(
                value = dealValue,
                onValueChange = onDealValueChange,
                label = { Text("Deal Pipeline Value Amount") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        // Follow Up Dates Fields
        OutlinedTextField(
            value = nextFollowUp,
            onValueChange = onNextFollowUpChange,
            label = { Text("Next Follow-Up Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = closingDate,
            onValueChange = onClosingDateChange,
            label = { Text("Estimated Closing Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Star Rating (1-5)
        Column {
            Text("Star Opportunity Rating", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { star ->
                    IconButton(onClick = { onRatingChange(star) }) {
                        Icon(
                            imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Rating $star",
                            tint = if (star <= rating) Color.Yellow else Color.Gray
                        )
                    }
                }
            }
        }

        // Toggles decision maker/budget
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Direct Decision Maker Engaged")
            Switch(checked = isDecisionMaker, onCheckedChange = onDecisionMakerChange)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Corporate Budget Allocation Confirmed")
            Switch(checked = isBudgetConfirmed, onCheckedChange = onBudgetConfirmedChange)
        }
    }
}

@Composable
fun IntelTab(
    competitors: String, onCompetitorsChange: (String) -> Unit,
    painPoints: String, onPainPointsChange: (String) -> Unit,
    notes: String, onNotesChange: (String) -> Unit,
    customFieldsList: List<CustomField>,
    customFieldValues: MutableMap<String, String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = competitors,
            onValueChange = onCompetitorsChange,
            label = { Text("Key Competitors details") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = painPoints,
            onValueChange = onPainPointsChange,
            label = { Text("Client Pain Points & Challenges") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4
        )

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Shared Meeting Discussion Notes") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5
        )

        // Dynamically render custom fields configured inside Settings!
        if (customFieldsList.isNotEmpty()) {
            Text("Dynamic Configuration Fields", fontWeight = FontWeight.Bold)
            customFieldsList.forEach { field ->
                var fVal by remember { mutableStateOf(customFieldValues[field.label] ?: "") }
                OutlinedTextField(
                    value = fVal,
                    onValueChange = {
                        fVal = it
                        customFieldValues[field.label] = it
                    },
                    label = { Text(field.label) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
