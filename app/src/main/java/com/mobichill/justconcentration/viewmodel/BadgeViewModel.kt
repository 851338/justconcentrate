package com.mobichill.justconcentration.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.mobichill.justconcentration.model.BadgeModel
import com.mobichill.justconcentration.repository.BadgeRepository

class BadgeViewModel(badgeRepository: BadgeRepository) : ViewModel() {
    val allBadges: LiveData<List<BadgeModel>> = badgeRepository.getAllBadges().asLiveData()
}