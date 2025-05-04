package com.mobichill.justconcentration.manager

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
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
                } else {class BillingClientManager(
                    private val context: Context,
                    private val onPurchaseUpdated: (BillingResult, List<Purchase>?) -> Unit
                ) {
                    private var billingClient: BillingClient = BillingClient.newBuilder(context)
                        .enablePendingPurchases()
                        .setListener(onPurchaseUpdated)
                        .build()

                    fun startConnection(onConnected: () -> Unit) {
                        billingClient.startConnection(object : BillingClientStateListener {
                            override fun onBillingSetupFinished(billingResult: BillingResult) {
                                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                    onConnected()
                                }
                            }

                            override fun onBillingServiceDisconnected() {
                                // Retry connection if needed
                            }
                        })
                    }

                    fun querySubscriptions(onResult: (List<ProductDetails>) -> Unit) {
                        val params = QueryProductDetailsParams.newBuilder()
                            .setProductList(
                                listOf(
                                    QueryProductDetailsParams.Product.newBuilder()
                                        .setProductId("premium_monthly")
                                        .setProductType(BillingClient.ProductType.SUBS)
                                        .build()
                                )
                            ).build()

                        billingClient.queryProductDetailsAsync(params) { _, productDetailsList ->
                            onResult(productDetailsList)
                        }
                    }

                    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
                        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
                        val params = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(
                                listOf(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails)
                                        .setOfferToken(offerToken)
                                        .build()
                                )
                            ).build()
                        billingClient.launchBillingFlow(activity, params)
                    }
                }

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