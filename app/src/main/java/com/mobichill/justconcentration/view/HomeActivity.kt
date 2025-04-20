package com.mobichill.justconcentration.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
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
import com.mobichill.justconcentration.util.MyContextWrapper
import com.mobichill.justconcentration.util.Utils
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
                    Utils.setAvatar(this@HomeActivity, user?.profilePic, binding.ivAvatar)
                }
            }
        } else binding.ivAvatar.setImageResource(R.drawable.ic_setting)

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
            is AboutFragment ->
                onBackPressedDispatcher.onBackPressed()

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
        btnTask.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    openTaskActivity()
                }
            })
        ivAvatar.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    showUserPopup(ivAvatar)
                }
            })
        btnConcentrate.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    openConcentrateSetup()
                }
            }
        )
        super.initView()
    }

    private fun openTaskActivity() {
        startActivity(Intent(this, TaskActivity::class.java))
    }

    private fun showUserPopup(view: View) {
        val popup = UserPopup(this)
        popup.show(view)
    }

    private fun openConcentrateSetup() {
        startActivity(Intent(this, ConcentrateSetupActivity::class.java))
    }

    fun openAboutFragment() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, AboutFragment())
            .addToBackStack(null)
            .commit()
    }

    fun setUIAfterLogout() {
        Utils.setAvatar(this, null, binding.ivAvatar)
    }
}