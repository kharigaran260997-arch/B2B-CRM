package com.example.domain

import com.example.domain.model.Activity
import com.example.domain.model.ActivityType
import com.example.domain.model.Deal
import com.example.domain.model.DealStatus
import com.example.domain.model.EnrichedDeal
import com.example.domain.model.PipelineMetrics
import com.example.domain.model.Stage
import com.example.domain.model.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object AnalyticsEngine {

    val STAGE_PROBABILITY = mapOf(
        Stage.LEAD to 0.10,
        Stage.CONTACTED to 0.20,
        Stage.DEMO to 0.40,
        Stage.PROPOSAL to 0.70,
        Stage.WON to 1.00,
        Stage.LOST to 0.00
    )

    val ACTIVITY_WEIGHT = mapOf(
        ActivityType.CALL to 2,
        ActivityType.EMAIL to 1,
        ActivityType.MEETING to 4,
        ActivityType.NOTES to 0,
        ActivityType.WHATSAPP to 1,
        ActivityType.DEMO to 3
    )

    fun enrichDeal(deal: Deal, activities: List<Activity>, tasks: List<Task>): EnrichedDeal {
        val prob = STAGE_PROBABILITY[deal.stage] ?: 0.0
        val weighted = deal.dealValue * prob

        val lastActivityDateStr = activities.maxByOrNull { it.date }?.date ?: deal.createdAt
        val daysStale = try {
            val lastActTime = LocalDateTime.parse(lastActivityDateStr)
            ChronoUnit.DAYS.between(lastActTime, LocalDateTime.now())
        } catch (e: Exception) {
            0L
        }

        val hasActiveTask = tasks.any {
            !it.isDone && !it.dueDate.isNullOrBlank() && try {
                LocalDate.parse(it.dueDate).isAfter(LocalDate.now().minusDays(1))
            } catch (e: Exception) {
                true
            }
        }

        val momentum = activities.sumOf { ACTIVITY_WEIGHT[it.type] ?: 0 }
        val priorityScore = weighted * (1.0 / (daysStale + 1.0))

        val status = when {
            daysStale > 30 -> DealStatus.CRITICAL
            daysStale > 14 -> DealStatus.STALE
            !hasActiveTask && deal.stage != Stage.WON && deal.stage != Stage.LOST -> DealStatus.ACTIONLESS
            else -> DealStatus.HEALTHY
        }

        return EnrichedDeal(
            deal = deal,
            weighted = weighted,
            daysStale = daysStale,
            hasActiveTask = hasActiveTask,
            momentum = momentum,
            priorityScore = priorityScore,
            status = status
        )
    }

    fun computePipelineMetrics(deals: List<EnrichedDeal>, target: Double): PipelineMetrics {
        val weightedTotal = deals.sumOf { it.weighted }
        val rawTotal = deals.sumOf { it.deal.dealValue }
        val realization = if (rawTotal > 0) (weightedTotal / rawTotal) * 100.0 else 0.0
        val gap = target - weightedTotal

        val wonDeals = deals.filter { it.deal.stage == Stage.WON }
        val closedDeals = deals.filter { it.deal.stage == Stage.WON || it.deal.stage == Stage.LOST }
        val winRate = if (closedDeals.isNotEmpty()) {
            (wonDeals.size.toDouble() / closedDeals.size.toDouble()) * 100.0
        } else {
            0.0
        }

        val staleCount = deals.count { it.status == DealStatus.STALE }
        val criticalCount = deals.count { it.status == DealStatus.CRITICAL }
        val actionableCount = deals.count { it.status == DealStatus.HEALTHY }

        return PipelineMetrics(
            weightedTotal = weightedTotal,
            rawTotal = rawTotal,
            realization = realization,
            gap = gap,
            winRate = winRate,
            staleCount = staleCount,
            criticalCount = criticalCount,
            actionableCount = actionableCount
        )
    }
}
