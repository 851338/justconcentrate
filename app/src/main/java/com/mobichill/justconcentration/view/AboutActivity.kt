package com.mobichill.justconcentration.view

import android.content.Intent
import android.view.View
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.FragmentAboutBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.manager.BannerAdManager
import com.mobichill.justconcentration.utils.Utils

class AboutActivity : BaseViewBindingActivity<FragmentAboutBinding>() {

    override fun initViewBinding(): FragmentAboutBinding =
        FragmentAboutBinding.inflate(layoutInflater)

    override fun initData(intent: Intent?, isNewIntent: Boolean) {}

    override fun initView(): Unit = with(binding) {
        BannerAdManager.loadBanner(adView)
        binding.rateOnPlaystore.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    Utils.gotoStore(this@AboutActivity)
                }
            }
        )
        binding.privacyPolicy.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    Utils.openPrivacyPolicy(this@AboutActivity)
                }
            }
        )
        binding.viewLicenses.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    Utils.openLicensePage(this@AboutActivity)
                }
            }
        )
    }
}
