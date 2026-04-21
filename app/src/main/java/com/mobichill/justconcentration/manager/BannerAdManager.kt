package com.mobichill.justconcentration.manager

import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

object BannerAdManager {
    private const val TAG = "BannerAdManager"

    //Test ID
//    private var bannerAdUnitId: String = "ca-app-pub-3940256099942544/6300978111"
    //Product ID
    private var bannerAdUnitId: String = "ca-app-pub-9156563357147909/6188577744"


    fun updateBannerAdUnitId(adUnitId: String) {
        if (adUnitId.isBlank()) return
        bannerAdUnitId = adUnitId
    }

    fun loadBanner(adView: AdView) {
        val existingAdUnitId = adView.adUnitId
        if (existingAdUnitId.isBlank()) {
            adView.adUnitId = bannerAdUnitId
        } else if (existingAdUnitId != bannerAdUnitId) {
            // Avoid IllegalStateException when an AdView already has adUnitId from XML.
            Log.w(TAG, "AdView already has adUnitId from layout; skip override.")
        }
        adView.loadAd(AdRequest.Builder().build())
    }
}
