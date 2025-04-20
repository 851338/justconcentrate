package com.mobichill.justconcentration.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mobichill.justconcentration.viewmodel.TaskViewModel

class TaskViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}