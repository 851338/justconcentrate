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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskModel>)

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskModel>>

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: Int): Flow<TaskModel>

    @Query("SELECT * FROM tasks WHERE requestCode = :requestCode") //requestCode is unique
    fun getTaskByRequestCode(requestCode: Int): Flow<TaskModel>

    @Query("SELECT * FROM tasks WHERE alarmTimeMillis < :currentTime AND alarmTimeMillis != 0 AND completed = 0")
    fun getMissedTasks(currentTime: Long): List<TaskModel>
}