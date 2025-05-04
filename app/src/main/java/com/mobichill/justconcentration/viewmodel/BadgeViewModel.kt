package com.mobichill.justconcentration.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.mobichill.justconcentration.model.BadgeModel
import com.mobichill.justconcentration.repository.BadgeRepository

class BadgeViewModel(private val badgeRepository: BadgeRepository) : ViewModel() {
    private val TAG = javaClass.simpleName
    val allBadges: LiveData<List<BadgeModel>> = badgeRepository.getAllBadges().asLiveData()

}