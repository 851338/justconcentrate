package com.mobichill.justconcentration.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mobichill.justconcentration.repository.BadgeRepository
import com.mobichill.justconcentration.viewmodel.BadgeViewModel

class BadgeViewModelFactory(private val badgeRepository: BadgeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BadgeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BadgeViewModel(badgeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}