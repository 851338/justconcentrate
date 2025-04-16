package com.mobichill.justconcentration.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mobichill.justconcentration.helper.RoomHelper
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.repository.RoomRepository
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

class TaskViewModel(context: Context) : ViewModel() {
    private val TAG = javaClass.canonicalName
    private val firestoreRepo = FireStoreRepository()
    private val roomRepo = RoomRepository(RoomHelper.getInstance(context))
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val allTasks: LiveData<List<TaskModel>> = roomRepo.getAllActiveTasks().asLiveData()
    val allDeletedTasks: LiveData<List<TaskModel>> = roomRepo.getAllDeletedTasks().asLiveData()

    fun syncTasks() = viewModelScope.launch {
        val tasks = mutableListOf<TaskModel>()
        firestoreRepo.getTasksFromFireStore { taskList, error ->
            if (taskList.isNotEmpty())
                tasks.addAll(taskList)
            else Log.e(TAG, error?.message.toString())
        }
        roomRepo.saveTasksToRoom(tasks)
    }

    fun saveTaskToRoom(task: TaskModel) = viewModelScope.launch {
        roomRepo.saveTaskToRoom(task)
    }

    fun updateTaskToRoom(task: TaskModel) = viewModelScope.launch {
        roomRepo.updateTaskToRoom(task)
    }

    fun saveTaskToFireStore(task: TaskModel, onComplete: (Boolean, Exception?) -> Unit) =
        viewModelScope.launch {
            firestoreRepo.saveTaskToFireStore(task, onComplete)
        }

    fun updateTaskToFireStore(task: TaskModel, onComplete: (Boolean, Exception?) -> Unit) =
        viewModelScope.launch {
            firestoreRepo.updateTaskToFireStore(task, onComplete)
        }

    fun removeOrRestoreTaskWithFireStore(
        task: TaskModel,
        isRemove: Boolean,
        onComplete: (Boolean, Exception?) -> Unit
    ) {
        viewModelScope.launch {
            firestoreRepo.removeOrRestoreTask(task, onComplete, isRemove)
        }
    }

    fun removeOrRestoreTaskWithRoom(task: TaskModel, isRemove: Boolean) {
        viewModelScope.launch {
            roomRepo.removeOrRestoreTask(task, isRemove)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<List<TaskModel>> = searchQuery
        .debounce(300) //Don’t emit until the user stops typing for 300ms
        .flatMapLatest { query ->
            roomRepo.searchTasks(query)
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