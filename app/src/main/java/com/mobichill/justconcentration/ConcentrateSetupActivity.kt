package com.mobichill.justconcentration

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
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityConcentrateSetupBinding
import com.mobichill.justconcentration.service.FocusService
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.FOCUS_AUDIO_URI
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.FOCUS_DURATION
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.FOCUS_QUOTE
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.FOCUS_USER_GOAL
import com.mobichill.justconcentration.util.OnSingleClickListener
import com.mobichill.justconcentration.util.Utils
import com.mobichill.justconcentration.util.Utils.persistUriPermission
import com.mobichill.justconcentration.view.ConcentrationActivity
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
                            Utils.getAudioNameFromUri(this, audioUri)
                        selectedUri = audioUri
                        checkAudioFile(audioUri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            persistUriPermission(this, audioUri)
                        }
                    }
                }
            }
    }

    override fun initView() = with(binding) {
        super.initView()

        val presetTimes = listOf("5 minutes", "10 minutes", "30 minutes", "1 hour", "Custom...")

        val adapter = ArrayAdapter(
            this@ConcentrateSetupActivity,
            android.R.layout.simple_dropdown_item_1line,
            presetTimes
        )
        autoCompleteTxtDuration.setAdapter(adapter)

        // Handle selection
        autoCompleteTxtDuration.setOnItemClickListener { _, _, position, _ ->
            val selected = adapter.getItem(position)

            if (selected == "Custom...") {
                showCustomTimeDialog()
            } else {
                autoCompleteTxtDuration.setText(selected, false)
            }
            txtInputDuration.error = null
        }

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
    }

    fun showCustomTimeDialog() {
        val editText = AppCompatEditText(this).apply {
            hint = context.getString(R.string.duration_input_hint)
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Enter custom time")
            .setView(editText)
            .setPositiveButton("OK", null)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                val duration = editText.text.toString().toIntOrNull()
                if (duration == null || duration <= 0) {
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

    private fun checkAudioFile(uri: Uri) {
        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val isValid = Utils.isValidAudioFile(this@ConcentrateSetupActivity, uri)
            // Hide loading
            binding.progressBar.visibility = View.GONE
            if (isValid) {
                // Proceed with valid file
                Log.d(TAG, "Audio is valid")
                binding.tvSelectedSound.error = null
            } else {
                binding.tvSelectedSound.error = "Invalid audio file. Please select another!"
                Log.e(TAG, "Invalid audio file")
            }
        }
    }

    fun startFocusSession(duration: Int, goal: String, audioUri: String?) {
        // Start foreground service with timer & sound
        val quote = ConcentrationQuotes.getRandomQuote()
        val intent = Intent(this, FocusService::class.java)
        intent.putExtra(FOCUS_AUDIO_URI, audioUri)
        intent.putExtra(FOCUS_DURATION, duration)
        intent.putExtra(FOCUS_USER_GOAL, goal)
        intent.putExtra(FOCUS_QUOTE, quote)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        openConcentrationActivity(quote)
    }

    private fun openConcentrationActivity(quote: String) {
        val intent = Intent(this, ConcentrationActivity::class.java)
        intent.putExtra(FOCUS_QUOTE, quote)
        startActivity(intent)
    }
}