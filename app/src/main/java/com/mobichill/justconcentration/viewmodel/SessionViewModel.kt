package com.mobichill.justconcentration.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.mobichill.justconcentration.base.application.MyApp
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.constants.Constants.OTHERS.DATE_FORMATTER
import com.mobichill.justconcentration.constants.TimeRangeOption
import java.time.DayOfWeek
import java.time.LocalDate

class SessionViewModel : ViewModel() {
    private val TAG = javaClass.simpleName
    private val sessionRepository = MyApp.instance.concentrateSessionRepository

    val _selectedTimeRange = MutableLiveData<TimeRangeOption>(TimeRangeOption.TODAY)
//    val selectedTimeRange: LiveData<TimeRangeOption> = _selectedTimeRange

    val allSessions: LiveData<List<ConcentrateSessionModel>> =
        sessionRepository.getAllSessions().asLiveData()

    private val _customStart = MutableLiveData<LocalDate?>()
    private val _customEnd = MutableLiveData<LocalDate?>()

    val completedSessions: LiveData<List<ConcentrateSessionModel>> = allSessions.map { sessions ->
        sessions.filter { it.wasCompleted }
    }
    val sessionCount: LiveData<Int> = completedSessions.map { sessions ->
        sessions.count()
    }

    // Stats based on completed sessions within the selected time range
    val sessionStats: LiveData<List<ConcentrateSessionModel>> =
        MediatorLiveData<List<ConcentrateSessionModel>>().apply {
            fun update() {
                val sessions = allSessions.value.orEmpty()
                val range = _selectedTimeRange.value ?: TimeRangeOption.TODAY
                val start = _customStart.value
                val end = _customEnd.value

                val newValue = filterSessionsByDate(sessions, range, start, end)

                // Check if value actually changed before emitting
                if (value != newValue) {
                    value = newValue
                }
            }
            addSource(allSessions) { update() }
            addSource(_selectedTimeRange) { update() }
            addSource(_customStart) { update() }
            addSource(_customEnd) { update() }
        }

    // Load session stats based on the selected time range
    private fun filterSessionsByDate(
        sessions: List<ConcentrateSessionModel>,
        range: TimeRangeOption,
        start: LocalDate?,
        end: LocalDate?
    ): List<ConcentrateSessionModel> {
        val (from, to) = when (range) {
            TimeRangeOption.TODAY -> {
                val today = LocalDate.now()
                today to today
            }

            TimeRangeOption.THIS_WEEK -> {
                val today = LocalDate.now()
                today.with(DayOfWeek.MONDAY) to today
            }

            TimeRangeOption.THIS_MONTH -> {
                val today = LocalDate.now()
                today.withDayOfMonth(1) to today
            }

            TimeRangeOption.CUSTOM ->
                if (start != null && end != null) start to end
                else null to null

            TimeRangeOption.ALL_TIME -> null to null
        }
        if (from == null && to == null && range != TimeRangeOption.ALL_TIME) {
            // Handle invalid CUSTOM range case
            Log.w(TAG, "Custom date range selected but start/end dates are null.")
            return emptyList()
        }
        if (range == TimeRangeOption.ALL_TIME) {
            return sessions // No date filtering needed
        }

        return sessions.filter {
            try {
                val sessionDate = LocalDate.parse(it.date, DATE_FORMATTER)
                val afterStart = from?.let { sessionDate >= it } != false
                val beforeEnd = to?.let { sessionDate <= it } != false
                afterStart && beforeEnd
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing date: ${it.date}", e)
                false
            }
        }
    }

    fun onCustomDateRangeSelected(startDate: LocalDate, endDate: LocalDate) {
        _customStart.value = startDate
        _customEnd.value = endDate
        _selectedTimeRange.value = TimeRangeOption.CUSTOM
    }

    val currentStreak: LiveData<Int> = completedSessions.map { sessions ->
        val completedDates = sessions
            .map { LocalDate.parse(it.date, DATE_FORMATTER) }
            .distinct()
            .sortedDescending()

        var streak = 0
        var dateToCheck = LocalDate.now()

        for (date in completedDates) {
            if (date == dateToCheck) {
                streak++
                dateToCheck = dateToCheck.minusDays(1)
            } else if (date.isBefore(dateToCheck)) {
                break
            }
        }

        streak
    }

    val getTotalFocusTime: LiveData<String> = allSessions.map { sessions ->
        val totalMinutes = sessions
            .sumOf { it.durationMinutes }
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        "${hours}h ${minutes}m"
    }

}