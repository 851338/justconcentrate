package com.mobichill.justconcentration.view

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.constants.ConcentrationQuotes
import com.mobichill.justconcentration.constants.Constants
import com.mobichill.justconcentration.databinding.ActivityConcentrateSetupBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.manager.AdsManager
import com.mobichill.justconcentration.service.FocusService
import com.mobichill.justconcentration.utils.AudioUtils
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConcentrateSetupActivity : BaseViewBindingActivity<ActivityConcentrateSetupBinding>() {
    private lateinit var pickAudioLauncher: ActivityResultLauncher<Intent>

    @Inject
    lateinit var adsManager: AdsManager

    private var selectedUri: Uri? = null
    private var selectedDuration: Int = 0
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun initViewBinding(): ActivityConcentrateSetupBinding =
        ActivityConcentrateSetupBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickAudioLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val audioUri: Uri? = result.data?.data
                    if (audioUri != null) {
                        binding.buttonSelectSound.text =
                            AudioUtils.getAudioNameFromUri(
                                R.string.unknown_audio_file,
                                this,
                                audioUri
                            )
                        selectedUri = audioUri
                        checkAudioFile(audioUri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            AudioUtils.persistUriPermission(this, audioUri)
                        }
                    }
                }
            }
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Notifications permission granted! Starting session.")
                startFocusSessionWithValidation()
            } else {
                Log.e(TAG, "User denied notifications.")
                Utils.showToast(this, getString(R.string.notification_required))
            }
        }
    }

    override fun initView() {
        super.initView()
        // Setup view
        binding.tvSelectedSound.text = AudioUtils.defaultSessionName(this@ConcentrateSetupActivity)
        selectedUri = AudioUtils.defaultSessionUri(this@ConcentrateSetupActivity)
        setupSpinner()

        //Setup onClick
        binding.btnBack.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                onBackPressedDispatcher.onBackPressed()
            }
        })

        binding.buttonSelectSound.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        type = "audio/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    pickAudioLauncher.launch(intent)
                }
            }
        )

        binding.startConcentrateButton.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    requestPermissionAndStartSession()
                }
            }
        )

        // Setup Ads
        lifecycleScope.launch {
            adsManager.loadAndShowBannerAd(binding.adView)
        }
    }

    private fun showCustomTimeDialog() {
        val editText = AppCompatEditText(this).apply {
            hint = context.getString(R.string.duration_input_hint)
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.enter_custom_time))
            .setView(editText)
            .setPositiveButton("OK", null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialog.setOnShowListener {
            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                val duration = editText.text.toString().toIntOrNull()
                if (duration == null || duration <= 0 || duration >= 1440) {
                    editText.error = getString(R.string.really)
                    editText.requestFocus()
                } else {
                    binding.autoCompleteTxtDuration.setText(editText.text.toString(), false)
                    selectedDuration = editText.text.toString().toInt()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun setupSpinner() = with(binding) {
        val presetTimes = listOf(
            getString(R.string._5_minutes),
            getString(R.string._10_minutes),
            getString(R.string._30_minutes),
            getString(R.string._60_minutes),
            getString(R.string.custom)
        )

        val adapter = ArrayAdapter(
            this@ConcentrateSetupActivity,
            android.R.layout.simple_dropdown_item_1line,
            presetTimes
        )
        autoCompleteTxtDuration.setAdapter(adapter)

        autoCompleteTxtDuration.setOnItemClickListener { _, _, position, _ ->
            val selected = adapter.getItem(position)
            if (selected == getString(R.string.custom)) {
                showCustomTimeDialog()
            } else {
                autoCompleteTxtDuration.setText(selected, false)
                selectedDuration = selected?.split(" ")?.get(0)?.toInt() ?: 0
            }
            txtInputDuration.error = null
        }
    }

    private fun checkAudioFile(uri: Uri) {
        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val isValid = AudioUtils.isValidAudioFile(this@ConcentrateSetupActivity, uri)
            // Hide loading
            binding.progressBar.visibility = View.GONE
            if (isValid) {
                // Proceed with valid file
                Log.d(TAG, "Audio is valid")
                binding.tvSelectedSound.error = null
            } else {
                binding.tvSelectedSound.error = getString(R.string.invalid_audio)
                Log.e(TAG, "Invalid audio file")
            }
        }
    }

    private fun requestPermissionAndStartSession() {
        // First, validate that a duration has been selected
        if (selectedDuration == 0) {
            binding.txtInputDuration.error = getString(R.string.you_haven_t_determined_duration)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                // Permission is already granted, proceed to start the service
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Permission already granted. Starting session.")
                    startFocusSessionWithValidation()
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show a dialog explaining why you need the permission
                    // For now, we'll just request it directly.
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                // Request the permission
                else -> {
                    Log.d(TAG, "Requesting notifications permission.")
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // No runtime permission needed for older Android versions
            Log.d(TAG, "Not on Android 13+, starting session directly.")
            startFocusSessionWithValidation()
        }
    }

    private fun startFocusSessionWithValidation() {
        if (selectedDuration == 0) {
            binding.txtInputDuration.error = getString(R.string.you_haven_t_determined_duration)
            return
        }
        startFocusSession(
            selectedDuration,
            binding.edtGoal.text.toString(),
            selectedUri?.toString()
        )
    }

    fun startFocusSession(duration: Int, goal: String, audioUri: String?) {
        val isActive = SharedPreferencesUtils(applicationContext).isFocusSessionActive()
        //Some OEMs aggressively kill services in the background without notice.
        if (isActive &&
            FocusService.isRunning
        ) {
            Utils.showToast(this, getString(R.string.focus_session_already_running))
        } else {
            // Start foreground service with timer & sound
            val quote = ConcentrationQuotes.getRandomQuote()
            val intent = Intent(this, FocusService::class.java)
            intent.action = Constants.OTHERS.ACTION_START_SESSION
            intent.putExtra(Constants.INTENT_EXTRA.FOCUS_AUDIO_URI, audioUri)
            intent.putExtra(Constants.INTENT_EXTRA.FOCUS_DURATION, duration)
            intent.putExtra(Constants.INTENT_EXTRA.FOCUS_USER_GOAL, goal)
            intent.putExtra(Constants.INTENT_EXTRA.FOCUS_QUOTE, quote)

            startForegroundService(intent)
            openConcentrationActivity(quote)
        }
    }

    private fun openConcentrationActivity(quote: String) {
        val intent = Intent(this, ConcentrationActivity::class.java)
        intent.putExtra(Constants.INTENT_EXTRA.FOCUS_QUOTE, quote)
        startActivity(intent)
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