package com.mobichill.justconcentration.view

import android.content.Intent
import android.view.View
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentChangePasswordBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.utils.Utils

class ChangePasswordFragment : BaseViewBindingFragment<FragmentChangePasswordBinding>() {
    override fun initViewBinding(): FragmentChangePasswordBinding =
        FragmentChangePasswordBinding.inflate(layoutInflater)

    override fun onResume() {
        super.onResume()
        if (activity is SettingsActivity)
            (activity as SettingsActivity).setupToolbar(getString(R.string.change_password))
    }

    override fun initData() {}

    override fun initView() = with(binding) {
        btnConfirm.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    if (validation())
                        changePassword()
                }
            })

        forgotPassword.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                openForgotPasswordActivity()
            }
        })
    }

    private fun openForgotPasswordActivity() {
        startActivity(Intent(requireActivity(), ForgotPasswordActivity::class.java))
    }

    private fun changePassword() = with(binding) {
        val currentPassword = etCurrentPassword.text.toString()
        val newPassword = etNewPassword.text.toString()
        val user = FirebaseAuth.getInstance().currentUser

        if (user?.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

            // Re-authenticate user first (Firebase require)
            user.reauthenticate(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        // Change password
                        user.updatePassword(newPassword)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Utils.showToast(
                                        requireContext(),
                                        getString(R.string.password_changed)
                                    )
                                } else {
                                    Utils.showToast(
                                        requireContext(),
                                        getString(R.string.password_change_failed)
                                    )
                                }
                            }
                    } else {
                        Utils.showToast(
                            requireContext(),
                            getString(R.string.current_password_incorrect)
                        )
                        etCurrentPassword.error = getString(R.string.current_password_incorrect)

                    }
                }
        }
    }

    private fun validation(): Boolean = with(binding) {
        val currentPassword = etCurrentPassword.text.toString()
        val newPassword = etNewPassword.text.toString()
        val reNewPassword = etReenterNewPassword.text.toString()

        when {
            currentPassword.isEmpty() -> {
                etCurrentPassword.requestFocus()
                etCurrentPassword.error = getString(R.string.empty_name)
                return false
            }

            newPassword.length < 6 -> {
                etNewPassword.requestFocus()
                etNewPassword.error = getString(R.string.password_min_6_characters_error)
                return false
            }

            reNewPassword != newPassword -> {
                etReenterNewPassword.requestFocus()
                etReenterNewPassword.error = getString(R.string.confirm_password_mismatch)
                return false
            }
        }
        return true
    }
}