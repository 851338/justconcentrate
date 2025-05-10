package com.mobichill.justconcentration.view

import android.view.View
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentAboutBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.utils.Utils

class AboutFragment : BaseViewBindingFragment<FragmentAboutBinding>() {

    override fun initViewBinding(): FragmentAboutBinding =
        FragmentAboutBinding.inflate(layoutInflater)

    override fun initData() {}

    override fun onResume() {
        super.onResume()
        if (activity is SettingsActivity)
            (activity as SettingsActivity).setupToolbar(getString(R.string.about_title))
    }

    override fun initView(): Unit = with(binding) {
        appVersion.text = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName

        rateOnPlaystore.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    Utils.gotoStore(requireActivity())
                }
            }
        )
        privacyPolicy.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    Utils.openPrivacyPolicy(requireActivity())
                }
            }
        )
        viewLicenses.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    Utils.openLicensePage(requireActivity())
                }
            }
        )
    }
}