package com.mobichill.justconcentration.view

import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.constants.ConcentrationQuotes
import com.mobichill.justconcentration.constants.Constants
import com.mobichill.justconcentration.databinding.ActivityConcentrateSetupBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.service.FocusService
import com.mobichill.justconcentration.utils.AudioUtils
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils
import kotlinx.coroutines.launch

class ConcentrateSetupActivity : BaseViewBindingActivity<ActivityConcentrateSetupBinding>() {
    private lateinit var pickAudioLauncher: ActivityResultLauncher<Intent>
    private var selectedUri: Uri? = null
    private var selectedDuration: Int = 0
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
    }

    override fun initView() = with(binding) {
        super.initView()
        // Setup view
        binding.tvSelectedSound.text = AudioUtils.defaultSessionName(this@ConcentrateSetupActivity)
        selectedUri = AudioUtils.defaultSessionUri(this@ConcentrateSetupActivity)
        setupSpinner()

        //Setup onClick
        btnBack.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                onBackPressed()
            }
        })

        buttonSelectSound.setOnClickListener(
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

        startConcentrateButton.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    if (selectedDuration != 0)
                        startFocusSession(
                            selectedDuration,
                            edtGoal.text.toString(),
                            selectedUri?.toString()
                        )
                    else txtInputDuration.error =
                        getString(R.string.you_haven_t_determined_duration)
                }
            }
        )

        //run ads
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    fun showCustomTimeDialog() {
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
                selectedDuration = selected?.split(" ")[0]?.toInt() ?: 0
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

    fun startFocusSession(duration: Int, goal: String, audioUri: String?) {
        val isActive = SharedPreferencesUtils(applicationContext).isFocusSessionActive()
        //Shared preference: Imagine the service crashes but the flag still says "active" — you'd block new sessions forever.
        //is Running: Some OEMs aggressively kill services in the background without notice.
        //So we use both
        if (isActive &&
            FocusService.Companion.isRunning
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

}