package com.mobichill.justconcentration.view

import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityLoginBinding
import com.mobichill.justconcentration.util.Utils

class LoginActivity : BaseViewBindingActivity<ActivityLoginBinding>() {
    override fun initViewBinding(): ActivityLoginBinding =
        ActivityLoginBinding.inflate(layoutInflater)

    private lateinit var auth: FirebaseAuth

    override fun initView() = with(binding) {
        auth = FirebaseAuth.getInstance()

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (Utils.isNetworkAvailable(this@LoginActivity)) {
                if (validateLoginInfo()) {
                    val email = getString(R.string.app_email, username) // Mapping username to email
                    loginUser(email, password)
                }
            } else
                Utils.showToast(
                    this@LoginActivity,
                    getString(R.string.no_internet_connection)
                )

        }

        btnBack.setOnClickListener { onBackPressed() }

        super.initView()
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    //save username to sf
                    Utils.saveUserNameAfterLogin(this, Utils.getUsernameFromEmail(email))
                    Utils.showToast(this, getString(R.string.login_success))
                    Utils.saveLoginState(this, true)
                    //open home after login
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Utils.showToast(
                        this, getString(R.string.login_failed, task.exception?.message)
                    )
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