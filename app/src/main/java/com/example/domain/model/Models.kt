package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class Stage { LEAD, CONTACTED, DEMO, PROPOSAL, WON, LOST }
enum class Priority { HIGH, MEDIUM, LOW }
enum class ActivityType { CALL, EMAIL, MEETING, NOTES, WHATSAPP, DEMO }
enum class FieldType { TEXT, DROPDOWN, NUMBER, DATE, CHECKBOX }

@Entity(tableName = "deals")
data class Deal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val company: String,
    val email: String = "",
    val phone: String = "",
    val title: String = "",
    val dealValue: Double = 0.0,
    val currency: String = "₹",
    val stage: Stage = Stage.LEAD,
    val priority: Priority = Priority.MEDIUM,
    val industry: String = "",
    val source: String = "",
    val owner: String = "",
    val tags: List<String> = emptyList(),
    val isDecisionMaker: Boolean = false,
    val isBudgetConfirmed: Boolean = false,
    val competitors: String = "",
    val painPoints: String = "",
    val notes: String = "",
    val nextFollowUp: String? = null, // ISO formatting
    val closingDate: String? = null,
    val rating: Int = 3,
    val annualRevenue: String = "",
    val website: String = "",
    val linkedin: String = "",
    val address: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val customFields: Map<String, String> = emptyMap(),
    val isSynced: Boolean = false
)

@Entity(tableName = "activities")
data class Activity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dealId: String,
    val type: ActivityType,
    val text: String,
    val date: String = "", // ISO string or format
    val isDone: Boolean = false
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dealId: String,
    val text: String,
    val dueDate: String? = null,
    val isDone: Boolean = false,
    val priority: Priority = Priority.MEDIUM
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dealId: String,
    val author: String,
    val text: String,
    val createdAt: String = ""
)

@Entity(tableName = "custom_fields")
data class CustomField(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String,
    val type: FieldType,
    val options: List<String> = emptyList(),
    val isRequired: Boolean = false,
    val sortOrder: Int = 0
)

data class EnrichedDeal(
    val deal: Deal,
    val weighted: Double,
    val daysStale: Long,
    val hasActiveTask: Boolean,
    val momentum: Int,
    val priorityScore: Double,
    val status: DealStatus
)

enum class DealStatus { CRITICAL, STALE, ACTIONLESS, HEALTHY }

data class PipelineMetrics(
    val weightedTotal: Double,
    val rawTotal: Double,
    val realization: Double, // weighted / raw
    val gap: Double,         // target - weighted
    val winRate: Double,
    val staleCount: Int,
    val criticalCount: Int,
    val actionableCount: Int
)
