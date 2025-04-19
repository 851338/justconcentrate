package com.mobichill.justconcentration.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mobichill.justconcentration.BuildConfig
import com.mobichill.justconcentration.ConcentrateSetupActivity
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityHomeBinding
import com.mobichill.justconcentration.helper.RoomHelper
import com.mobichill.justconcentration.popup.UserPopup
import com.mobichill.justconcentration.repository.RoomRepository
import com.mobichill.justconcentration.util.MyContextWrapper
import com.mobichill.justconcentration.util.OnSingleClickListener
import com.mobichill.justconcentration.util.Utils
import com.mobichill.justconcentration.worker.AutoDeleteOldTasksWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

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
                    RoomRepository(RoomHelper.getInstance(this@HomeActivity)).getUserById(uid)
                // Update UI here
                withContext(Dispatchers.Main) {
                    Utils.setAvatar(this@HomeActivity, user?.profilePic, binding.ivAvatar)
                }
            }
        } else binding.ivAvatar.setImageResource(R.drawable.baseline_settings_24)

        binding.btnBack.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    onBackPressed()
                }
            }
        )
        binding.btnBack.visibility = View.GONE

        //auto delete task in trash bin after 7 days
        scheduleAutoDeleteWorker()
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

    //delete old tasks after 7 day, run daily
    private fun scheduleAutoDeleteWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Only run when online
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AutoDeleteOldTasksWorker>(
            1, TimeUnit.DAYS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AutoDeleteOldTasks",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
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