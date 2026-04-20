package com.mobichill.justconcentration.view.popup

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseDialog
import com.mobichill.justconcentration.databinding.DialogLoadingBinding

class LoadingDialog(
    context: Context,
    private val isLanguageActivity: Boolean,
    private val title: String? = null
) :
    BaseDialog<DialogLoadingBinding>(context) {
    override fun setBinding(layoutInflater: LayoutInflater) =
        DialogLoadingBinding.inflate(layoutInflater)

    override fun initView() {
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setGravity(Gravity.CENTER)
        Handler(Looper.getMainLooper()).postDelayed({ dismiss() }, 1000)
        if (isLanguageActivity) {
            binding.tvTitle.text = context.getString(R.string.translating)
            binding.tvDescription.visibility = View.VISIBLE
        } else {
            binding.tvDescription.visibility = View.GONE
        }
    }

    override fun listener() {
        if (title != null) {
            binding.tvTitle.text = title
        }
    }
}