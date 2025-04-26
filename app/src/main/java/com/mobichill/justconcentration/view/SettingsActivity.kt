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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivitySettingsBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.SETTINGS_DARK_MODE_KEY
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.SETTINGS_DEFAULT_ALARM_KEY
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.SETTINGS_PREFS_NAME
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.SETTINGS_SYNC_KEY
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.SETTINGS_VIBRATION_KEY
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.SETTING_DEFAULT_SESSION_SOUND_KEY
import com.mobichill.justconcentration.util.AudioUtils
import com.mobichill.justconcentration.util.SFUtils
import com.mobichill.justconcentration.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : BaseViewBindingActivity<ActivitySettingsBinding>() {
    private enum class SoundType {
        ALARM, FOCUS
    }

    private var currentSoundType: SoundType? = null
    private val prefs by lazy {
        getSharedPreferences(SETTINGS_PREFS_NAME, MODE_PRIVATE)
    }
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
        super.onCreate(savedInstanceState)
    }

    override fun initView() = with(binding) {
        super.initView()
        // Back button
        btnBack.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                onBackPressed()
            }
        })

        // Sync with cloud switch
        itemSyncWithCloud.settingToggleTitle.text = getString(R.string.sync_with_cloud)
        val isSynced = prefs.getBoolean(SETTINGS_SYNC_KEY, false)
        itemSyncWithCloud.settingToggleSwitch.isChecked = isSynced
        itemSyncWithCloud.settingToggleSwitch.setOnCheckedChangeListener { switch, isChecked ->
            switch.isEnabled = false
            if (SFUtils.isUserLoggedIn(this@SettingsActivity)) {
                prefs.edit { putBoolean(SETTINGS_SYNC_KEY, isChecked) }
                Handler(Looper.getMainLooper()).postDelayed({
                    switch.isEnabled = true
                }, 1000)
            } else Utils.showToast(this@SettingsActivity, getString(R.string.you_must_log_in_first))
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
                    if (!SFUtils.isUserLoggedIn(this@SettingsActivity)) {
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
        val vibrationEnabled = prefs.getBoolean(SETTINGS_VIBRATION_KEY, false)
        itemVibrationSwitch.settingToggleSwitch.isChecked = vibrationEnabled
        itemVibrationSwitch.settingToggleSwitch.setOnCheckedChangeListener { switch, isChecked ->
            switch.isEnabled = false
            prefs.edit { putBoolean(SETTINGS_VIBRATION_KEY, isChecked) }
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

        // Dark mode
        itemDarkMode.settingToggleTitle.text = getString(R.string.dark_mode_text)
        val darkModeEnabled = prefs.getBoolean(SETTINGS_DARK_MODE_KEY, false)
        itemDarkMode.settingToggleSwitch.isChecked = darkModeEnabled
        itemDarkMode.settingToggleSwitch.setOnCheckedChangeListener { switch, isChecked ->
            switch.isEnabled = false
            prefs.edit { putBoolean(SETTINGS_DARK_MODE_KEY, isChecked) }
            Handler(Looper.getMainLooper()).postDelayed({
                switch.isEnabled = true
            }, 1000)
        }

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

    override fun onBackPressed() {
        setupToolbar(getString(R.string.settings))
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when (current) {
            is AboutFragment, is ChangePasswordFragment ->
                onBackPressedDispatcher.onBackPressed()

            else ->
                super.onBackPressed()
        }
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
                prefs.edit { putString(SETTINGS_DEFAULT_ALARM_KEY, uri.toString()) }
                binding.selectedAlarmSound.text =
                    AudioUtils.getAudioNameFromUri(
                        R.string.default_alarm_sound,
                        this@SettingsActivity,
                        uri
                    )
            }

            SoundType.FOCUS -> {
                prefs.edit { putString(SETTING_DEFAULT_SESSION_SOUND_KEY, uri.toString()) }
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
        val popupView = layoutInflater.inflate(R.layout.tooltip_layout, null)

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
}