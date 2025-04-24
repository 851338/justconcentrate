package com.mobichill.justconcentration.view

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
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
        // Setup view
        selectedAlarmSound.text = SFUtils.getDefaultAlarmSound(this@SettingsActivity)
        selectedSessionSound.text = SFUtils.getDefaultSessionSound(this@SettingsActivity)

        itemSyncWithCloud.settingToggleTitle.text = getString(R.string.sync_with_cloud)
        itemChangePassword.settingTitle.text = getString(R.string.change_password)

        itemVibrationSwitch.settingToggleTitle.text = getString(R.string.vibration)
        itemReminderTime.settingTitle.text = getString(R.string.default_alarm_time)
        itemAlarmSound.settingTitle.text = getString(R.string.default_alarm_sound)
        itemSessionSound.settingTitle.text = getString(R.string.default_session_sound)

        itemDarkMode.settingToggleTitle.text = getString(R.string.dark_mode_text)
        itemAbout.settingTitle.text = getString(R.string.about)

        // Back button
        btnBack.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                onBackPressed()
            }
        })

        // Sync with cloud switch
        val isSynced = prefs.getBoolean(SETTINGS_SYNC_KEY, false)
        binding.itemSyncWithCloud.settingToggleSwitch.isChecked = isSynced
        itemSyncWithCloud.settingToggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(SETTINGS_SYNC_KEY, isChecked) }
        }

        // Change password button
        //TODO

        // Vibration switch
        val vibrationEnabled = prefs.getBoolean(SETTINGS_VIBRATION_KEY, false)
        binding.itemVibrationSwitch.settingToggleSwitch.isChecked = vibrationEnabled
        itemVibrationSwitch.settingToggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(SETTINGS_VIBRATION_KEY, isChecked) }
        }

        // Default alarm time

        // Default alarm sound
        binding.itemAlarmSound.root.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    currentSoundType = SoundType.ALARM
                    AudioUtils.showSoundChoiceDialog(this@SettingsActivity, pickAudioLauncher)
                }
            }
        )

        //Default session sound
        binding.itemSessionSound.root.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    currentSoundType = SoundType.FOCUS
                    AudioUtils.showSoundChoiceDialog(this@SettingsActivity, pickAudioLauncher)
                }
            }
        )

        // Dark mode
        val darkModeEnabled = prefs.getBoolean(SETTINGS_DARK_MODE_KEY, false)
        binding.itemDarkMode.settingToggleSwitch.isChecked = darkModeEnabled
        itemDarkMode.settingToggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(SETTINGS_DARK_MODE_KEY, isChecked) }
        }

        // About button
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

    override fun onBackPressed() {
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when (current) {
            is AboutFragment -> {
                onBackPressedDispatcher.onBackPressed()
                setupToolbar(getString(R.string.settings))
            }

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
}