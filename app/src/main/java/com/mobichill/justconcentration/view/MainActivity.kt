package com.mobichill.justconcentration.view

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Outline
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityMainBinding
import com.mobichill.justconcentration.manager.NativeAdManager
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.view.language.LanguageActivity

class MainActivity : BaseViewBindingActivity<ActivityMainBinding>() {
    private var index = 0
    private val handler = Handler(Looper.getMainLooper())
    private var loadedNativeAd: NativeAd? = null
    private val sfUtils: SharedPreferencesUtils by lazy {
        SharedPreferencesUtils(applicationContext)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun initViewBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        super.initView()
        AnimationUtils.loadAnimation(this, R.anim.indeterminate_anim)
        loadBottomNativeAd()

        // Progress bar setup (ViewOutlineProvider works for API 21+, clipToOutline only works for API 31+)
        binding.progressBar.post {
            val radius = resources.getDimensionPixelSize(R.dimen.progress_corner_radius).toFloat()
            binding.progressBar.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radius)
                }
            }
            binding.progressBar.clipToOutline = true

            // Sliding animation
            val startX = -binding.loopingSegment.width.toFloat()
            val endX = binding.progressBar.width.toFloat()
            val animator = ObjectAnimator.ofFloat(binding.loopingSegment, View.TRANSLATION_X, startX, endX)
            animator.duration = 1200L
            animator.repeatCount = ValueAnimator.INFINITE
            animator.interpolator = LinearInterpolator()
            animator.start()
        }
        showNextText()

        // Keep splash timing, then route based on language/policy/login state.
        Handler(Looper.getMainLooper()).postDelayed({
            val pref = getSharedPreferences("data", MODE_PRIVATE)
            val hasSetLanguage = pref.contains("KEY_LANGUAGE")
            val hasAccepted = sfUtils.hasAcceptedPolicy()
            val skippedLogin = sfUtils.isSkippedLogin()

            if (!hasSetLanguage) {
                startActivity(Intent(this, LanguageActivity::class.java))
            } else if (!hasAccepted) {
                startActivity(Intent(this, PrivacyConsentActivity::class.java))
            } else {
                if (skippedLogin || SharedPreferencesUtils(applicationContext).isUserLoggedIn()) {
                    startActivity(Intent(this, HomeActivity::class.java))
                } else {
                    startActivity(Intent(this, WelcomeActivity::class.java))
                }
            }
            finish()
        }, 6000)
    }

    private fun loadBottomNativeAd() {
        val nativeAdView = binding.nativeAdContainer.root as? NativeAdView ?: return
        NativeAdManager.loadNativeAd(
            context = this,
            nativeAdView = nativeAdView,
            onLoaded = { nativeAd ->
                loadedNativeAd?.destroy()
                loadedNativeAd = nativeAd
            },
            onFailed = {
                loadedNativeAd?.destroy()
                loadedNativeAd = null
            }
        )
    }

    private fun showNextText() {
        val texts = resources.getStringArray(R.array.splash_texts)

        if (index < texts.size) {
            binding.splashText.text = texts[index]
            index++
            handler.postDelayed({ showNextText() }, 300) // 0.3s
        }
    }

    override fun onDestroy() {
        loadedNativeAd?.destroy()
        loadedNativeAd = null
        super.onDestroy()
    }
}