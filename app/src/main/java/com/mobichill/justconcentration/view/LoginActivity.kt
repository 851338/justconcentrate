package com.mobichill.justconcentration.view

import android.content.Intent
import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityLoginBinding
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.util.OnSingleClickListener
import com.mobichill.justconcentration.util.Utils

class LoginActivity : BaseViewBindingActivity<ActivityLoginBinding>() {
    override fun initViewBinding(): ActivityLoginBinding =
        ActivityLoginBinding.inflate(layoutInflater)

    private lateinit var auth: FirebaseAuth

    override fun initView() = with(binding) {
        auth = FirebaseAuth.getInstance()

        loginButton.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                val username = usernameEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()

                if (Utils.isNetworkAvailable(this@LoginActivity)) {
                    if (validateLoginInfo()) {
                        val email =
                            getString(R.string.app_email, username) // Mapping username to email
                        loginUser(email, password)
                    }
                } else
                    Utils.showToast(
                        this@LoginActivity,
                        getString(R.string.no_internet_connection)
                    )
            }
        })

        btnBack.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                onBackPressed()
            }
        })

        super.initView()
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        FireStoreRepository().fetchUserFromFireStore(this@LoginActivity, uid)
                        //fetch and save local
                    }
                    Utils.showToast(this, getString(R.string.login_success))
                    Log.d(TAG, getString(R.string.login_success))
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Utils.showToast(this, getString(R.string.login_failed, task.exception?.message))
                    Log.e(TAG, getString(R.string.login_failed, task.exception?.message))
                }
            }
    }

    private fun validateLoginInfo(): Boolean = with(binding) {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        when {
            username.isEmpty() -> {
                usernameEditText.requestFocus()
                usernameEditText.error = getString(R.string.empty_username)
                return false
            }

            password.isEmpty() -> {
                passwordEditText.requestFocus()
                passwordEditText.error = getString(R.string.empty_password)
                return false
            }
        }
        return true
    }
}