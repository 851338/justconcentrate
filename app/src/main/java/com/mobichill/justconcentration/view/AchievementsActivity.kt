package com.mobichill.justconcentration.view

import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityAchievementsBinding
import com.mobichill.justconcentration.databinding.DialogBadgeDetailsBinding
import com.mobichill.justconcentration.manager.AdsManager
import com.mobichill.justconcentration.model.BadgeModel
import com.mobichill.justconcentration.utils.ConvertUtils
import com.mobichill.justconcentration.view.adapter.BadgeAdapter
import com.mobichill.justconcentration.viewmodel.BadgeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AchievementsActivity : BaseViewBindingActivity<ActivityAchievementsBinding>() {

    @Inject
    lateinit var adsManager: AdsManager
    private lateinit var badgeAdapter: BadgeAdapter

    private val badgeViewModel: BadgeViewModel by viewModels()

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

        // Setup ads
        lifecycleScope.launch {
            adsManager.loadAndShowBannerAd(binding.adView)
        }
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

            // Showing status, progress & criteria
            if (badge.goal > 0) {
                val progressPercent = (badge.progress * 100) / badge.goal
                dialogBinding.badgeStatus.append(
                    getString(
                        R.string.progress,
                        badge.progress,
                        badge.goal,
                        progressPercent
                    ))
                dialogBinding.badgeProgress.visibility = View.VISIBLE
                dialogBinding.badgeProgress.progress = progressPercent
            } else {
                dialogBinding.badgeStatus.append(getString(R.string.how_to_unlock, badge.criteria))
                dialogBinding.badgeProgress.visibility = View.GONE
            }

            // Pro requirement text
            if (badge.isPro) {
                dialogBinding.badgeProRequirementText.visibility = View.VISIBLE
                dialogBinding.badgeProRequirementText.text =
                    getString(R.string.pro_subscription_required_to_unlock)
                if (badge.isUnlocked) {
                    dialogBinding.badgeProRequirementText.text = getString(R.string.pro_badge)
                }
            } else {
                dialogBinding.badgeProRequirementText.visibility = View.GONE
            }
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    override fun onPause() {
        super.onPause()
        adsManager.onPause(binding.adView)
    }

    override fun onResume() {
        super.onResume()
        adsManager.onResume(binding.adView)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing)
            adsManager.onDestroy(binding.adView)
    }
}