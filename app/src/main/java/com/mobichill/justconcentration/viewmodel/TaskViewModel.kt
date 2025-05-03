package com.mobichill.justconcentration.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mobichill.justconcentration.manager.BadgeProgressManager
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.repository.TaskRepository
import com.mobichill.justconcentration.utils.ConvertUtils
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
import java.time.LocalDate
import java.time.LocalTime

class TaskViewModel(
    private val firestoreRepo: FireStoreRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {
    private val TAG = javaClass.simpleName
    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allTasks: LiveData<List<TaskModel>> = taskRepository.getAllActiveTasks().asLiveData()

    fun saveTaskToRoom(task: TaskModel) = viewModelScope.launch {
        taskRepository.saveTaskToRoom(task)
    }

    fun updateTaskToRoom(task: TaskModel) = viewModelScope.launch {
        taskRepository.updateTaskToRoom(task)
    }

    fun updateAfterTaskCompletion(completedAt: Long?) {
        try {
            viewModelScope.launch {
                BadgeProgressManager().updateAfterTaskCompletion(
                    totalTasks = taskRepository.getCompletedTaskCount(),
                    taskTime = LocalTime.now(),
                    completedDate = ConvertUtils.convertTimeMillisIntoLocalDate(completedAt!!),
                    currentDate = LocalDate.now(),
                    taskStreakDays = getCurrentStreak()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateAfterTaskCompletion: ", e)
        }
    }

    fun saveTaskToFireStore(task: TaskModel) =
        viewModelScope.launch {
            firestoreRepo.saveTaskToFireStore(task)
        }

    fun updateTaskToFireStore(task: TaskModel) =
        viewModelScope.launch {
            firestoreRepo.updateTaskToFireStore(task)
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

    suspend fun getCurrentStreak(): Int {
        val completedDates = taskRepository.getCompletedTaskDates()
        return calculateStreak(completedDates)
    }

    private fun calculateStreak(completedDates: List<String>): Int {
        val completedSet = completedDates.toSet()
        var streak = 0
        var currentDate = LocalDate.now()

        // Loop stops when hit a date that is not in completedSet
        while (completedSet.contains(currentDate.toString())) {
            streak++
            currentDate = currentDate.minusDays(1)
        }

        return streak
    }
}