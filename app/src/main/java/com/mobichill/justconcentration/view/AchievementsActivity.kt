package com.mobichill.justconcentration.view

import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.base.application.MyApp
import com.mobichill.justconcentration.databinding.ActivityAchievementsBinding
import com.mobichill.justconcentration.factory.BadgeViewModelFactory
import com.mobichill.justconcentration.model.BadgeModel
import com.mobichill.justconcentration.view.adapter.BadgeAdapter
import com.mobichill.justconcentration.viewmodel.BadgeViewModel

class AchievementsActivity : BaseViewBindingActivity<ActivityAchievementsBinding>() {
    private lateinit var badgeAdapter: BadgeAdapter
    val badgeViewModelFactory by lazy {
        BadgeViewModelFactory(MyApp.instance.badgeRepository)
    }
    val badgeViewModel: BadgeViewModel by viewModels {
        badgeViewModelFactory
    }
    override fun initViewBinding(): ActivityAchievementsBinding =
        ActivityAchievementsBinding.inflate(layoutInflater)

    override fun initView() {
        super.initView()

        badgeAdapter = BadgeAdapter { badge ->


        }
        binding.recyclerViewBadges.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewBadges.adapter = badgeAdapter

        badgeViewModel
    }
}