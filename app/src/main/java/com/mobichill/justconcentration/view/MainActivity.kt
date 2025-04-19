package com.mobichill.justconcentration.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.util.Utils
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.ACCEPTED_POLICY_KEY
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.APP_PREFS_NAME

class MainActivity : AppCompatActivity() {
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
        val prefs = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE)
        val hasAccepted = prefs.getBoolean(ACCEPTED_POLICY_KEY, false)
        val skippedLogin = prefs.getBoolean(ACCEPTED_POLICY_KEY, false)

        // Check read policy first
        if (!hasAccepted) {
            startActivity(Intent(this, PrivacyConsentActivity::class.java))
        } else {
            // Check login state || User skipped login
            // Directly go to home page
            if (skippedLogin || Utils.isUserLoggedIn(this)) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                // User is not logged in || user has not skipped login, go to welcome page
                startActivity(Intent(this, WelcomeActivity::class.java))
            }
        }
        // Close SplashActivity to prevent going back to it
        finish()
    }
}