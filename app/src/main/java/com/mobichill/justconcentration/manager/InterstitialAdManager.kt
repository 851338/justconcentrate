package com.mobichill.justconcentration.manager

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdManager {
    //Test ID
//    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    //Product ID
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-9156563357147909/2804992230"

    private const val INTERSTITIAL_COOLDOWN_MS = 30_000L

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var isInitialized = false
    private var lastShownAtMs = 0L

    fun initialize(context: Context) {
        if (!isInitialized) {
            MobileAds.initialize(context)
            isInitialized = true
        }
        preload(context)
    }

    fun preload(context: Context) {
        if (interstitialAd != null || isLoading) return

        isLoading = true
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    fun showIfAvailable(activity: Activity, onComplete: () -> Unit) {
        if (!isCooldownComplete()) {
            onComplete()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            preload(activity.applicationContext)
            onComplete()
            return
        }

        interstitialAd = null
        var completed = false

        fun completeOnce() {
            if (completed) return
            completed = true
            onComplete()
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastShownAtMs = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                preload(activity.applicationContext)
                completeOnce()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                preload(activity.applicationContext)
                completeOnce()
            }
        }

        try {
            ad.show(activity)
        } catch (_: Exception) {
            preload(activity.applicationContext)
            completeOnce()
        }
    }

    private fun isCooldownComplete(): Boolean {
        if (lastShownAtMs == 0L) return true
        return SystemClock.elapsedRealtime() - lastShownAtMs >= INTERSTITIAL_COOLDOWN_MS
    }
}