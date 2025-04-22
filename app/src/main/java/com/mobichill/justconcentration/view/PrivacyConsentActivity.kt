package com.mobichill.justconcentration.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityPrivacyConsentBinding
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.ACCEPTED_POLICY_KEY
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.APP_PREFS_NAME
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.util.Utils

class PrivacyConsentActivity : BaseViewBindingActivity<ActivityPrivacyConsentBinding>() {
    override fun initViewBinding(): ActivityPrivacyConsentBinding =
        ActivityPrivacyConsentBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable Continue button only if checkbox is checked
        binding.acceptCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.continueButton.isEnabled = isChecked
        }

        // Launch Privacy Policy (external link or internal screen)
        binding.viewPolicyButton.setOnClickListener (
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    Utils.openPrivacyPolicy(this@PrivacyConsentActivity)
                }
            }
        )

        // Save acceptance and go to main/login
        binding.continueButton.setOnClickListener (
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    val prefs = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE)
                    prefs.edit { putBoolean(ACCEPTED_POLICY_KEY, true) }
                    startActivity(Intent(this@PrivacyConsentActivity, WelcomeActivity::class.java))
                    finish()
                }
            }
        )
    }
}
