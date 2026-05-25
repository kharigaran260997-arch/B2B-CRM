package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.local.converters.Converters
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DealDao {
    @Query("SELECT * FROM deals ORDER BY name ASC")
    fun getAllDeals(): Flow<List<Deal>>

    @Query("SELECT * FROM deals WHERE id = :id")
    fun getDealById(id: String): Flow<Deal?>

    @Query("SELECT * FROM deals WHERE id = :id")
    suspend fun getDealByIdSync(id: String): Deal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeal(deal: Deal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeals(deals: List<Deal>)

    @Query("DELETE FROM deals WHERE id = :id")
    suspend fun deleteDealById(id: String)

    @Query("DELETE FROM deals")
    suspend fun deleteAllDeals()

    @Query("UPDATE deals SET stage = :stage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStage(id: String, stage: Stage, updatedAt: String)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE dealId = :dealId ORDER BY date DESC")
    fun getActivitiesForDeal(dealId: String): Flow<List<Activity>>

    @Query("SELECT * FROM activities ORDER BY date DESC")
    fun getAllActivities(): Flow<List<Activity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: Activity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE dealId = :dealId ORDER BY dueDate ASC")
    fun getTasksForDeal(dealId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("UPDATE tasks SET isDone = :isDone WHERE id = :id")
    suspend fun updateTaskStatus(id: String, isDone: Boolean)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE dealId = :dealId ORDER BY createdAt ASC")
    fun getCommentsForDeal(dealId: String): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)
}

@Dao
interface CustomFieldDao {
    @Query("SELECT * FROM custom_fields ORDER BY sortOrder ASC")
    fun getAllCustomFields(): Flow<List<CustomField>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomField(field: CustomField)

    @Query("DELETE FROM custom_fields WHERE id = :id")
    suspend fun deleteCustomField(id: String)
}

@Database(
    entities = [Deal::class, Activity::class, Task::class, Comment::class, CustomField::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dealDao(): DealDao
    abstract fun activityDao(): ActivityDao
    abstract fun taskDao(): TaskDao
    abstract fun commentDao(): CommentDao
    abstract fun customFieldDao(): CustomFieldDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "revenue_command_center_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
