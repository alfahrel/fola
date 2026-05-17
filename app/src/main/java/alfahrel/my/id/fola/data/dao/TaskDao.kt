package alfahrel.my.id.fola.data.dao

import androidx.room.*
import alfahrel.my.id.fola.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDateMs ASC, createdAt DESC")
    fun getActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY createdAt DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDateMs ASC, createdAt DESC")
    fun getAllTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDateMs ASC, createdAt DESC")
    fun getAllActiveTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY createdAt DESC")
    fun getAllCompletedTasksSync(): List<Task>

    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 0
          AND (
            (dueDateMs >= :startMs AND dueDateMs < :endMs)
            OR (startDateMs >= :startMs AND startDateMs < :endMs)
            OR (dueDateMs IS NULL AND startDateMs IS NULL)
          )
        ORDER BY dueDateMs ASC, createdAt ASC
    """)
    fun getActiveTasksForDaySync(startMs: Long, endMs: Long): List<Task>

    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 1
          AND (
            (dueDateMs >= :startMs AND dueDateMs < :endMs)
            OR (startDateMs >= :startMs AND startDateMs < :endMs)
            OR (dueDateMs IS NULL AND startDateMs IS NULL)
          )
        ORDER BY createdAt ASC
    """)
    fun getCompletedTasksForDaySync(startMs: Long, endMs: Long): List<Task>

    @Query("""
        SELECT * FROM tasks
        WHERE (dueDateMs >= :startMs AND dueDateMs < :endMs)
           OR (startDateMs >= :startMs AND startDateMs < :endMs)
           OR (dueDateMs IS NULL AND startDateMs IS NULL)
        ORDER BY isCompleted ASC, createdAt ASC
    """)
    fun getTasksForDaySync(startMs: Long, endMs: Long): List<Task>

    @Query("""
        SELECT * FROM tasks
        WHERE (dueDateMs >= :startMs AND dueDateMs < :endMs)
           OR (startDateMs >= :startMs AND startDateMs < :endMs)
           OR (dueDateMs IS NULL AND startDateMs IS NULL)
        ORDER BY isCompleted ASC, createdAt ASC
    """)
    suspend fun getTasksForDay(startMs: Long, endMs: Long): List<Task>

    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 0
          AND (
            (dueDateMs >= :startMs AND dueDateMs < :endMs)
            OR (startDateMs >= :startMs AND startDateMs < :endMs)
            OR (dueDateMs IS NULL AND startDateMs IS NULL)
          )
        ORDER BY dueDateMs ASC, createdAt ASC
    """)
    suspend fun getActiveTasksForDay(startMs: Long, endMs: Long): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Update
    fun updateSync(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    fun setCompletedSync(id: Long, completed: Boolean)
}