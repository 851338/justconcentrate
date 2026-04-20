package com.mobichill.justconcentration.view

import android.os.Bundle
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityConcentrationBinding
import com.mobichill.justconcentration.constants.Constants.INTENT_EXTRA.FOCUS_QUOTE
import com.mobichill.justconcentration.manager.BannerAdManager

class ConcentrationActivity : BaseViewBindingActivity<ActivityConcentrationBinding>() {
    override fun initViewBinding(): ActivityConcentrationBinding =
        ActivityConcentrationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BannerAdManager.loadBanner(binding.adView)
        val quote = intent.getStringExtra(FOCUS_QUOTE)
        binding.tvQuote.apply {
            text = quote
            alpha = 0f
            animate().alpha(1f).setDuration(600).start()
        }
    }
}