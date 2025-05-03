package com.mobichill.justconcentration.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.mobichill.justconcentration.BuildConfig
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.base.application.MyApp
import com.mobichill.justconcentration.constants.MyContextWrapper
import com.mobichill.justconcentration.databinding.ActivityHomeBinding
import com.mobichill.justconcentration.helper.SyncHelper
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.manager.BadgeProgressManager
import com.mobichill.justconcentration.utils.ConvertUtils.px
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils
import com.mobichill.justconcentration.view.popup.UserPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HomeActivity : BaseViewBindingActivity<ActivityHomeBinding>() {
    private val sfUtils: SharedPreferencesUtils by lazy {
        SharedPreferencesUtils(applicationContext)
    }

    private val badgeProgressManager: BadgeProgressManager by lazy {
        BadgeProgressManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            // Debug-specific behavior
            Log.d("HomeActivity", "This is a debug build!")
        }
        // Check login streak and handle comeback
        handleDailyActivityCheck()

        // Sync function
        val isSynced = sfUtils.isSettingsSyncEnabled()
        if (!isSynced) return
        SyncHelper.enqueueOneTimeSync(this)

        // Load user avatar
        if (sfUtils.isUserLoggedIn()) {
            val uid = sfUtils.getUserId()
            CoroutineScope(Dispatchers.IO).launch {
                val user =
                    MyApp.instance.userRepository.getUserById(uid)
                // Update UI here
                withContext(Dispatchers.Main) {
                    binding.ivAvatar.setPadding(0, 0, 0, 0)
                    Utils.setAvatar(this@HomeActivity, user?.profilePic, binding.ivAvatar)
                }
            }
        } else {
            binding.ivAvatar.setPadding(px(5), px(5), px(5), px(5))
            binding.ivAvatar.setImageResource(R.drawable.ic_setting)
        }
        binding.btnBack.visibility = View.GONE
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(MyContextWrapper.wrap(newBase, "en"))
    }

    override fun initViewBinding(): ActivityHomeBinding =
        ActivityHomeBinding.inflate(layoutInflater)

    override fun initView(): Unit = with(binding) {
        super.initView()
        ivAvatar.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    showUserPopup(ivAvatar)
                }
            })
        cardTasks.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    openTaskActivity()
                }
            })
        cardConcentrate.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    openConcentrateSetupActivity()
                }
            }
        )
        cardStats.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    openStatsActivity()
                }
            }
        )
        cardAchievements.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    openAchievementsActivity()
                }
            }
        )

        //run ads
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun showUserPopup(view: View) {
        val popup = UserPopup(this)
        popup.show(view)
    }

    private fun openConcentrateSetupActivity() {
        startActivity(Intent(this, ConcentrateSetupActivity::class.java))
    }

    private fun openTaskActivity() {
        startActivity(Intent(this, TaskActivity::class.java))
    }

    fun openSettingsActivity() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun openStatsActivity() {
        startActivity(Intent(this, ViewStatsActivity::class.java))
    }

    private fun openAchievementsActivity() {
        startActivity(Intent(this, AchievementsActivity::class.java))
    }

    fun setUIAfterLogout() {
        Utils.setAvatar(this, null, binding.ivAvatar)
    }

    private fun handleDailyActivityCheck() {
        lifecycleScope.launch {
            val currentStreakForToday = withContext(Dispatchers.IO) {
                calculateStreakAndCheckComeback()
            }
            // Always update the streak badge based on today's calculated value.
            Log.d(
                TAG,
                "Updating login streak badge. Today's streak value: $currentStreakForToday days"
            )
            try {
                badgeProgressManager.updateLoginStreak(currentStreakForToday)
                Log.d(TAG, "Login streak badge update call finished.")
            } catch (e: Exception) {
                Log.e(TAG, "Error calling updateLoginStreak", e)
            }

        }
    }

    /**
     * Calculates the current login streak and checks for the comeback condition.
     * Updates persistent storage (last active date, streak count).
     * Calls the comeback badge update directly if conditions are met.
     * Should run on a background thread (e.g., Dispatchers.IO).
     *
     * @return The calculated login streak count applicable for *today*.
     */
    private suspend fun calculateStreakAndCheckComeback(): Int {
        val today = LocalDate.now()
        // Get the last active date *before* any updates for today
        val lastActiveDate = sfUtils.getLastActiveDate()
        // Case 1: Already active today - Return current streak, no storage changes needed.
        if (lastActiveDate == today) {
            Log.d(TAG, "Already active today.")
            return sfUtils.getCurrentLoginStreak()
        }
        // Case 2: If not active today, proceed with calculations
        var calculatedStreak: Int
        if (lastActiveDate == null) {
            // Case 2.1: First run / No previous date stored
            Log.d(TAG, "First run detected.")
            calculatedStreak = 1 // Start streak at 1
        } else {
            // Case 2.2: Previous activity exists, but not today.
            val daysAgo = ChronoUnit.DAYS.between(lastActiveDate, today)
            Log.d(TAG, "Last active was $daysAgo days ago.")
            // --- Comeback Check ---
            if (daysAgo >= 3) {
                Log.d(TAG, "Comeback condition met (>= 3 days).")
                try {
                    badgeProgressManager.updateComeback(daysAgo.toInt())
                    Log.d(TAG, "Comeback badge update call finished.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error calling updateComeback", e)
                }
            }
            // --- Calculate Streak Continuing case 3---
            if (lastActiveDate == today.minusDays(1)) {
                // Case 2.2.1: consecutive day
                Log.d(TAG, "Consecutive day.")
                calculatedStreak = sfUtils.getCurrentLoginStreak() + 1 // Increment stored streak
            } else {
                // Case 2.2.2: Gap detected - Streak resets
                Log.d(TAG, "Streak broken or gap detected.")
                calculatedStreak = 1 // Reset streak to 1
            }
        }
        // Still case 2
        Log.d(TAG, "Updating storage: Date=$today, Streak=$calculatedStreak")
        sfUtils.setLastActiveDate(today)
        sfUtils.setCurrentLoginStreak(calculatedStreak)
        return calculatedStreak
    }

}