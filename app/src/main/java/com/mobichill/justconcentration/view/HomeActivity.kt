package com.mobichill.justconcentration.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.BuildConfig
import com.mobichill.justconcentration.ConcentrateSetupActivity
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.application.MyApp
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityHomeBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.popup.UserPopup
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.others.MyContextWrapper
import com.mobichill.justconcentration.util.Utils
import com.mobichill.justconcentration.util.Utils.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : BaseViewBindingActivity<ActivityHomeBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            // Debug-specific behavior
            Log.d("HomeActivity", "This is a debug build!")
        }
        //load user avatar
        if (Utils.isUserLoggedIn(this)) {
            val uid = Utils.getUserIdFromSF(this)
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

        CoroutineScope(Dispatchers.IO).launch {
            if (Utils.isNetworkAvailable(this@HomeActivity)) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    //Sync sessions
                    try {
                        //sync from room to fireStore
                        FireStoreRepository().syncUnsyncedSessionToFireStore(user.uid)
                        //sync from fireStore to room
                        val sessions = FireStoreRepository().getSessionsFromFireStore()
                        MyApp.instance.concentrateSessionRepository.syncSessionsToRoom(sessions)
                    } catch (e: Exception) {
                        Log.e(TAG, "Sync sessions: ", e)
                    }
                    //Sync deleted tasks
                    try {
                        val deletedRoomTasks = MyApp.instance.taskRepository.getUnsyncedDeletedTasks()
                        deletedRoomTasks.forEach { t ->
                            FireStoreRepository().deleteTaskFromFireStore(t)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Sync deleted tasks: ", e)
                    }
                }
            }
        }
        super.onCreate(savedInstanceState)
    }

    fun setupToolbar(title: String, isBackEnabled: Boolean) {
        binding.btnBack.isVisible = isBackEnabled
        binding.abTitle.text = title
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(getString(R.string.main_title), false)
    }

    override fun onBackPressed() {
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when (current) {
            is AboutFragment, is ViewStatsFragment -> {
                onBackPressedDispatcher.onBackPressed()
                setupToolbar(getString(R.string.main_title), false)
            }
            else ->
                super.onBackPressed()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(MyContextWrapper.wrap(newBase, "en"))
    }

    override fun initViewBinding(): ActivityHomeBinding =
        ActivityHomeBinding.inflate(layoutInflater)

    override fun initView(): Unit = with(binding) {
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
                    openConcentrateSetup()
                }
            }
        )
        cardStats.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    openStatsFragment()
                }
            }
        )
        cardSubscription.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    //TODO
                }
            }
        )
        super.initView()
    }

    private fun showUserPopup(view: View) {
        val popup = UserPopup(this)
        popup.show(view)
    }

    private fun openConcentrateSetup() {
        startActivity(Intent(this, ConcentrateSetupActivity::class.java))
    }
    
    private fun openTaskActivity() {
        startActivity(Intent(this, TaskActivity::class.java))
    }
    
    private fun openStatsFragment() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            .replace(binding.fragmentContainer.id, ViewStatsFragment())
            .addToBackStack(null)
            .commit()
    }

    fun openAboutFragment() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            .replace(binding.fragmentContainer.id, AboutFragment())
            .addToBackStack(null)
            .commit()
    }

    fun setUIAfterLogout() {
        Utils.setAvatar(this, null, binding.ivAvatar)
    }
}