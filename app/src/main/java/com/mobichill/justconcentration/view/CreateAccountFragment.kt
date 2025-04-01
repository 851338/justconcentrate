package com.mobichill.justconcentration.view

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentCreateAccountBinding
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.util.Utils
import java.util.UUID

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
        binding.btnSignup.setOnClickListener {
            if (Utils.isNetworkAvailable(requireContext())) {
                if (validateRegInfo())             //check and create
                    checkUsernameAvailability(etUsername.toString(), etPassword.toString())
            } else Utils.showToast(
                requireContext(),
                getString(R.string.no_internet_connection)
            )
        }
    }

    private fun createAccount(username: String, password: String) {
        val userId = UUID.randomUUID().toString() // Generate a unique user ID
        // Store hashed password (Never store raw password)
        val hashedPassword = password.hashCode().toString() // Basic password hashing
        FireStoreRepository().addNewUserBySigningUp(
            requireContext(),
            userId,
            username,
            hashedPassword
        )
    }

    private fun validateRegInfo(): Boolean = with(binding) {
        val username = etUsername.text.toString()
        val password = etPassword.text.toString()
        val rePassword = etReenterPassword.text.toString()

        when {
            username.isEmpty() -> {
                etUsername.requestFocus()
                etUsername.error = getString(R.string.empty_username)
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

    private fun checkUsernameAvailability(username: String, password: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").whereEqualTo("username", username).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Username is available, proceed with signup
                    createAccount(username, password)
                } else {
                    Utils.showToast(requireContext(), getString(R.string.username_already_taken))
                }
            }
            .addOnFailureListener {
                Utils.showToast(
                    requireContext(),
                    getString(R.string.error_checking_username, it.message)
                )
            }
    }
}
