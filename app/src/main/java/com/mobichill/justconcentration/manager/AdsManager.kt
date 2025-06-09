package com.mobichill.justconcentration.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.mobichill.justconcentration.di.ApplicationCoroutineScope
import com.mobichill.justconcentration.repository.FirestoreRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseRepository: FirestoreRepository,
    @ApplicationCoroutineScope private val applicationScope: CoroutineScope
) {
    companion object {
        private val TAG = AdsManager::class.java.simpleName
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-9156563357147909/2804992230"
    }

    private var mInterstitialAd: InterstitialAd? = null
    private var isInterstitialLoading: Boolean = false

    init {
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "Mobile Ads SDK Initialized. Status: $initializationStatus")
        }
    }

    suspend fun loadAndShowBannerAd(adView: AdView) {
        if (firebaseRepository.isUserPro()) {
            adView.visibility = View.GONE
            return
        }

        adView.visibility = View.VISIBLE

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Banner Ad Loaded.")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(TAG, "Banner Ad Failed to Load: ${loadAdError.message}")
                adView.visibility = View.GONE // Hide if failed
            }
            // ... other listener methods (onAdOpened, onAdClicked, onAdClosed)
        }

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        Log.d(TAG, "Banner Ad load request initiated.")
    }

    suspend fun loadInterstitialAd() {
        if (firebaseRepository.isUserPro()) {
            Log.d(TAG, "User is Pro, not loading interstitial ad.")
            mInterstitialAd = null // Ensure it's cleared
            return
        }

        if (isInterstitialLoading || mInterstitialAd != null) {
            Log.d(TAG, "Interstitial ad already loaded or loading.")
            return
        }

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context, INTERSTITIAL_AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Interstitial Ad Loaded.")
                    mInterstitialAd = interstitialAd
                    isInterstitialLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Interstitial Ad Failed to Load: ${loadAdError.message}")
                    mInterstitialAd = null
                    isInterstitialLoading = false
                }
            })
    }

    fun showInterstitialAd(activity: Activity, onAdDismissed: (() -> Unit)? = null): Boolean {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial Ad Dismissed.")
                    mInterstitialAd = null // Ad can only be shown once
                    applicationScope.launch { loadInterstitialAd() } // Preload the next one
                    onAdDismissed?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Interstitial Ad Failed to Show: ${adError.message}")
                    mInterstitialAd = null
                    applicationScope.launch { loadInterstitialAd() }
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial Ad Showed.")
                }
            }
            mInterstitialAd?.show(activity)
            return true
        } else {
            Log.d(TAG, "Interstitial Ad not ready to show.")
            // Optionally try to load it now if not already loading
            if (!isInterstitialLoading) {
                applicationScope.launch { loadInterstitialAd() }
            }
            return false
        }
    }

    fun onPause(adView: AdView?) {
        adView?.pause()
        Log.d(TAG, "AdView paused.")
    }

    fun onResume(adView: AdView?) {
        adView?.resume()
        Log.d(TAG, "AdView resumed.")
    }

    fun onDestroy(adView: AdView?) {
        adView?.destroy()
        Log.d(TAG, "AdView destroyed.")
    }

}