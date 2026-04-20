package com.mobichill.justconcentration.view

import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.base.application.MyApp
import com.mobichill.justconcentration.databinding.ActivityAchievementsBinding
import com.mobichill.justconcentration.databinding.DialogBadgeDetailsBinding
import com.mobichill.justconcentration.factory.BadgeViewModelFactory
import com.mobichill.justconcentration.manager.BannerAdManager
import com.mobichill.justconcentration.model.BadgeModel
import com.mobichill.justconcentration.utils.ConvertUtils
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
            showBadgeDetailsDialog(badge)
        }
        binding.recyclerViewBadges.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewBadges.adapter = badgeAdapter

        badgeViewModel.allBadges.observe(this) { badgesList ->
            // Check if the list is not null before submitting
            badgesList?.let {
                // Submit the updated list to the ListAdapter
                badgeAdapter.updateItems(it)
            }
            binding.recyclerViewBadges.visibility =
                if (badgesList.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        BannerAdManager.loadBanner(binding.adView)
    }

    private fun showBadgeDetailsDialog(badge: BadgeModel) {
        val dialogBinding = DialogBadgeDetailsBinding.inflate(layoutInflater)

        val actualResId = badge.getDrawableResourceId(this)
        dialogBinding.badgeIcon.setImageResource(actualResId)
        dialogBinding.badgeName.text = badge.name
        dialogBinding.badgeDescription.text = badge.description

        if (badge.isUnlocked) {
            dialogBinding.badgeStatus.text = if (badge.unlockedAt != null) {
                val date = ConvertUtils.convertTimeMillisIntoDateString(badge.unlockedAt!!)
                getString(R.string.status_unlocked_on, date)
            } else getString(R.string.status_unlocked)
            dialogBinding.badgeProgress.visibility = View.GONE
        } else {
            dialogBinding.badgeStatus.text = getString(R.string.status_locked)
            if (badge.goal > 0) {
                val progressPercent = (badge.progress * 100) / badge.goal
                dialogBinding.badgeStatus.append("\nProgress: ${badge.progress} / ${badge.goal} ($progressPercent%)")
                dialogBinding.badgeProgress.visibility = View.VISIBLE
                dialogBinding.badgeProgress.progress = progressPercent
            } else {
                dialogBinding.badgeStatus.append("\nHow to unlock: [Your unlock criteria text here]")
                dialogBinding.badgeProgress.visibility = View.GONE
            }
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}