package com.mobichill.justconcentration.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mobichill.justconcentration.helper.RoomHelper
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.repository.RoomRepository
import kotlinx.coroutines.launch

class TaskViewModel(context: Context) : ViewModel() {
    private val firestoreRepo = FireStoreRepository()
    private val roomRepo = RoomRepository(RoomHelper.getInstance(context))

    val allTasks: LiveData<List<TaskModel>> = roomRepo.getAllTasks().asLiveData()

    fun syncTasks() = viewModelScope.launch {
        val tasks = firestoreRepo.getTasksFromFireStore()
        roomRepo.saveTasksToRoom(tasks)
    }

    fun saveTaskToRoom(task: TaskModel) = viewModelScope.launch {
        roomRepo.saveTaskToRoom(task)
    }

    fun updateTaskToRoom(task: TaskModel) = viewModelScope.launch {
        roomRepo.updateTaskToRoom(task)
    }
}