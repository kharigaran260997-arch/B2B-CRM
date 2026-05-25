package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.domain.AnalyticsEngine
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DealRepository(private val db: AppDatabase) {
    private val dealDao = db.dealDao()
    private val activityDao = db.activityDao()
    private val taskDao = db.taskDao()
    private val commentDao = db.commentDao()
    private val customFieldDao = db.customFieldDao()

    val allDeals: Flow<List<Deal>> = dealDao.getAllDeals()
    val allActivities: Flow<List<Activity>> = activityDao.getAllActivities()
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allCustomFields: Flow<List<CustomField>> = customFieldDao.getAllCustomFields()

    // Highly reactive enriched deals stream
    val enrichedDeals: Flow<List<EnrichedDeal>> = combine(
        allDeals,
        allActivities,
        allTasks
    ) { deals, activities, tasks ->
        deals.map { deal ->
            val dealActivities = activities.filter { it.dealId == deal.id }
            val dealTasks = tasks.filter { it.dealId == deal.id }
            AnalyticsEngine.enrichDeal(deal, dealActivities, dealTasks)
        }
    }

    fun getEnrichedDealById(dealId: String): Flow<EnrichedDeal?> {
        return combine(
            dealDao.getDealById(dealId),
            allActivities,
            allTasks
        ) { deal, activities, tasks ->
            deal?.let {
                val dealActivities = activities.filter { it.dealId == dealId }
                val dealTasks = tasks.filter { it.dealId == dealId }
                AnalyticsEngine.enrichDeal(deal, dealActivities, dealTasks)
            }
        }
    }

    // CRUD Ops for Deals
    suspend fun insertDeal(deal: Deal) = dealDao.insertDeal(deal)
    suspend fun insertDeals(deals: List<Deal>) = dealDao.insertDeals(deals)
    suspend fun deleteDeal(id: String) = dealDao.deleteDealById(id)
    suspend fun updateStage(id: String, stage: Stage, updatedAt: String) = dealDao.updateStage(id, stage, updatedAt)
    suspend fun deleteAllDeals() = dealDao.deleteAllDeals()

    // CRUD Ops for Activities
    fun getActivities(dealId: String): Flow<List<Activity>> = activityDao.getActivitiesForDeal(dealId)
    suspend fun insertActivity(activity: Activity) = activityDao.insertActivity(activity)
    suspend fun deleteActivity(id: String) = activityDao.deleteActivityById(id)

    // CRUD Ops for Tasks
    fun getTasks(dealId: String): Flow<List<Task>> = taskDao.getTasksForDeal(dealId)
    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    suspend fun deleteTask(id: String) = taskDao.deleteTaskById(id)
    suspend fun updateTaskStatus(id: String, isDone: Boolean) = taskDao.updateTaskStatus(id, isDone)

    // CRUD Ops for Comments
    fun getComments(dealId: String): Flow<List<Comment>> = commentDao.getCommentsForDeal(dealId)
    suspend fun insertComment(comment: Comment) = commentDao.insertComment(comment)

    // CRUD Ops for Custom Fields
    suspend fun insertCustomField(customField: CustomField) = customFieldDao.insertCustomField(customField)
    suspend fun deleteCustomField(id: String) = customFieldDao.deleteCustomField(id)
}
