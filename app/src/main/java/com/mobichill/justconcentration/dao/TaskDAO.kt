package com.mobichill.justconcentration.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mobichill.justconcentration.model.TaskModel
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskModel)

    @Update
    suspend fun updateTask(task: TaskModel)

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL")
    fun getAllActiveTasks(): Flow<List<TaskModel>>

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskPermanentlyById(id: String)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteTasksPermanentlyByIds(ids: List<String>)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<TaskModel?>

    @Query("SELECT * FROM tasks WHERE requestCode = :requestCode") //requestCode is unique
    fun getTaskByRequestCode(requestCode: Int): Flow<TaskModel>

    @Query("SELECT * FROM tasks WHERE taskText LIKE '%' || :query || '%' AND deletedAt IS NULL")
    fun searchTasks(query: String): Flow<List<TaskModel>>

    @Query("SELECT * FROM tasks WHERE isSynced = 0 AND deletedAt IS NULL")
    suspend fun getUnsyncedActiveTasks(): List<TaskModel>

    @Query("SELECT * FROM tasks WHERE isSynced = 0 AND deletedAt > 0")
    suspend fun getUnsyncedDeletedTasks(): List<TaskModel>

    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 1 AND createdAt BETWEEN :startOfDay AND :endOfDay")
    suspend fun getTodayCompletedTaskCount(startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 1")
    suspend fun getCompletedTaskCount(): Int

    @Query("SELECT DISTINCT DATE(completedAt / 1000, 'unixepoch', 'localtime') FROM tasks WHERE completed = 1")
    suspend fun getCompletedTaskDates(): List<String> // format: YYYY-MM-DD

    @Query("UPDATE tasks SET isSynced = 1, needsUpload = 0, serverLastUpdatedMillis = :serverTimestampMillis WHERE id = :id")
    suspend fun markTaskAsSyncedById(id: String, serverTimestampMillis: Long)

    @Query("UPDATE tasks SET isSynced = 1, needsUpload = 0 WHERE id IN (:ids)")
    suspend fun markTasksAsSyncedAfterUpload(ids: List<String>)
}