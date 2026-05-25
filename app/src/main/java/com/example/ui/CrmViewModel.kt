package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.remote.ContactIntelResult
import com.example.data.remote.DealCoachResult
import com.example.data.remote.GeminiService
import com.example.data.repository.DealRepository
import com.example.domain.AnalyticsEngine
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

sealed interface AuthState {
    object Unauthenticated : AuthState
    data class Authenticated(val email: String, val role: String) : AuthState
}

class CrmViewModel(private val repository: DealRepository) : ViewModel() {

    private val TAG = "CrmViewModel"

    // Authentication & Roles
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Configurable Target (In Lakhs, e.g. 50L = 5,000,000)
    private val _revenueTarget = MutableStateFlow(5000000.0)
    val revenueTarget: StateFlow<Double> = _revenueTarget.asStateFlow()

    // Configurable Team Members list
    private val _teamMembers = MutableStateFlow(listOf("Kari G.", "Rohan S.", "Anita D.", "David L."))
    val teamMembers: StateFlow<List<String>> = _teamMembers.asStateFlow()

    // Active Filters
    val searchQuery = MutableStateFlow("")
    val selectedStageFilter = MutableStateFlow<Stage?>(null)
    val selectedStatusFilter = MutableStateFlow<DealStatus?>(null)
    val selectedPriorityFilter = MutableStateFlow<Priority?>(null)
    val selectedOwnerFilter = MutableStateFlow<String?>(null)
    val sortBy = MutableStateFlow("Priority Score") // "Priority Score", "Deal Value", "Staleness", "Close Date"

    // Live Streams from Database
    val enrichedDeals: StateFlow<List<EnrichedDeal>> = repository.enrichedDeals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customFields: StateFlow<List<CustomField>> = repository.allCustomFields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined Pipeline Metrics State
    val pipelineMetrics: StateFlow<PipelineMetrics> = combine(enrichedDeals, revenueTarget) { deals, target ->
        AnalyticsEngine.computePipelineMetrics(deals, target)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PipelineMetrics(0.0, 0.0, 0.0, 5000000.0, 0.0, 0, 0, 0))

    // Helper data class for state filters
    data class SearchFilters(
        val stage: Stage?,
        val status: DealStatus?,
        val priority: Priority?,
        val owner: String?,
        val sort: String
    )

    // Intermediate combining filters flow (fits inside kotlin's standard arity parameters list)
    private val filtersFlow = combine(
        selectedStageFilter,
        selectedStatusFilter,
        selectedPriorityFilter,
        selectedOwnerFilter,
        sortBy
    ) { stage, status, priority, owner, sort ->
        SearchFilters(stage, status, priority, owner, sort)
    }

