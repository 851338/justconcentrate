package com.mobichill.justconcentration

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.mobichill.justconcentration.model.ProductItem

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .build()

    fun checkSubscriptionStatus(productId: String, onResult: (Boolean) -> Unit) {
        val purchasesResult =
            billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val isSubscribed =
                        purchases.any { it.products.contains(productId) && it.isAutoRenewing }
                    onResult(isSubscribed)
                } else {
                    onResult(false)
                }
            }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        // Handle new purchases here
    }

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {

            }

            override fun onBillingServiceDisconnected() {
                // Handle disconnection
            }
        })
    }

    fun querySubscriptions(onResult: (List<ProductItem>) -> Unit) {
        val subscriptionIds =
            listOf("weekly_subscription", "monthly_subscription", "yearly_subscription")
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(subscriptionIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }).build()
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val products = productDetailsList.map { product ->
                    ProductItem(
                        title = product.title,
                        description = product.description,
                        price = product.subscriptionOfferDetails
                            ?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)?.formattedPrice
                            ?: "N/A",
                        productDetails = product
                    )
                }
                onResult(products)
            } else {
                onResult(emptyList())
            }
        }
    }

}
