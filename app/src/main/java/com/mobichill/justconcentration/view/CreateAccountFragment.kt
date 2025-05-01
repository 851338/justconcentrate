package com.mobichill.justconcentration.view

import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentCreateAccountBinding
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.constants.Constants.OTHERS.EMAIL_REGEX
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils

class CreateAccountFragment : BaseViewBindingFragment<FragmentCreateAccountBinding>() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun initViewBinding(): FragmentCreateAccountBinding =
        FragmentCreateAccountBinding.inflate(layoutInflater)

    override fun initData() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun initView() = with(binding) {
        binding.btnSignup.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                if (Utils.isNetworkAvailable(requireContext())) {
                    if (validateRegInfo())             //check and create
                        createAccount(
                            etEmail.text.toString(),
                            etName.text.toString(),
                            etPassword.text.toString()
                        )
                } else Utils.showToast(
                    requireContext(),
                    getString(R.string.no_internet_connection)
                )
            }
        })
    }

    private fun validateRegInfo(): Boolean = with(binding) {
        val email = etEmail.text.toString()
        val name = etName.text.toString()
        val password = etPassword.text.toString()
        val rePassword = etReenterPassword.text.toString()

        when {
            email.isEmpty() -> {
                etEmail.requestFocus()
                etEmail.error = getString(R.string.empty_email)
                return false
            }

            !EMAIL_REGEX.matcher(email).matches() -> {
                etEmail.requestFocus()
                etEmail.error = getString(R.string.wrong_format_email)
                return false
            }

            name.isEmpty() -> {
                etName.requestFocus()
                etName.error = getString(R.string.empty_name)
                return false
            }

            password.length < 6 -> {
                etPassword.requestFocus()
                etPassword.error = getString(R.string.password_min_6_characters_error)
                return false
            }

            rePassword != password -> {
                etReenterPassword.requestFocus()
                etReenterPassword.error = getString(R.string.confirm_password_mismatch)
                return false
            }
        }
        return true
    }

    private fun createAccount(email: String, name: String, password: String) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val uid = firebaseUser?.uid
                    //Save user data to FireStore and Room
                    if (uid != null) {
                        FireStoreRepository().addNewUserBySigningUp(
                            requireContext(),
                            uid,
                            email,
                            name
                        )
                        //save shared preferences
                        SharedPreferencesUtils(requireContext()).saveUserInfoToSF(uid)
                    }
                } else {
                    Utils.showToast(requireContext(), "Signup failed: ${task.exception?.message}")
                }
            }
    }

}
