package com.mobichill.justconcentration.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.mobichill.justconcentration.application.MyApp
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.others.TimeRangeOption
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class SessionViewModel : ViewModel() {
    private val TAG = javaClass.canonicalName
    private val sessionRepository = MyApp.instance.concentrateSessionRepository
    val _selectedTimeRange = MutableLiveData<TimeRangeOption>(TimeRangeOption.ALL_TIME)
    val selectedTimeRange: LiveData<TimeRangeOption> = _selectedTimeRange

    val allSessions: LiveData<List<ConcentrateSessionModel>> =
        sessionRepository.getAllSessions().asLiveData()

    private val _customStart = MutableLiveData<LocalDate?>()
    private val _customEnd = MutableLiveData<LocalDate?>()

    val sessionStats: LiveData<List<ConcentrateSessionModel>> =
        MediatorLiveData<List<ConcentrateSessionModel>>().apply {
            fun update() {
                val sessions = allSessions.value.orEmpty()
                val range = _selectedTimeRange.value ?: TimeRangeOption.ALL_TIME
                val start = _customStart.value
                val end = _customEnd.value
                value = filterSessions(sessions, range, start, end)
            }
            addSource(allSessions) { update() }
            addSource(_selectedTimeRange) { update() }
            addSource(_customStart) { update() }
            addSource(_customEnd) { update() }
        }

    // Load session stats based on the selected time range
    private fun filterSessions(
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

            TimeRangeOption.CUSTOM -> start!! to end!!
            TimeRangeOption.ALL_TIME -> null to null
        }

        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
        return sessions.filter {
            val sessionDate = LocalDate.parse(it.date, formatter)
            val afterStart = from?.let { sessionDate >= it } != false
            val beforeEnd = to?.let { sessionDate <= it } != false
            afterStart && beforeEnd
        }
    }

    fun onCustomDateRangeSelected(startDate: LocalDate, endDate: LocalDate) {
        _customStart.value = startDate
        _customEnd.value = endDate
        _selectedTimeRange.value = TimeRangeOption.CUSTOM
    }
}