package com.mobichill.justconcentration.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mobichill.justconcentration.application.MyApp
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.repository.FireStoreRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel : ViewModel() {
    private val TAG = javaClass.simpleName
    private val firestoreRepo = FireStoreRepository()
    private val taskRepository = MyApp.instance.taskRepository
    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allTasks: LiveData<List<TaskModel>> = taskRepository.getAllActiveTasks().asLiveData()

    fun syncTasks() = viewModelScope.launch {
        val tasks = mutableListOf<TaskModel>()
        firestoreRepo.getTasksFromFireStore { taskList, error ->
            if (taskList.isNotEmpty())
                tasks.addAll(taskList)
            else Log.e(TAG, error?.message.toString())
        }
        taskRepository.syncTasksToRoom(tasks)
    }

    fun saveTaskToRoom(task: TaskModel) = viewModelScope.launch {
        taskRepository.saveTaskToRoom(task)
    }

    fun updateTaskToRoom(task: TaskModel) = viewModelScope.launch {
        taskRepository.updateTaskToRoom(task)
    }

    fun saveTaskToFireStore(task: TaskModel, onComplete: (Boolean, Exception?) -> Unit) =
        viewModelScope.launch {
            firestoreRepo.saveTaskToFireStore(task, onComplete)
        }

    fun updateTaskToFireStore(task: TaskModel, onComplete: (Boolean, Exception?) -> Unit) =
        viewModelScope.launch {
            firestoreRepo.updateTaskToFireStore(task, onComplete)
        }

    fun deleteTaskFromFireStore(task: TaskModel) {
        viewModelScope.launch {
            firestoreRepo.deleteTaskFromFireStore(task)
        }
    }

    fun deleteTaskFromRoom(task: TaskModel) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<List<TaskModel>> = searchQuery
        .debounce(300) //Don’t emit until the user stops typing for 300ms
        .flatMapLatest { query ->
            taskRepository.searchTasks(query)
        } //If a new value comes in, drop what you were doing, and only care about the latest one
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        ) //Start collecting the flow when there's at least 1 active subscriber (like UI collecting it)
    //Wait 5000 ms (5 seconds) after the last subscriber disappears before stopping

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}