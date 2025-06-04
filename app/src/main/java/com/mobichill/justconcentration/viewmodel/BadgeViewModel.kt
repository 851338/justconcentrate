package com.mobichill.justconcentration.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mobichill.justconcentration.manager.ProBadgeManager
import com.mobichill.justconcentration.model.BadgeModel
import com.mobichill.justconcentration.repository.BadgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BadgeViewModel @Inject constructor(
    badgeRepository: BadgeRepository,
    proBadgeManager: ProBadgeManager
) : ViewModel() {

    companion object {
        private val TAG = BadgeViewModel::class.java.simpleName
    }

    val allBadges: LiveData<List<BadgeModel>> = badgeRepository.getAllBadges().asLiveData()

    init {
        Log.d(TAG, "SessionViewModel init block START")

        // Checking pro badges
        viewModelScope.launch {
            Log.d(TAG, "SessionViewModel init: Calling checkAndUpdateLoyalistBadges.")
            proBadgeManager.checkAndUpdateLoyalistBadges()
        }
    }
}