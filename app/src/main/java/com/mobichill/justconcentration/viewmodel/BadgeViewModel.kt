package com.mobichill.justconcentration.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.mobichill.justconcentration.model.BadgeModel
import com.mobichill.justconcentration.repository.BadgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BadgeViewModel @Inject constructor(badgeRepository: BadgeRepository) : ViewModel() {
    val allBadges: LiveData<List<BadgeModel>> = badgeRepository.getAllBadges().asLiveData()
}