package com.mobichill.justconcentration.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mobichill.justconcentration.base.application.MyApp
import com.mobichill.justconcentration.helper.StatsCalculateHelper
import com.mobichill.justconcentration.viewmodel.SessionViewModel

class SessionViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            val myApp = application as MyApp
            val statsCalculator = StatsCalculateHelper(
                taskRepository = myApp.taskRepository,
                sessionRepository = myApp.concentrateSessionRepository
            )
            @Suppress("UNCHECKED_CAST")
            return SessionViewModel(myApp.concentrateSessionRepository, statsCalculator) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}