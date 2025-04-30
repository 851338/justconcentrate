package com.mobichill.justconcentration.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdRequest
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.BuildConfig
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.application.MyApp
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityHomeBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.NAME_SETTINGS_PREFS
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_SETTINGS_SYNC
import com.mobichill.justconcentration.constants.MyContextWrapper
import com.mobichill.justconcentration.view.popup.UserPopup
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.utils.ConvertUtils.px
import com.mobichill.justconcentration.utils.SFUtils
import com.mobichill.justconcentration.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : BaseViewBindingActivity<ActivityHomeBinding>() {
    private val prefs by lazy {
        getSharedPreferences(NAME_SETTINGS_PREFS, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            // Debug-specific behavior
            Log.d("HomeActivity", "This is a debug build!")
        }
        //load user avatar
        if (SFUtils.isUserLoggedIn(this)) {
            val uid = SFUtils.getUserIdFromSF(this)
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

        binding.btnBack.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    onBackPressed()
                }
            }
        )
        binding.btnBack.visibility = View.GONE

        // Sync feature
        val isSynced = prefs.getBoolean(KEY_SETTINGS_SYNC, false)
        val fireStoreRepo = FireStoreRepository()
        if (!isSynced) return
        CoroutineScope(Dispatchers.IO).launch {
            if (Utils.isNetworkAvailable(this@HomeActivity)) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    //Sync sessions
                    try {
                        //sync from room to fireStore
                        fireStoreRepo.syncUnsyncedSessionToFireStore(user.uid)
                        //sync from fireStore to room
                        val sessions = fireStoreRepo.getSessionsFromFireStore()
                        MyApp.instance.concentrateSessionRepository.syncSessionsToRoom(sessions)
                    } catch (e: Exception) {
                        Log.e(TAG, "Sync sessions: ", e)
                    }
                    //Sync deleted tasks
                    try {
                        val deletedRoomTasks =
                            MyApp.instance.taskRepository.getUnsyncedDeletedTasks()
                        deletedRoomTasks.forEach { t ->
                            fireStoreRepo.deleteTaskFromFireStore(t)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Sync deleted tasks: ", e)
                    }
                }
            }
        }
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
}