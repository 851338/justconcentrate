package com.mobichill.justconcentration.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.repository.TaskRepository
import com.mobichill.justconcentration.viewmodel.TaskViewModel

class TaskViewModelFactory(
    private val fireStoreRepository: FireStoreRepository,
    private val taskRepository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(fireStoreRepository, taskRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}