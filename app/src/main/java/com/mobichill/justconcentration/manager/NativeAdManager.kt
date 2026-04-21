package com.mobichill.justconcentration.manager

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MediaContent
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.mobichill.justconcentration.R

object NativeAdManager {
    //Test ID
//    private var nativeAdUnitId: String = "ca-app-pub-3940256099942544/2247696110"
    //Product ID
    private var nativeAdUnitId: String = "ca-app-pub-9156563357147909/7016421408"


    fun updateNativeAdUnitId(adUnitId: String) {
        if (adUnitId.isBlank()) return
        nativeAdUnitId = adUnitId
    }

    fun loadNativeAd(
        context: Context,
        nativeAdView: NativeAdView,
        onLoaded: (NativeAd) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val adLoader = AdLoader.Builder(context, nativeAdUnitId)
            .forNativeAd { nativeAd ->
                populateNativeAdView(nativeAd, nativeAdView)
                nativeAdView.visibility = View.VISIBLE
                onLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    nativeAdView.visibility = View.GONE
                    onFailed()
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
        val mediaView = nativeAdView.findViewById<MediaView>(R.id.ad_media)
        val headlineView = nativeAdView.findViewById<TextView>(R.id.ad_headline)
        val bodyView = nativeAdView.findViewById<TextView>(R.id.ad_body)
        val ctaView = nativeAdView.findViewById<Button>(R.id.ad_call_to_action)
        val iconView = nativeAdView.findViewById<ImageView>(R.id.ad_app_icon)
        val iconCard = nativeAdView.findViewById<View>(R.id.cv_icon_ads)

        nativeAdView.mediaView = mediaView
        nativeAdView.headlineView = headlineView
        nativeAdView.bodyView = bodyView
        nativeAdView.callToActionView = ctaView
        nativeAdView.iconView = iconView

        headlineView.text = nativeAd.headline

        val body = nativeAd.body
        if (body.isNullOrBlank()) {
            bodyView.visibility = View.GONE
        } else {
            bodyView.visibility = View.VISIBLE
            bodyView.text = body
        }

        val cta = nativeAd.callToAction
        if (cta.isNullOrBlank()) {
            ctaView.visibility = View.GONE
        } else {
            ctaView.visibility = View.VISIBLE
            ctaView.text = cta
        }

        val icon = nativeAd.icon
        if (icon == null) {
            iconCard.visibility = View.GONE
        } else {
            iconCard.visibility = View.VISIBLE
            iconView.setImageDrawable(icon.drawable)
        }

        val mediaContent: MediaContent? = nativeAd.mediaContent
        mediaView.setMediaContent(mediaContent)

        nativeAdView.setNativeAd(nativeAd)
    }
}
