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
        // Check login state

        if (Utils.isUserLoggedIn(this)) {
            // User is logged in, go to HomeActivity
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // User is NOT logged in, go to LoginActivity
            startActivity(Intent(this, WelcomeActivity::class.java))
        }

        // Close SplashActivity to prevent going back to it
        finish()
    }
}