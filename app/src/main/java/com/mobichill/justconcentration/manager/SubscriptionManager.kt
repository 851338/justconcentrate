package com.mobichill.justconcentration.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.mobichill.justconcentration.constants.Constants.SUBSCRIPTION.PRO_SUBSCRIPTION_ID
import com.mobichill.justconcentration.repository.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubscriptionManager(
    private val context: Context,
    private val externalScope: CoroutineScope,
    private val firestoreRepository: FirestoreRepository
) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private val TAG = SubscriptionManager::class.java.simpleName
    }

    private lateinit var billingClient: BillingClient

    // ProductDetails: Holds info like price, title, description, offerTokens for SKUs
    private val _productDetailsFlow = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsFlow = _productDetailsFlow.asStateFlow()

    // Current active purchases/subscriptions of the user
    private val _userPurchasesFlow = MutableStateFlow<List<Purchase>>(emptyList())
    val userPurchasesFlow =
        _userPurchasesFlow.asStateFlow() // Observe this for current entitlements

    // To signal UI about purchase flow events
    private val _purchaseEventFlow = MutableStateFlow<PurchaseEvent?>(null)
    val purchaseEventFlow = _purchaseEventFlow.asStateFlow()

    sealed class PurchaseEvent {
        data object InProgress : PurchaseEvent() // Indicate purchase flow started
        data class PurchaseSuccess(val purchase: Purchase) : PurchaseEvent()
        data class PurchaseFailure(val billingResult: BillingResult, val message: String? = null) :
            PurchaseEvent()

        data object PurchaseCancelled : PurchaseEvent()
        data object PurchaseErrorGeneric : PurchaseEvent()
        data object AcknowledgmentSuccess : PurchaseEvent()
        data class AcknowledgmentFailure(val billingResult: BillingResult) : PurchaseEvent()
        data object AlreadyOwned : PurchaseEvent() // If user tries to buy something they own
    }

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        Log.d(TAG, "Initializing BillingClient.")
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .build()
        connectToGooglePlay()
    }

    private fun connectToGooglePlay() {
        if (!billingClient.isReady) {
            Log.d(TAG, "Connecting to Google Play Billing...")
            billingClient.startConnection(this)
        } else {
            Log.d(TAG, "BillingClient already ready.")
            queryProductDetails()
            queryUserPurchases()
        }
    }

    private fun queryProductDetails() {
        if (!billingClient.isReady) {
            Log.e(TAG, "queryProductDetails: BillingClient not ready. Attempting to connect.")
            connectToGooglePlay()
            return
        }
        Log.d(TAG, "Querying product details...")

        val productList = listOf(
            // Query for the main subscription product ID
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_SUBSCRIPTION_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, listProductDetails ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Product details query successful: ${listProductDetails.size} items.")
                _productDetailsFlow.value = listProductDetails
                listProductDetails.forEach { pd ->
                    Log.d(TAG, "Product: ${pd.productId}, Title: ${pd.title}")
                    pd.subscriptionOfferDetails?.forEach { offer ->
                        Log.d(
                            TAG,
                            "Offer: ${offer.offerId}, BasePlan: ${offer.basePlanId}, Token: ${offer.offerToken}"
                        )
                        offer.pricingPhases.pricingPhaseList.forEachIndexed { i, phase ->
                            Log.d(
                                TAG,
                                "Phase $i: ${phase.formattedPrice} for ${phase.billingPeriod}, Recurrence: ${phase.recurrenceMode}"
                            )
                        }
                    }
                }
            } else {
                Log.e(
                    TAG,
                    "Product details query failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})"
                )
                _productDetailsFlow.value = emptyList()
            }
        }
    }

    fun queryUserPurchases() {
        if (!billingClient.isReady) {
            Log.e(TAG, "queryUserPurchases: BillingClient not ready.")
            connectToGooglePlay() // Optionally try to reconnect
            return
        }

        Log.d(TAG, "Querying user's current subscriptions...")
        val subsParams =
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        billingClient.queryPurchasesAsync(subsParams) { billingResult, activeSubsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(
                    TAG,
                    "Active subscriptions query success: ${activeSubsList.size} found."
                )
                _userPurchasesFlow.value = activeSubsList
                activeSubsList.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        Log.w(
                            TAG,
                            "Found unacknowledged active sub during query: ${purchase.orderId}. Handling it."
                        )
                        externalScope.launch { handlePurchase(purchase) }
                    }
                }
            } else {
                Log.e(
                    TAG,
                    "Active subscriptions query failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})"
                )
            }
        }
    }

    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        selectedOfferToken: String
    ) {
        if (!billingClient.isReady) {
            Log.e(TAG, "launchPurchaseFlow: BillingClient not ready.")
            _purchaseEventFlow.value = PurchaseEvent.PurchaseErrorGeneric
            connectToGooglePlay() // Try to reconnect
            return
        }

        // Check if user already owns this subscription via a different plan
        val currentProSubscription = _userPurchasesFlow.value.find { purchase ->
            purchase.products.contains(PRO_SUBSCRIPTION_ID) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (currentProSubscription != null) {
            Log.w(
                TAG,
                "User already owns a Pro subscription (Order: ${currentProSubscription.orderId}). Informing user."
            )
            _purchaseEventFlow.value = PurchaseEvent.AlreadyOwned
            return
        }

        Log.d(
            TAG,
            "Launching purchase flow for ${productDetails.productId} with offerToken: $selectedOfferToken"
        )
        _purchaseEventFlow.value = PurchaseEvent.InProgress // Signal UI purchase has started

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(selectedOfferToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(
                TAG,
                "launchBillingFlow failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})"
            )
            _purchaseEventFlow.value = PurchaseEvent.PurchaseFailure(billingResult)
        } else {
            Log.d(TAG, "launchBillingFlow successful, user is in purchase flow.")
            // Purchase result will be handled in onPurchasesUpdated
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        Log.d(
            TAG,
            "Handling purchase for Order ID: ${purchase.orderId}, Products: ${purchase.products}, State: ${purchase.purchaseState}, Token: ${purchase.purchaseToken}"
        )

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // --- TODO: SERVER-SIDE VALIDATION ---
            // 1. Send purchase.purchaseToken and purchase.products.first() to your backend.
            // 2. Backend validates with Google Play Developer API.
            // 3. Backend updates user's record (e.g., in Firestore) with subscription status, expiry, etc.
            // 4. Backend tells app success/failure.
            val isVerifiedByServer = verifyPurchaseOnServer(purchase)
            // ------------------------------------

            if (isVerifiedByServer) {
                // For subscriptions, grant entitlement based on server update.
                // For local testing without backend, you might update UserRepository here.
                Log.i(
                    TAG,
                    "Purchase VERIFIED (simulated or real). Product: ${purchase.products.firstOrNull()}"
                )
                // Example: This would typically be handled by your backend updating Firestore,
                // and your app observing Firestore changes
                // For direct client update (less secure, for dev only):
                // userRepository.updateProStatus(true, calculateExpiry(purchase), purchase.purchaseToken)
                if (!purchase.isAcknowledged) {
                    Log.d(TAG, "Purchase needs acknowledgment: ${purchase.orderId}")
                    val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    val ackResult = withContext(Dispatchers.IO) {
                        billingClient.acknowledgePurchase(acknowledgeParams)
                    }
                    if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.i(TAG, "Purchase acknowledged: ${purchase.orderId}")
                        _purchaseEventFlow.value = PurchaseEvent.AcknowledgmentSuccess
                    } else {
                        Log.e(
                            TAG,
                            "Acknowledgment failed: ${ackResult.debugMessage} (Code: ${ackResult.responseCode})"
                        )
                        _purchaseEventFlow.value = PurchaseEvent.AcknowledgmentFailure(ackResult)
                    }
                } else {
                    Log.i(TAG, "Purchase already acknowledged: ${purchase.orderId}")
                    _purchaseEventFlow.value =
                        PurchaseEvent.AcknowledgmentSuccess // Indicate success to UI
                }
            } else {
                Log.e(
                    TAG,
                    "Purchase NOT VERIFIED. Order ID: ${purchase.orderId}. Do not grant entitlement."
                )
                // TODO: Handle verification failure (e.g., inform user, potentially revoke if granted optimistically)
                _purchaseEventFlow.value = PurchaseEvent.PurchaseFailure(
                    BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
                        .setDebugMessage("Server verification failed").build(),
                    "Purchase verification failed."
                )
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.i(TAG, "Purchase is PENDING: ${purchase.orderId}. User needs to complete payment.")
            // Inform UI that purchase is pending
        } else {
            Log.w(
                TAG,
                "Purchase in UNSPECIFIED or other state: ${purchase.purchaseState} for ${purchase.orderId}"
            )
        }
        queryUserPurchases() // Refresh purchases after handling
    }

    // TODO: Implement robust server-side verification. This is a placeholder.
    private suspend fun verifyPurchaseOnServer(purchase: Purchase): Boolean {
        Log.w(
            TAG,
            "verifyPurchaseOnServer: SIMULATING successful verification for ${purchase.orderId}. NOT FOR PRODUCTION!"
        )
        // In a real app, this involves an HTTPS call to your backend which then calls Google's API.
        // The backend would update your user database (e.g., Firestore).
        // For now, just return true to allow client-side flow to continue for testing.
        // If you have a backend, call it here:
        // val result = myBackendService.validatePurchase(purchase.purchaseToken, purchase.products.first(), auth.currentUser.uid)
        // return result.isSuccess
        return true
    }

    fun destroy() {
        if (::billingClient.isInitialized && billingClient.isReady) {
            Log.d(TAG, "BillingClient ending connection.")
            billingClient.endConnection()
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        Log.d(
            TAG,
            "onPurchasesUpdated: Response Code: ${billingResult.responseCode}, Purchases: ${purchases?.size ?: "null"}"
        )
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    purchases.forEach { purchase ->
                        externalScope.launch { handlePurchase(purchase) }
                    }
                    _purchaseEventFlow.value = PurchaseEvent.PurchaseSuccess(purchases.first())
                } else {
                    _purchaseEventFlow.value =
                        PurchaseEvent.PurchaseSuccess(FakeEmptyPurchase()) // Should not happen if OK
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User cancelled the purchase flow.")
                _purchaseEventFlow.value = PurchaseEvent.PurchaseCancelled
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.w(TAG, "Item already owned.")
                _purchaseEventFlow.value = PurchaseEvent.AlreadyOwned
                queryUserPurchases() // Refresh to ensure local state is correct
            }

            else -> {
                Log.e(
                    TAG,
                    "Purchase error: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})"
                )
                _purchaseEventFlow.value = PurchaseEvent.PurchaseFailure(billingResult)
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "BillingClient service disconnected.")
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "BillingClient setup successful.")
            queryProductDetails()
            queryUserPurchases()
        } else {
            Log.e(
                TAG,
                "BillingClient setup failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})"
            )
        }
    }

    private class FakeEmptyPurchase : Purchase("{}", "")
}