package com.mobichill.justconcentration.manager

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

object BannerAdManager {
    // Change this value to quickly swap banner ad unit id across the app.
    private var bannerAdUnitId: String = "ca-app-pub-3940256099942544/6300978111"

    fun updateBannerAdUnitId(adUnitId: String) {
        if (adUnitId.isBlank()) return
        bannerAdUnitId = adUnitId
    }

    fun loadBanner(adView: AdView) {
        adView.adUnitId = bannerAdUnitId
        adView.loadAd(AdRequest.Builder().build())
    }
}
