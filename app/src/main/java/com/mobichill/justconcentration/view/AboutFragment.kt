package com.mobichill.justconcentration.view

import android.view.View
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentAboutBinding
import com.mobichill.justconcentration.util.OnSingleClickListener
import com.mobichill.justconcentration.util.Utils

class AboutFragment : BaseViewBindingFragment<FragmentAboutBinding>() {

    override fun initViewBinding(): FragmentAboutBinding =
        FragmentAboutBinding.inflate(layoutInflater)

    override fun initData() {
    }

    override fun onResume() {
        super.onResume()
        if (activity is HomeActivity)
            (activity as HomeActivity).setupToolbar(getString(R.string.about_title), true)
    }

    override fun initView(): Unit = with(binding) {
        binding.rateOnPlaystore.setOnClickListener {
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    Utils.gotoStore(requireActivity())
                }
            }
        }
        binding.privacyPolicy.setOnClickListener {
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    Utils.openPrivacyPolicy(requireActivity())
                }
            }
        }
        binding.viewLicenses.setOnClickListener {
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    Utils.openLicensePage(requireActivity())
                }
            }
        }
    }
}