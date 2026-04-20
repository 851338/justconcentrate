package com.mobichill.justconcentration.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityPrivacyConsentBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.manager.BannerAdManager
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils

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

        BannerAdManager.loadBanner(binding.adView)

        // Save acceptance and go to main/login
        binding.continueButton.setOnClickListener (
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    SharedPreferencesUtils(applicationContext).setAcceptedPolicy(true)
                    startActivity(Intent(this@PrivacyConsentActivity, WelcomeActivity::class.java))
                    finish()
                }
            }
        )
    }
}
