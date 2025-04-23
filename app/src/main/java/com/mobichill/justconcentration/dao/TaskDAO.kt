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

    @Update
    suspend fun updateTask(task: TaskModel)

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL")
    fun getAllActiveTasks(): Flow<List<TaskModel>>

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<TaskModel>

    @Query("SELECT * FROM tasks WHERE requestCode = :requestCode") //requestCode is unique
    fun getTaskByRequestCode(requestCode: Int): Flow<TaskModel>

    @Query("SELECT * FROM tasks WHERE taskText LIKE '%' || :query || '%' AND deletedAt IS NULL")
    fun searchTasks(query: String): Flow<List<TaskModel>>

    @Query("SELECT * FROM tasks WHERE isSynced = 0 AND deletedAt IS NULL")
    suspend fun getUnsyncedActiveTasks(): List<TaskModel>

    @Query("SELECT * FROM tasks WHERE isSynced = 0 AND deletedAt > 0")
    suspend fun getUnsyncedDeletedTasks(): List<TaskModel>
}