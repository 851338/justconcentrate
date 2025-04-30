package com.mobichill.justconcentration.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mobichill.justconcentration.viewmodel.BadgeViewModel

class BadgeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BadgeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BadgeViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}