package com.mobichill.justconcentration.view

import android.os.CountDownTimer
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityForgotPasswordBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.others.Constants.OTHERS.EMAIL_REGEX

class ForgotPasswordActivity : BaseViewBindingActivity<ActivityForgotPasswordBinding>() {
    override fun initViewBinding(): ActivityForgotPasswordBinding =
        ActivityForgotPasswordBinding.inflate(layoutInflater)

    private var cooldownActive = false
    private val cooldownTimeMillis = 60_000L

    override fun initView() = with(binding) {
        super.initView()
        btnConfirm.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    if (!cooldownActive && validateEmail())
                        resetPassword()
                }
            })
    }

    private fun resetPassword() = with(binding) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(emailEditText.text.toString())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    tvStatus.text = getString(R.string.password_reset_email_sent)
                    tvStatus.setTextColor(getColor(R.color.white))
                    startCooldown()
                } else {
                    tvStatus.text =
                        getString(R.string.failed_reset_password, task.exception?.message)
                    tvStatus.setTextColor(getColor(R.color.red))
                }
            }
    }

    private fun startCooldown() = with(binding) {
        cooldownActive = true
        btnConfirm.isEnabled = false

        object : CountDownTimer(cooldownTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                btnConfirm.text = getString(R.string.wait_sec, secondsLeft)
            }

            override fun onFinish() {
                cooldownActive = false
                btnConfirm.isEnabled = true
                btnConfirm.text = getString(R.string.send_reset_email)
            }
        }.start()
    }

    private fun validateEmail(): Boolean = with(binding) {
        val email = emailEditText.text.toString()
        when {
            email.isEmpty() -> {
                emailEditText.requestFocus()
                emailEditText.error = getString(R.string.empty_email)
                return false
            }

            !EMAIL_REGEX.matcher(email).matches() -> {
                emailEditText.requestFocus()
                emailEditText.error = getString(R.string.wrong_format_email)
                return false
            }
        }
        return true
    }
}