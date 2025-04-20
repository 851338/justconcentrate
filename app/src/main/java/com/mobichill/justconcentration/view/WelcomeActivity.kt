package com.mobichill.justconcentration.view

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityWelcomeBinding
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.util.Utils
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import androidx.core.content.edit
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.APP_PREFS_NAME
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.SKIPPED_LOGIN_KEY

class WelcomeActivity : BaseViewBindingActivity<ActivityWelcomeBinding>() {
    override fun initViewBinding(): ActivityWelcomeBinding =
        ActivityWelcomeBinding.inflate(layoutInflater)

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun initView() {
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        binding.btnGoogleSignIn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                if (Utils.isNetworkAvailable(this@WelcomeActivity))
                    lifecycleScope.launch { signInWithGoogle() }
                else Utils.showToast(
                    this@WelcomeActivity,
                    getString(R.string.no_internet_connection)
                )
            }
        })
        binding.btnCreateAccount.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                gotoCreateAccountFragment()
            }
        })
        binding.txtAlreadyHaveAccount.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                gotoLoginActivity()
            }
        })
        binding.txtSkip.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                val sharedPref = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE)
                sharedPref.edit {
                    putBoolean(SKIPPED_LOGIN_KEY, true)
                }
                goToHomeActivity()
            }
        })
        super.initView()
    }

    private fun gotoLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun goToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun gotoCreateAccountFragment() {
        val fragment = CreateAccountFragment().apply {
        }
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .addToBackStack(null)
            .commit()
    }

    private suspend fun signInWithGoogle() {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        //create googleIdOption
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setNonce(hashedNonce)
            .build()

        //create request using ggIdOption
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        coroutineScope {
            try {
                val result = credentialManager.getCredential(this@WelcomeActivity, request)
                val credential = result.credential

                //should get googleIdToken by this
                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken
                Log.i(TAG, googleIdToken)
                firebaseAuthWithGoogle(googleIdToken)
            } catch (e: GetCredentialException) {
                Log.e(TAG, e.errorMessage.toString())
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, e.localizedMessage ?: getString(R.string.unknown_exception))
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            //save user to firestore checking existence
                            FireStoreRepository().checkIfUserExists(
                                user.uid,
                                user.displayName,
                                user.email,
                                user.photoUrl.toString()
                            )
                            //save shared preferences
                            Utils.saveUserInfoToSF(this,user.uid)
                        }
                        //show toast & open main activity
                        Utils.showToast(this, getString(R.string.sign_in_successful))
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        Utils.showToast(this, getString(R.string.authentication_failed))
                    }
                }
            }
    }
}

