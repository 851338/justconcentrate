package com.mobichill.justconcentration.view

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.install.model.AppUpdateType
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivitySettingsBinding
import com.mobichill.justconcentration.listener.AppUpdateListener
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.manager.MyUpdateManager
import com.mobichill.justconcentration.utils.AudioUtils
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : BaseViewBindingActivity<ActivitySettingsBinding>(), AppUpdateListener {
    private lateinit var settingsUpdateManager: MyUpdateManager
    private val updateTypeForSettings = AppUpdateType.FLEXIBLE

    private enum class SoundType {
        ALARM, FOCUS
    }

    private val sfUtils: SharedPreferencesUtils by lazy {
        SharedPreferencesUtils(applicationContext)
    }

    private var currentSoundType: SoundType? = null

    private lateinit var pickAudioLauncher: ActivityResultLauncher<Intent>

    override fun initViewBinding(): ActivitySettingsBinding =
        ActivitySettingsBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle pick audio from file only
        pickAudioLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val audioUri: Uri? = result.data?.data
                    if (audioUri != null) {
                        checkAndSaveAudioFile(audioUri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            AudioUtils.persistUriPermission(this@SettingsActivity, audioUri)
                        }
                    }
                }
            }

        // Update manager
        settingsUpdateManager = MyUpdateManager(
            activity = this, // Pass this SettingsActivity
            currentUpdateType = updateTypeForSettings,
            appUpdateListener = this,
            checkForUpdateOnStart = false
        )

        // Override onBackPressed
        onBackPressedDispatcher.addCallback(this) {
            setupToolbar(getString(R.string.settings))
            val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
            when (current) {
                is AboutFragment, is ChangePasswordFragment -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }

                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        super.onCreate(savedInstanceState)
    }

    override fun initView() = with(binding) {
        super.initView()
        // Back button
        btnBack.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                onBackPressedDispatcher.onBackPressed()
            }
        })

        // Sync with cloud switch
        itemSyncWithCloud.settingToggleTitle.text = getString(R.string.sync_with_cloud)
        val isSynced = sfUtils.isSettingsSyncEnabled()
        itemSyncWithCloud.settingToggleSwitch.isChecked = isSynced
        itemSyncWithCloud.settingToggleSwitch.setOnCheckedChangeListener { switch, isChecked ->
            switch.isEnabled = false
            if (sfUtils.isUserLoggedIn()) {
                sfUtils.updateSettingSync(isChecked)
                Handler(Looper.getMainLooper()).postDelayed({
                    switch.isEnabled = true
                }, 1000)
            } else {
                switch.isChecked = !isChecked
                Utils.showToast(this@SettingsActivity, getString(R.string.you_must_log_in_first))
            }
        }

        // Change password button
        itemChangePassword.settingTitle.text = getString(R.string.change_password)
        val isGoogleUser = FirebaseAuth.getInstance()
            .currentUser
            ?.providerData
            ?.any { it.providerId == "google.com" } == true
        itemChangePassword.root.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    // Do not let google user change password in-app
                    if (!sfUtils.isUserLoggedIn()) {
                        Utils.showToast(
                            this@SettingsActivity,
                            getString(R.string.you_must_log_in_first)
                        )
                        return
                    }
                    if (isGoogleUser) {
                        Utils.showToast(
                            this@SettingsActivity,
                            getString(R.string.google_signed_password_change_not_available)
                        )
                    } else {
                        openChangePasswordFragment()
                    }
                }
            })

        // Vibration switch
        itemVibrationSwitch.settingToggleTitle.text = getString(R.string.vibration)
        val vibrationEnabled = sfUtils.isVibrationEnabled()
        itemVibrationSwitch.settingToggleSwitch.isChecked = vibrationEnabled
        itemVibrationSwitch.settingToggleSwitch.setOnCheckedChangeListener { switch, isChecked ->
            switch.isEnabled = false
            sfUtils.updateSettingsVibration(isChecked)
            Handler(Looper.getMainLooper()).postDelayed({
                switch.isEnabled = true
            }, 1000)
        }

        // Default alarm sound
        selectedAlarmSound.text = AudioUtils.defaultAlarmName(this@SettingsActivity)
        itemAlarmSound.apply {
            settingTitle.text = getString(R.string.default_alarm_sound)
            toolTip.visibility = View.VISIBLE
            toolTip.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    showToolTip(getString(R.string.default_alarm_tooltip), toolTip)
                }
            })
        }
        itemAlarmSound.root.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    currentSoundType = SoundType.ALARM
                    AudioUtils.showSoundChoiceDialog(this@SettingsActivity, pickAudioLauncher)
                }
            }
        )

        //Default session sound
        selectedSessionSound.text = AudioUtils.defaultSessionName(this@SettingsActivity)
        itemSessionSound.apply {
            settingTitle.text = getString(R.string.default_session_sound)
            toolTip.visibility = View.VISIBLE
            toolTip.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    showToolTip(getString(R.string.default_session_tooltip), toolTip)
                }
            })
        }
        itemSessionSound.root.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    currentSoundType = SoundType.FOCUS
                    AudioUtils.openFilePicker(pickAudioLauncher)
                }
            }
        )
        removeSound.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    selectedSessionSound.text = getString(R.string.silence)
                    sfUtils.removeSettingsDefaultSessionSound()
                }
            }
        )

        // Dark mode
        itemDarkMode.settingToggleTitle.text = getString(R.string.dark_mode_text)
        val darkModeEnabled = sfUtils.isDarkModeEnabled()
        itemDarkMode.settingToggleSwitch.isChecked = darkModeEnabled
        itemDarkMode.settingToggleSwitch.setOnCheckedChangeListener { switch, isChecked ->
            switch.isEnabled = false
            sfUtils.updateSettingsDarkMode(isChecked)
            Handler(Looper.getMainLooper()).postDelayed({
                switch.isEnabled = true
            }, 1000)
        }

        // Update button
        itemUpdate.settingTitle.text = getString(R.string.check_for_update)
        itemUpdate.root.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                if (::settingsUpdateManager.isInitialized)
                    settingsUpdateManager.checkForUpdate()
            }
        })

        // About button
        itemAbout.settingTitle.text = getString(R.string.about)
        itemAbout.root.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                openAboutFragment()
            }
        })
    }

    fun setupToolbar(title: String) {
        binding.abTitle.text = title
    }

    private fun openAboutFragment() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            .replace(binding.fragmentContainer.id, AboutFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun openChangePasswordFragment() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            .replace(binding.fragmentContainer.id, ChangePasswordFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun checkAndSaveAudioFile(uri: Uri) {
        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val isValid = withContext(Dispatchers.IO) {
                AudioUtils.isValidAudioFile(this@SettingsActivity, uri)
            }
            if (isValid) {
                Log.d(TAG, "Audio is valid")
                when (currentSoundType) {
                    SoundType.ALARM -> binding.selectedAlarmSound.error = null
                    SoundType.FOCUS -> binding.selectedSessionSound.error = null
                    null -> Log.e(TAG, "check Uri failed with type: SoundType? is null")
                }
                saveSelectedUri(uri)
            } else {
                Log.e(TAG, "Invalid audio file")
                when (currentSoundType) {
                    SoundType.ALARM -> binding.selectedAlarmSound.error =
                        "Invalid audio file. Please select another!"

                    SoundType.FOCUS -> binding.selectedSessionSound.error =
                        "Invalid audio file. Please select another!"

                    null -> Log.e(TAG, "check Uri failed with type: SoundType? is null")
                }
            }
            // Hide loading
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun saveSelectedUri(uri: Uri) {
        when (currentSoundType) {
            SoundType.ALARM -> {
                sfUtils.updateSettingsDefaultAlarmSound(uri)
                binding.selectedAlarmSound.text =
                    AudioUtils.getAudioNameFromUri(
                        R.string.default_alarm_sound,
                        this@SettingsActivity,
                        uri
                    )
            }

            SoundType.FOCUS -> {
                sfUtils.updateSettingsDefaultSessionSound(uri)
                binding.selectedSessionSound.text =
                    AudioUtils.getAudioNameFromUri(
                        R.string.default_session_sound,
                        this@SettingsActivity,
                        uri
                    )
            }

            null -> {
                Log.e(TAG, "save Uri failed with type: SoundType? is null")
            }
        }
    }

    // Case choose ringtone
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        val uri: Uri? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            else
                @Suppress("DEPRECATION")
                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) ?: return
        if (uri == null)
            return
        checkAndSaveAudioFile(uri)
    }

    @SuppressLint("InflateParams")
    private fun showToolTip(content: String, view: View) {
        val popupView = layoutInflater.inflate(R.layout.layout_tooltip, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // Focusable: enables outside touch to dismiss
        )
        val tooltipText = popupView.findViewById<AppCompatTextView>(R.id.tooltipText)
        tooltipText.text = content
        popupWindow.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = 10f

        popupWindow.showAsDropDown(view, 0, 10)
    }

    //Listener implementation:
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
        Utils.showToast(
            this,
            getString(R.string.could_not_initiate_update_check, error.localizedMessage)
        )
    }

    override fun onUpdateFlowResultOk() {
        Log.i(TAG, "Update flow successful (RESULT_OK). Type: $updateTypeForSettings")
        Utils.showToast(this, getString(R.string.update_download_started))
        // For IMMEDIATE, app will likely restart soon. No Toast needed usually.
    }

    override fun onUpdateFlowResultCancelled() {
        Log.w(TAG, "Update flow cancelled by user. Type: $updateTypeForSettings")
        Utils.showToast(this, getString(R.string.update_canceled))
    }

    override fun onUpdateFlowResultFailed(resultCode: Int) {
        Log.e(TAG, "Update flow failed with result code: $resultCode. Type: $updateTypeForSettings")
        Utils.showToast(this, getString(R.string.update_failed_error, resultCode))
    }

    override fun onUpdateNotAvailable() {
        Utils.showToast(this, getString(R.string.no_update_available_at_this_moment))
    }
}