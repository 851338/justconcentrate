package com.mobichill.justconcentration.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.view.language.LanguageActivity

class MainActivity : AppCompatActivity() {
    private val sfUtils: SharedPreferencesUtils by lazy {
        SharedPreferencesUtils(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //version 31 has splash API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }
        
        val pref = getSharedPreferences("data", MODE_PRIVATE)
        val hasSetLanguage = pref.contains("KEY_LANGUAGE")
        
        val hasAccepted = sfUtils.hasAcceptedPolicy()
        val skippedLogin = sfUtils.isSkippedLogin()
        
        if (!hasSetLanguage) {
            startActivity(Intent(this, LanguageActivity::class.java))
        } else if (!hasAccepted) {
            startActivity(Intent(this, PrivacyConsentActivity::class.java))
        } else {
            // Check login state  User skipped login
            // Directly go to home page
            if (skippedLogin || SharedPreferencesUtils(applicationContext).isUserLoggedIn()) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                // User is not logged in  user has not skipped login, go to welcome page
                startActivity(Intent(this, WelcomeActivity::class.java))
            }
        }
        // Close SplashActivity to prevent going back to it
        finish()
    }
}