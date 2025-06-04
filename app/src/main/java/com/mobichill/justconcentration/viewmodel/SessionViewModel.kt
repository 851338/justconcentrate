package com.mobichill.justconcentration.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.mobichill.justconcentration.constants.Constants.OTHERS.DATE_FORMATTER
import com.mobichill.justconcentration.constants.TimeRangeOption
import com.mobichill.justconcentration.helper.StatsCalculateHelper
import com.mobichill.justconcentration.manager.ProBadgeManager
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.repository.ConcentrateSessionRepository
import com.mobichill.justconcentration.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionRepository: ConcentrateSessionRepository,
    private val statsCalculator: StatsCalculateHelper,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    companion object {
        private val TAG = SessionViewModel::class.java.simpleName
    }

    private val _mediatorActivationTest = MutableLiveData<String>()
    val mediatorActivationTest: LiveData<String> = _mediatorActivationTest

    private val _selectedTimeRange = MutableLiveData<TimeRangeOption>(TimeRangeOption.TODAY)
    val selectedTimeRange: LiveData<TimeRangeOption> = _selectedTimeRange // Expose non-mutable

    private val _customStart = MutableLiveData<LocalDate?>()
    val customStart: LiveData<LocalDate?> = _customStart // Expose non-mutable

    private val _customEnd = MutableLiveData<LocalDate?>()
    val customEnd: LiveData<LocalDate?> = _customEnd // Expose non-mutable

    val allSessions: LiveData<List<ConcentrateSessionModel>> =
        sessionRepository.getAllSessions().asLiveData()

    val completedSessions: LiveData<List<ConcentrateSessionModel>> = allSessions.map { sessions ->
        sessions.filter { it.wasCompleted }
    }
    val sessionCount: LiveData<Int> = completedSessions.map { sessions ->
        sessions.count()
    }

    val currentStreak: LiveData<Int> = completedSessions.map { sessions ->
        calculateStreak(sessions)
    }

    private val _isUserPro = MutableLiveData<Boolean>()
    val isUserPro: LiveData<Boolean> = _isUserPro

    private val _barChartData = MediatorLiveData<List<ConcentrateSessionModel>>()
    val barChartData: LiveData<List<ConcentrateSessionModel>> = _barChartData

    private val _focusTrendData = MutableLiveData<Map<String, Int>?>()
    val focusTrendData: LiveData<Map<String, Int>?> = _focusTrendData

    private val _sessionSuccessRate = MutableLiveData<Double?>()
    val sessionSuccessRate: LiveData<Double?> = _sessionSuccessRate

    private val _taskCompletionRate = MutableLiveData<Double?>()
    val taskCompletionRate: LiveData<Double?> = _taskCompletionRate

    private val _overdueTaskAnalysis =
        MutableLiveData<StatsCalculateHelper.OverdueAnalysisResult?>()
    val overdueTaskAnalysis: LiveData<StatsCalculateHelper.OverdueAnalysisResult?> =
        _overdueTaskAnalysis


    // --- Mediator to Trigger Updates ---
    // This MediatorLiveData observes time range changes and triggers updates
    // for both the bar chart filter AND the new stats calculations.
    private val statsUpdateTrigger = MediatorLiveData<Any?>()

    init {
        Log.d(TAG, "SessionViewModel init block START")
        checkUserProStatus() // Check on init
        // Define the common update logic as a separate function
        fun performCalculationsAndUpdates(sourceTrigger: String) {
            Log.d(TAG, "performCalculationsAndUpdates() CALLED by: $sourceTrigger")
            // 1. Init value
            val currentProStatus = _isUserPro.value == true
            val currentSessions = allSessions.value
            val currentRangeOption = _selectedTimeRange.value ?: TimeRangeOption.TODAY
            val currentCustomStart = _customStart.value
            val currentCustomEnd = _customEnd.value
            val millisRange = getSelectedMillisRange()

            // 2. Update Bar Chart Data (For everyone)
            if (currentSessions != null) {
                val filteredForBarChart = filterSessionsByLocalDate(
                    currentSessions,
                    currentRangeOption,
                    currentCustomStart,
                    currentCustomEnd
                )
                if (_barChartData.value != filteredForBarChart) {
                    _barChartData.value = filteredForBarChart
                }
            } else {
                if (_barChartData.value?.isNotEmpty() == true) _barChartData.value = emptyList()
            }

            // 3. Calculation for Pro status
            if (millisRange != null) {
                Log.d(
                    TAG,
                    "performCalculationsAndUpdates - Valid millisRange: $millisRange. Launching coroutines."
                )
                val startMillis = millisRange.first
                val endMillis = millisRange.second
                if (currentProStatus) {
                    Log.d(
                        TAG,
                        "User is PRO. Calculating Pro stats for range: $startMillis-$endMillis"
                    )
                    viewModelScope.launch {
                        try {
                            launch {
                                val trend =
                                    statsCalculator.getFocusTimeTrend(startMillis, endMillis)
                                _focusTrendData.postValue(trend)

                                val successRate =
                                    statsCalculator.getSessionSuccessRate(startMillis, endMillis)
                                _sessionSuccessRate.postValue(successRate)

                                val taskRate =
                                    statsCalculator.getTaskCompletionRate(startMillis, endMillis)
                                _taskCompletionRate.postValue(taskRate)

                                val overdueResult =
                                    statsCalculator.getOverdueTaskAnalysis(startMillis, endMillis)
                                _overdueTaskAnalysis.postValue(overdueResult)
                            }
                            // ... other launch blocks for other stats with logging ...
                        } catch (e: Exception) {
                            Log.e(TAG, "Error calculating stats", e)
                            _focusTrendData.postValue(emptyMap())
                            _sessionSuccessRate.postValue(0.0)
                            _taskCompletionRate.postValue(0.0)
                            _overdueTaskAnalysis.postValue(StatsCalculateHelper.OverdueAnalysisResult())                        // ...
                        }
                    }
                } else {
                    Log.d(TAG, "User is NOT PRO. Clearing Pro stats.")
                    // Set Pro stats to null or empty to indicate they are not available
                    if (_focusTrendData.value != null) _focusTrendData.value = null
                    if (_sessionSuccessRate.value != null) _sessionSuccessRate.value = null
                    if (_taskCompletionRate.value != null) _taskCompletionRate.value = null
                    if (_overdueTaskAnalysis.value != null) _overdueTaskAnalysis.value = null
                }
            } else {
                Log.w(
                    TAG,
                    "performCalculationsAndUpdates - Invalid millisRange for stats. Clearing stats."
                )
                _focusTrendData.value = emptyMap()
                _sessionSuccessRate.value = 0.0
                _taskCompletionRate.value = 0.0
                _overdueTaskAnalysis.value = StatsCalculateHelper.OverdueAnalysisResult()
            }
        }

        // Add sources to the mediator after being defined
        statsUpdateTrigger.addSource(_isUserPro) { isPro ->
            performCalculationsAndUpdates("_isUserPro")
        }
        statsUpdateTrigger.addSource(allSessions) { sessionsData ->
            performCalculationsAndUpdates("allSessions")
        }
        statsUpdateTrigger.addSource(_selectedTimeRange) { timeRangeOption ->
            performCalculationsAndUpdates("_selectedTimeRange")
        }
        statsUpdateTrigger.addSource(_customStart) { localDate ->
            performCalculationsAndUpdates("_customStart")
        }
        statsUpdateTrigger.addSource(_customEnd) { localDate ->
            performCalculationsAndUpdates("_customEnd")
        }

        // IMPORTANT: The MediatorLiveData itself needs to be "active" for the above
        // addSource callbacks to fire. We make it active by observing it, even if we
        // don't directly use its value.
        statsUpdateTrigger.observeForever {}
//        Log.d(TAG, "SessionViewModel init: Forcing _selectedTimeRange update to trigger mediator.")
//        _selectedTimeRange.value = _selectedTimeRange.value
        Log.d(TAG, "SessionViewModel init block END")

    }

    // Important
    override fun onCleared() {
        super.onCleared()
        statsUpdateTrigger.removeObserver { } // Simplified removal if only one observer
        Log.d(TAG, "SessionViewModel onCleared.")
    }

    // --- Helper Functions ---
    private fun checkUserProStatus() {
        viewModelScope.launch {
            val proStatus = firestoreRepository.isUserPro()
            Log.d(TAG, "User Pro Status from repository: $proStatus")
            _isUserPro.postValue(proStatus) // This will trigger the mediator if value changes
        }
    }

    // Load session stats based on the selected time range
    private fun filterSessionsByLocalDate(
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

        return sessions.filter { session ->
            try {
                val sessionDate = LocalDate.parse(session.date, DATE_FORMATTER)
                val afterStart = from?.let { sessionDate >= it } != false
                val beforeEnd = to?.let { sessionDate <= it } != false
                afterStart && beforeEnd
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing date: ${session.date} for session ${session.id}", e)
                false
            }
        }
    }

    private fun getSelectedMillisRange(): Pair<Long, Long>? {
        val range = _selectedTimeRange.value ?: TimeRangeOption.TODAY
        val startLocal = _customStart.value
        val endLocal = _customEnd.value

        return when (range) {
            TimeRangeOption.TODAY -> {
                val today = LocalDate.now()
                today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() to
                        today.atTime(23, 59, 59, 999_999_999).atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
            }

            TimeRangeOption.THIS_WEEK -> {
                val today = LocalDate.now()
                val startOfWeek = today.with(DayOfWeek.MONDAY)
                startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() to
                        today.atTime(23, 59, 59, 999_999_999).atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli() // End of today
            }

            TimeRangeOption.THIS_MONTH -> {
                val today = LocalDate.now()
                val startOfMonth = today.withDayOfMonth(1)
                startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() to
                        today.atTime(23, 59, 59, 999_999_999).atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli() // End of today
            }

            TimeRangeOption.CUSTOM -> {
                if (startLocal != null && endLocal != null && !endLocal.isBefore(startLocal)) {
                    startLocal.atStartOfDay(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli() to
                            endLocal.atTime(23, 59, 59, 999_999_999)
                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else {
                    null // Invalid custom range
                }
            }

            TimeRangeOption.ALL_TIME -> 0L to Long.MAX_VALUE // Represent ALL_TIME range
        }
    }

    fun setTimeRange(option: TimeRangeOption) {
        // Avoid redundant updates if the value hasn't changed
        if (_selectedTimeRange.value != option) {
            _selectedTimeRange.value = option
        }
    }

    fun onCustomDateRangeSelected(startDate: LocalDate, endDate: LocalDate) {
        // Avoid redundant updates
        if (_customStart.value != startDate
            || _customEnd.value != endDate
            || _selectedTimeRange.value != TimeRangeOption.CUSTOM
        ) {
            _customStart.value = startDate
            _customEnd.value = endDate
            _selectedTimeRange.value = TimeRangeOption.CUSTOM // Ensure mode is set
        }
    }

    val getTotalFocusTime: LiveData<String> = allSessions.map { sessions ->
        val totalMinutes = sessions
            .sumOf { it.durationMinutes }
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        "${hours}h ${minutes}m"
    }

    private fun calculateStreak(completedSessions: List<ConcentrateSessionModel>): Int {
        val completedDates = completedSessions
            .mapNotNull { session -> // Use mapNotNull to handle parsing errors gracefully
                try {
                    LocalDate.parse(session.date, DATE_FORMATTER)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date for streak calculation: ${session.date}", e)
                    null
                }
            }
            .distinct()
            .sortedDescending()

        var streak = 0
        var dateToCheck = LocalDate.now()

        for (date in completedDates) {
            if (date == dateToCheck) {
                streak++
                dateToCheck = dateToCheck.minusDays(1)
            } else if (date == dateToCheck.minusDays(1)) {
                // Allow for yesterday even if today wasn't done yet
                streak++
                dateToCheck = date.minusDays(1)
            } else if (date.isBefore(dateToCheck)) {
                // If there's a gap bigger than one day, the streak breaks
                break
            }
            // If date.isAfter(dateToCheck), it means future dates somehow got in, skip them.
        }

        // Check if today is part of the streak if no sessions yet today
        if (streak == 0 && completedDates.isNotEmpty() && completedDates.first() == LocalDate.now()
                .minusDays(1)
        ) {
            // If the most recent session was yesterday, the potential streak continues from yesterday
            // Recalculate starting from yesterday
            dateToCheck = LocalDate.now().minusDays(1)
            for (date in completedDates) {
                if (date == dateToCheck) {
                    streak++
                    dateToCheck = dateToCheck.minusDays(1)
                } else if (date.isBefore(dateToCheck)) {
                    break
                }
            }
        }

        return streak
    }
}