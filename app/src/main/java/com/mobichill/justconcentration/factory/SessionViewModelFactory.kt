package com.mobichill.justconcentration.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mobichill.justconcentration.repository.ConcentrateSessionRepository
import com.mobichill.justconcentration.viewmodel.SessionViewModel

class SessionViewModelFactory(private val sessionRepository: ConcentrateSessionRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionViewModel(sessionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}