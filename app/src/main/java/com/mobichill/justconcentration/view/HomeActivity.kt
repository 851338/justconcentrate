package com.mobichill.justconcentration.view

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.install.model.AppUpdateType
import com.mobichill.justconcentration.BuildConfig
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityHomeBinding
import com.mobichill.justconcentration.helper.SyncHelper
import com.mobichill.justconcentration.listener.AppUpdateListener
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.manager.AdsManager
import com.mobichill.justconcentration.manager.BadgeProgressManager
import com.mobichill.justconcentration.manager.MyUpdateManager
import com.mobichill.justconcentration.repository.UserRepository
import com.mobichill.justconcentration.utils.ConvertUtils.px
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils
import com.mobichill.justconcentration.utils.Utils.openActivity
import com.mobichill.justconcentration.view.popup.UserPopup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.repository.DataCleanupRepository

@AndroidEntryPoint
class HomeActivity : BaseViewBindingActivity<ActivityHomeBinding>(), AppUpdateListener {

    @Inject
    lateinit var popup: UserPopup

    @Inject
    lateinit var adsManager: AdsManager

    @Inject
    lateinit var badgeProgressManager: BadgeProgressManager

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var dataCleanupRepository: DataCleanupRepository

    // Not hilt
    private lateinit var myUpdateManager: MyUpdateManager
    private var chosenUpdateType = AppUpdateType.FLEXIBLE // Default or decide dynamically

    private val sfUtils: SharedPreferencesUtils by lazy {
        SharedPreferencesUtils(applicationContext)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            // Debug-specific behavior
            Log.d("HomeActivity", "This is a debug build!")
        }
        handleUpdate()
        handleDailyActivityCheck()
        enqueueSync()
    }

    override fun initViewBinding(): ActivityHomeBinding =
        ActivityHomeBinding.inflate(layoutInflater)

    override fun initView() {
        super.initView()
        loadUserAvatar()
        setupOnclick()
        setupAds()
    }

    private fun enqueueSync() {
        val isSynced = sfUtils.isSettingsSyncEnabled()
        if (!isSynced) return
        SyncHelper.enqueueOneTimeSync(this)
    }

    private fun handleUpdate() {
        myUpdateManager = MyUpdateManager(
            activity = this,
            currentUpdateType = chosenUpdateType,
            appUpdateListener = this,
            checkForUpdateOnStart = true
        )
    }

    private fun setupAds() {
        lifecycleScope.launch {
            adsManager.loadAndShowBannerAd(binding.adView)
        }
    }

    private fun loadUserAvatar() {
        if (sfUtils.isUserLoggedIn()) {
            CoroutineScope(Dispatchers.IO).launch {
                val fallbackAvatarUrl = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()

                // Update UI here
                withContext(Dispatchers.Main) {
                    binding.ivAvatar.setPadding(0, 0, 0, 0)
                    Utils.setAvatar(this@HomeActivity, fallbackAvatarUrl, binding.ivAvatar)
                    Log.d(TAG, "Set user avatar: $fallbackAvatarUrl")
                }
            }
        } else {
            binding.ivAvatar.setPadding(px(5), px(5), px(5), px(5))
            binding.ivAvatar.setImageResource(R.drawable.ic_setting)
            Log.d(TAG, "ivAvatar: Settings icon")
        }
    }

    private fun setupOnclick() = with(binding) {
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
    }

    private fun showUserPopup(view: View) {
        popup.show(view)
    }

    private fun openConcentrateSetupActivity() = openActivity<ConcentrateSetupActivity>()
    private fun openTaskActivity() = openActivity<TaskActivity>()
    fun openSettingsActivity() = openActivity<SettingsActivity>()
    fun openWelcomeActivity() = openActivity<WelcomeActivity>()
    fun openSubscriptionActivity() = openActivity<SubscriptionActivity>()
    private fun openStatsActivity() = openActivity<ViewStatsActivity>()
    private fun openAchievementsActivity() = openActivity<AchievementsActivity>()

    fun clearDataAfterLogout() {
        lifecycleScope.launch {
            dataCleanupRepository.clearAllAndPrepopulateDatabase()
        }
        Utils.setAvatar(this, null, binding.ivAvatar)
    }

    private fun handleDailyActivityCheck() {
        lifecycleScope.launch {
            val currentStreakForToday = withContext(Dispatchers.IO) {
                calculateStreakAndCheckComeback()
            }
            Log.d(TAG, "Updating login streak. Today's streak value: $currentStreakForToday days")
            try {
                badgeProgressManager.updateLoginStreak(currentStreakForToday)
                Log.d(TAG, "Login streak badge update call finished.")
            } catch (e: Exception) {
                Log.e(TAG, "Error calling updateLoginStreak", e)
            }
        }
    }

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
        val calculatedStreak: Int
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


    // ---LISTENER IMPLEMENTATION---

    override fun showUpdateDownloadedSnackbar(onCompleteUpdate: () -> Unit) {
        Log.d(TAG, "showUpdateDownloadedSnackbar called")
        Snackbar.make(
            findViewById(android.R.id.content), // Use root content view
            "A new version has been downloaded and is ready to install.",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("RESTART") {
                onCompleteUpdate() // This will call appUpdateManager.completeUpdate()
            }
            // setActionTextColor(getColor(R.color.your_color)) // Optional: customize color
            show()
        }
    }

    override fun onUpdateFlowStartFailed(error: Exception) {
        Log.e(TAG, "Update flow could not be started: ${error.message}", error)
    }

    override fun onUpdateFlowResultOk() {
        Log.i(TAG, "Update flow successful (RESULT_OK). Type: $chosenUpdateType")
        if (chosenUpdateType == AppUpdateType.FLEXIBLE) {
            Utils.showToast(this, getString(R.string.update_download_started))
        }
        // For IMMEDIATE, app will likely restart soon. No Toast needed usually.
    }

    override fun onUpdateFlowResultCancelled() {
        Log.w(TAG, "Update flow cancelled by user. Type: $chosenUpdateType")
        Utils.showToast(this, getString(R.string.update_canceled))
    }

    override fun onUpdateFlowResultFailed(resultCode: Int) {
        Log.e(TAG, "Update flow failed with result code: $resultCode. Type: $chosenUpdateType")
    }

    override fun onUpdateNotAvailable() {}

    // ---ACTIVITY LIFECYCLE FOR ADS MANAGER ---
    override fun onPause() {
        super.onPause()
        adsManager.onPause(binding.adView)
    }

    override fun onResume() {
        super.onResume()
        setupAds()
        adsManager.onResume(binding.adView)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing)
            adsManager.onDestroy(binding.adView)
    }
}