    // Filtered/Sorted list for the main Pipeline view
    val filteredDeals: StateFlow<List<EnrichedDeal>> = combine(
        enrichedDeals,
        searchQuery,
        filtersFlow
    ) { deals, query, filters ->
        var list = deals
        if (query.isNotEmpty()) {
            list = list.filter {
                it.deal.name.contains(query, ignoreCase = true) ||
                        it.deal.company.contains(query, ignoreCase = true) ||
                        it.deal.email.contains(query, ignoreCase = true)
            }
        }
        if (filters.stage != null) {
            list = list.filter { it.deal.stage == filters.stage }
        }
        if (filters.status != null) {
            list = list.filter { it.status == filters.status }
        }
        if (filters.priority != null) {
            list = list.filter { it.deal.priority == filters.priority }
        }
        if (filters.owner != null) {
            list = list.filter { it.deal.owner == filters.owner }
        }

        when (filters.sort) {
            "Deal Value" -> list.sortedByDescending { it.deal.dealValue }
            "Staleness" -> list.sortedByDescending { it.daysStale }
            "Close Date" -> list.sortedBy { it.deal.closingDate ?: "9999-12-31" }
            else -> list.sortedByDescending { it.priorityScore } // Priority Score
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Coaching States (Keyed by Deal ID)
    private val _aiCoachingResults = MutableStateFlow<Map<String, DealCoachResult>>(emptyMap())
    val aiCoachingResults: StateFlow<Map<String, DealCoachResult>> = _aiCoachingResults.asStateFlow()

    private val _aiCoachingLoading = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val aiCoachingLoading: StateFlow<Map<String, Boolean>> = _aiCoachingLoading.asStateFlow()

    // AI Contact Intelligence States (Keyed by Contact Name)
    private val _contactIntelResults = MutableStateFlow<Map<String, ContactIntelResult>>(emptyMap())
    val contactIntelResults: StateFlow<Map<String, ContactIntelResult>> = _contactIntelResults.asStateFlow()

    private val _contactIntelLoading = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val contactIntelLoading: StateFlow<Map<String, Boolean>> = _contactIntelLoading.asStateFlow()

    // CSV Importing workflow states
    var csvParsedRows = MutableStateFlow<List<List<String>>>(emptyList())
    var csvHeaders = MutableStateFlow<List<String>>(emptyList())
    // Column index matching: "Name" -> 0, "Company" -> 1, "Value" -> 2 etc.
    val columnMappings = MutableStateFlow<Map<String, Int>>(emptyMap())
    var csvImportSuccessCount = MutableStateFlow<Int?>(null)

    init {
        // Hydrate demo/seed data if Room database is empty on first boot.
        seedDatabaseIfEmpty()
    }

    // Login Action
    fun login(email: String, role: String) {
        if (email.isNotBlank()) {
            _authState.value = AuthState.Authenticated(email, role)
        }
    }

    // Logout Action
    fun logout() {
        _authState.value = AuthState.Unauthenticated
    }

    // Settings actions
    fun setRevenueTarget(target: Double) {
        _revenueTarget.value = target
    }

    fun addTeamMember(name: String) {
        if (name.isNotBlank() && !_teamMembers.value.contains(name)) {
            _teamMembers.value = _teamMembers.value + name
        }
    }

    fun removeTeamMember(name: String) {
        _teamMembers.value = _teamMembers.value - name
    }

    // Local DB Operations
    fun saveDeal(deal: Deal) {
        viewModelScope.launch {
            val updated = deal.copy(updatedAt = LocalDateTime.now().toString())
            repository.insertDeal(updated)
        }
    }

    fun deleteDeal(dealId: String) {
        viewModelScope.launch {
            repository.deleteDeal(dealId)
        }
    }

    fun updateDealStageInline(dealId: String, newStage: Stage) {
        viewModelScope.launch {
            repository.updateStage(dealId, newStage, LocalDateTime.now().toString())
            // Push active log activity
            repository.insertActivity(
                Activity(
                    dealId = dealId,
                    type = ActivityType.MEETING,
                    text = "Moved deal to stage: ${newStage.name}",
                    date = LocalDateTime.now().toString()
                )
            )
        }
    }

    fun logActivity(activity: Activity) {
        viewModelScope.launch {
            val updated = activity.copy(date = LocalDateTime.now().toString())
            repository.insertActivity(updated)
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            repository.insertTask(task)
        }
    }

    fun toggleTask(taskId: String, isDone: Boolean) {
        viewModelScope.launch {
            repository.updateTaskStatus(taskId, isDone)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun addComment(comment: Comment) {
        viewModelScope.launch {
            val updated = comment.copy(createdAt = LocalDateTime.now().toString())
            repository.insertComment(updated)
        }
    }

    fun createCustomField(label: String, type: FieldType, options: List<String>) {
        viewModelScope.launch {
            repository.insertCustomField(
                CustomField(
                    label = label,
                    type = type,
                    options = options,
                    sortOrder = (repository.allCustomFields.firstOrNull()?.size ?: 0) + 1
                )
            )
        }
    }

    fun deleteCustomField(labelId: String) {
        viewModelScope.launch {
            repository.deleteCustomField(labelId)
        }
    }

    // Streams for individual deal interactions
    fun getEnrichedDeal(id: String): Flow<EnrichedDeal?> {
        return repository.getEnrichedDealById(id)
    }

    fun getActivities(dealId: String): Flow<List<Activity>> {
        return repository.getActivities(dealId)
    }

    fun getTasks(dealId: String): Flow<List<Task>> {
        return repository.getTasks(dealId)
    }

    fun getComments(dealId: String): Flow<List<Comment>> {
        return repository.getComments(dealId)
    }

    // AI Coaching Engine
    fun requestDealCoachAnalysis(enriched: EnrichedDeal, activities: List<Activity>, tasks: List<Task>) {
        val dealId = enriched.deal.id
        viewModelScope.launch {
            _aiCoachingLoading.value = _aiCoachingLoading.value + (dealId to true)
            try {
                val result = GeminiService.analyzeDeal(enriched, activities, tasks)
                _aiCoachingResults.value = _aiCoachingResults.value + (dealId to result)
            } catch (e: Exception) {
                Log.e(TAG, "Failed AI coached deal analysis", e)
            } finally {
                _aiCoachingLoading.value = _aiCoachingLoading.value + (dealId to false)
            }
        }
    }

    // AI Contact Intelligence Enrichment
    fun requestContactEnrichment(name: String, company: String) {
        viewModelScope.launch {
            _contactIntelLoading.value = _contactIntelLoading.value + (name to true)
            try {
                val result = GeminiService.enrichContact(name, company)
                _contactIntelResults.value = _contactIntelResults.value + (name to result)
            } catch (e: Exception) {
                Log.e(TAG, "Failed AI contact profiling", e)
            } finally {
                _contactIntelLoading.value = _contactIntelLoading.value + (name to false)
            }
        }
    }

    // CSV Parse file into grid for preview
    fun parseCSV(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val allLines = mutableListOf<List<String>>()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        // Handle simple csv splits
                        val tokens = line!!.split(",").map { it.replace("\"", "").trim() }
                        allLines.add(tokens)
                    }

                    if (allLines.isNotEmpty()) {
                        csvHeaders.value = allLines[0]
                        csvParsedRows.value = allLines.drop(1)
                        // Auto map by names
                        val mapping = mutableMapOf<String, Int>()
                        allLines[0].forEachIndexed { index, header ->
                            val lower = header.lowercase()
                            when {
                                lower.contains("name") -> mapping["Name"] = index
                                lower.contains("company") || lower.contains("account") -> mapping["Company"] = index
                                lower.contains("value") || lower.contains("amount") -> mapping["Deal Value"] = index
                                lower.contains("stage") || lower.contains("status") -> mapping["Stage"] = index
                                lower.contains("email") -> mapping["Email"] = index
                                lower.contains("phone") || lower.contains("mobile") -> mapping["Phone"] = index
                            }
                        }
                        columnMappings.value = mapping
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "CSV Parsing failed", e)
            }
        }
    }

    fun updateMapping(columnKey: String, headerIndex: Int) {
        columnMappings.value = columnMappings.value + (columnKey to headerIndex)
    }

    // Perform bulk insertion mapping CSV data elements
    fun importMappedCSV() {
        val rows = csvParsedRows.value
        val mappings = columnMappings.value
        if (rows.isEmpty()) return

        viewModelScope.launch {
            val loadedDeals = mutableListOf<Deal>()
            rows.forEach { row ->
                try {
                    val nameIdx = mappings["Name"] ?: -1
                    val name = if (nameIdx in row.indices) row[nameIdx] else "Imported Item"
                    val companyIdx = mappings["Company"] ?: -1
                    val company = if (companyIdx in row.indices) row[companyIdx] else "Unknown Inc."
                    val valIdx = mappings["Deal Value"] ?: -1
                    val valStr = if (valIdx in row.indices) row[valIdx].replace(Regex("[^\\d.]"), "") else "0.0"
                    val dealValue = valStr.toDoubleOrNull() ?: 0.0
                    val emailIdx = mappings["Email"] ?: -1
                    val email = if (emailIdx in row.indices) row[emailIdx] else ""
                    val phoneIdx = mappings["Phone"] ?: -1
                    val phone = if (phoneIdx in row.indices) row[phoneIdx] else ""
                    val stageIdx = mappings["Stage"] ?: -1
                    val stageStr = if (stageIdx in row.indices) row[stageIdx].uppercase() else "LEAD"
                    val stage = try { Stage.valueOf(stageStr) } catch(e: Exception) { Stage.LEAD }

                    if (name.isNotBlank()) {
                        loadedDeals.add(
                            Deal(
                                name = name,
                                company = company,
                                dealValue = dealValue,
                                email = email,
                                phone = phone,
                                stage = stage,
                                owner = _teamMembers.value.randomOrNull() ?: "Self",
                                createdAt = LocalDateTime.now().toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Row mapping failed", e)
                }
            }

            if (loadedDeals.isNotEmpty()) {
                repository.insertDeals(loadedDeals)
                csvImportSuccessCount.value = loadedDeals.size
                // Reset parsed data
                csvParsedRows.value = emptyList()
            }
        }
    }

    // Export completely
    fun generateExportCSVText(dealsList: List<EnrichedDeal>): String {
        val header = "Name,Company,Value,Stage,Email,Phone,Owner\n"
        val rows = dealsList.joinToString("\n") {
            val d = it.deal
            "\"${d.name}\",\"${d.company}\",${d.dealValue},\"${d.stage.name}\",\"${d.email}\",\"${d.phone}\",\"${d.owner}\""
        }
        return header + rows
    }

    private fun seedDatabaseIfEmpty() {
        viewModelScope.launch {
            if (repository.allDeals.firstOrNull()?.isEmpty() == true) {
                Log.d(TAG, "Empty Database. Seeding realistic sample corporate sales data.")
                val users = _teamMembers.value
                val sampleDeals = listOf(
                    Deal(
                        name = "Enterprise Cloud Migrate",
                        company = "Atlas Tech Corp",
                        dealValue = 1200000.0,
                        stage = Stage.PROPOSAL,
                        priority = Priority.HIGH,
                        email = "contact@atlas.com",
                        phone = "9823476123",
                        owner = users[0],
                        createdAt = LocalDateTime.now().minusDays(18).toString(),
                        notes = "Highly interested in multi-cloud deployment. Competitor offering 10% discount."
                    ),
                    Deal(
                        name = "Security Firewalls Suite",
                        company = "Apex Financial Group",
                        dealValue = 4500000.0,
                        stage = Stage.DEMO,
                        priority = Priority.HIGH,
                        email = "procure@apex.com",
                        phone = "8899776655",
                        owner = users[1],
                        createdAt = LocalDateTime.now().minusDays(22).toString(),
                        nextFollowUp = LocalDate.now().plusDays(1).toString(),
                        isDecisionMaker = true,
                        notes = "Critical security upgrade requested. CFO confirmed budget is available."
                    ),
                    Deal(
                        name = "AI Co-Pilot Integrator",
                        company = "Vertex Global Labs",
                        dealValue = 850000.0,
                        stage = Stage.LEAD,
                        priority = Priority.LOW,
                        email = "devops@vertex.io",
                        phone = "9011223344",
                        owner = users[2],
                        createdAt = LocalDateTime.now().minusDays(2).toString(),
                        isBudgetConfirmed = false,
                        notes = "Warm lead from technology conference. Needs deep custom integration."
                    ),
                    Deal(
                        name = "SaaS Analytics Dashboard",
                        company = "Nova Retailers",
                        dealValue = 1500000.0,
                        stage = Stage.WON,
                        priority = Priority.MEDIUM,
                        email = "contact@novaretail.in",
                        phone = "7760991122",
                        owner = users[3],
                        createdAt = LocalDateTime.now().minusDays(45).toString(),
                        isBudgetConfirmed = true,
                        isDecisionMaker = true,
                        notes = "Contract fully signed! Implementation underway."
                    ),
                    Deal(
                        name = "API Gateway Enterprise Licenses",
                        company = "Quantum Logistics",
                        dealValue = 2400000.0,
                        stage = Stage.CONTACTED,
                        priority = Priority.MEDIUM,
                        email = "infra@quantum-log.com",
                        phone = "6612457812",
                        owner = users[0],
                        createdAt = LocalDateTime.now().minusDays(15).toString(),
                        notes = "Initial discovery call completed. Looking to replace home-grown router nodes."
                    )
                )

                repository.insertDeals(sampleDeals)

                // Add activities for seeded deals to populate mock pipelines
                sampleDeals.forEach { deal ->
                    repository.insertActivity(
                        Activity(
                            dealId = deal.id,
                            type = ActivityType.CALL,
                            text = "Initial introductory discussion logged.",
                            date = LocalDateTime.now().minusDays(10).toString()
                        )
                    )
                    repository.insertActivity(
                        Activity(
                            dealId = deal.id,
                            type = ActivityType.MEETING,
                            text = "Platform architecture requirements review.",
                            date = LocalDateTime.now().minusDays(3).toString()
                        )
                    )

                    // Add demo tasks
                    repository.insertTask(
                        Task(
                            dealId = deal.id,
                            text = "Send customized Pricing Proposal.",
                            dueDate = LocalDate.now().plusDays(3).toString(),
                            priority = Priority.HIGH
                        )
                    )
                    repository.insertTask(
                        Task(
                            dealId = deal.id,
                            text = "Draft mutual NDAs and legal forms.",
                            dueDate = LocalDate.now().plusDays(8).toString(),
                            priority = Priority.MEDIUM
                        )
                    )
                }
            }
        }
    }
}

class CrmViewModelFactory(private val repository: DealRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CrmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CrmViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
