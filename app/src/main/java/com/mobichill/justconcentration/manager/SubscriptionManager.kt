package com.mobichill.justconcentration.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.constants.Constants.SUBSCRIPTION.PRO_SUBSCRIPTION_ID
import com.mobichill.justconcentration.di.ApplicationCoroutineScope
import com.mobichill.justconcentration.repository.FirestoreRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationCoroutineScope private val externalScope: CoroutineScope,
    private val firestoreRepository: FirestoreRepository
) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private val TAG = SubscriptionManager::class.java.simpleName
        private const val RECONNECT_DELAY_MS = 3000L
    }

    private var billingClient: BillingClient? = null

    // ProductDetails: Holds info like price, title, description, offerTokens for SKUs
    private val _productDetailsFlow = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsFlow = _productDetailsFlow.asStateFlow()

    // Current active purchases/subscriptions of the user
    private val _userPurchasesFlow = MutableStateFlow<List<Purchase>>(emptyList())
    val userPurchasesFlow = _userPurchasesFlow.asStateFlow()

    // To signal UI about purchase flow events
    private val _purchaseEventFlow = MutableStateFlow<PurchaseEvent?>(null)
    val purchaseEventFlow = _purchaseEventFlow.asStateFlow()

    sealed class PurchaseEvent {
        data object InProgress : PurchaseEvent() // Indicate purchase flow started
        data class PurchaseSuccess(val purchase: Purchase) : PurchaseEvent()
        data class PurchaseFailure(val billingResult: BillingResult, val message: String? = null) :
            PurchaseEvent()

        data object PurchaseCancelled : PurchaseEvent()
        data object PurchaseErrorGeneric : PurchaseEvent() // Use for non-BillingResult errors
        data object AcknowledgmentSuccess : PurchaseEvent()
        data class AcknowledgmentFailure(val billingResult: BillingResult) : PurchaseEvent()
        data object AlreadyOwned : PurchaseEvent()
        data object BillingClientDisconnected : PurchaseEvent()
        data object BillingClientReady : PurchaseEvent()
    }


    init {
        startConnection()
    }

    // This method ensures a client exists and is ready to connect
    private fun ensureBillingClientInitializedAndReadyToConnect(): BillingClient {
        // If the client is null or NOT ready, create a new one
        if (billingClient == null || !billingClient!!.isReady) {
            Log.d(TAG, "Creating new BillingClient instance or client is not ready.")
            // End existing connection cleanly before replacing, if it exists and wasn't already disconnected
            billingClient?.endConnection() // Safe call

            billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener(this) // SubscriptionManager implements PurchasesUpdatedListener
                .build()
            Log.d(TAG, "New BillingClient instance created.")
        }
        return billingClient!! // Return the non-null instance
    }


    // Public method to start/ensure connection, can be called by ViewModel
    // idempotent: safe to call multiple times
    fun startConnection() {
        Log.d(TAG, "startConnection() called. Checking BillingClient status.")
        val client = ensureBillingClientInitializedAndReadyToConnect()

        if (!client.isReady) {
            Log.d(TAG, "BillingClient is null or not ready. Initiating connection.")
            connectToGooglePlay()
        } else {
            Log.d(TAG, "BillingClient is already ready.")
            _purchaseEventFlow.value = PurchaseEvent.BillingClientReady // Re-signal ready state
        }
    }

    // Internal connection logic
    private fun connectToGooglePlay() {
        val client = ensureBillingClientInitializedAndReadyToConnect() // Get or create client
        Log.d(TAG, "Starting connection to Google Play Billing...")
        client.startConnection(this)
    }

    // Call this when the client is no longer needed (e.g., app closing, screen gone)
    // This should be called appropriately from your application or activity lifecycle.
    fun endConnection() {
        Log.d(TAG, "endConnection() called.")
        if (billingClient?.isReady == true) {
            Log.d(TAG, "BillingClient ending connection.")
            billingClient?.endConnection()
        } else if (billingClient != null) {
            Log.w(TAG, "BillingClient exists but not ready. Attempting to end connection anyway.")
            try {
                billingClient?.endConnection()
            } catch (e: Exception) {
                Log.e(TAG, "Error ending connection on non-ready client", e)
            }
        }
        // Setting client to null is crucial so a new one is created next time startConnection is called
        billingClient = null
        Log.d(TAG, "BillingClient set to null.")
    }


    // Make isBillingClientReady a public utility
    private fun isBillingClientReady(): Boolean {
        return billingClient?.isReady == true
    }


    private fun queryProductDetails() {
        // Check readiness *after* ensureClient and connect are called
        if (!isBillingClientReady()) {
            Log.e(TAG, "queryProductDetails: BillingClient not ready.")
            return
        }
        Log.d(TAG, "Querying product details...")

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_SUBSCRIPTION_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, listProductDetails ->
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
            }
        }
    }

    fun queryUserPurchases() {
        if (!isBillingClientReady()) {
            Log.e(TAG, "queryUserPurchases: BillingClient not ready.")
            // connectToGooglePlay() // Already handled by startConnection/retry logic
            return
        }

        // ... (rest of queryUserPurchases remains the same) ...
        Log.d(TAG, "Querying user's current subscriptions...")
        val subsParams =
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        billingClient?.queryPurchasesAsync(subsParams) { billingResult, activeSubsList -> // Use safe call ?
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(
                    TAG,
                    "Active subscriptions query success: ${activeSubsList.size} found."
                )
                _userPurchasesFlow.value = activeSubsList.filter {
                    // Filter out pending/other states if you only want PURCHASED here
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                    // Or keep all and let UI handle states:
                    // it.purchaseState != Purchase.PurchaseState.UNSPECIFIED
                }
                val pendingPurchases =
                    activeSubsList.filter { it.purchaseState == Purchase.PurchaseState.PENDING }
                if (pendingPurchases.isNotEmpty()) {
                    Log.i(TAG, "Found PENDING purchases: ${pendingPurchases.size}")
                    // Signal pending state if your UI needs a specific event
                    // _purchaseEventFlow.value = PurchaseEvent.InProgress // Example: reuse InProgress
                }


                activeSubsList.forEach { purchase ->
                    // Handle acknowledged status or pending states here
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        Log.w(
                            TAG,
                            "Found unacknowledged active sub during query: ${purchase.orderId}. Handling it."
                        )
                        externalScope.launch { handlePurchase(purchase) } // Handle acknowledgment
                    } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                        Log.i(TAG, "Found PENDING purchase during query: ${purchase.orderId}.")
                        // Handle logic for pending purchases (e.g., show UI indicator)
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
        if (!isBillingClientReady()) { // Use the public check
            Log.e(TAG, "launchPurchaseFlow: BillingClient not ready.")
            // Signal a specific event indicating billing is not ready
            _purchaseEventFlow.value = PurchaseEvent.PurchaseFailure(
                BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.ERROR)
                    .setDebugMessage("Billing client not ready").build(),
                "Billing service not ready. Please try again shortly." // User friendly message
            )
            // connectToGooglePlay() // The retry logic in onBillingServiceDisconnected/onBillingSetupFinished handles this
            return
        }

        // Check if user already owns this subscription via a different plan
        // Rely on _userPurchasesFlow being populated by queryUserPurchases
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
            queryUserPurchases() // Refresh purchases to ensure local state is correct
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

        // Launch flow on the current billingClient instance
        val billingResult =
            billingClient?.launchBillingFlow(activity, billingFlowParams) // Use safe call
        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) { // Check responseCode on the result (nullable)
            Log.e(
                TAG,
                "launchBillingFlow failed: ${billingResult?.debugMessage} (Code: ${billingResult?.responseCode})" // Use safe calls
            )
            // Provide a default result if billingResult was null unexpectedly
            _purchaseEventFlow.value = PurchaseEvent.PurchaseFailure(
                billingResult ?: BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                    .setDebugMessage("Unknown launch error").build()
            )
        } else {
            Log.d(TAG, "launchBillingFlow successful, user is in purchase flow.")
            // Purchase result will be handled asynchronously in onPurchasesUpdated
        }
    }

    fun consumePurchaseEvent() {
        _purchaseEventFlow.value = null
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        Log.d(
            TAG,
            "Handling purchase for Order ID: ${purchase.orderId}, Products: ${purchase.products}, State: ${purchase.purchaseState}, Token: ${purchase.purchaseToken}"
        )
        // Need Firebase Auth ID token for backend verification
        val idToken = try {
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                ?.await()?.token // Use await() here
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firebase ID token", e)
            null
        }
        Log.d(TAG, "Firebase id token obtained: ${idToken}...") // Log first few chars or null

        if (idToken == null) {
            Log.e(TAG, "Cannot handle purchase: Firebase ID token is null.")
            // Signal an error to UI, maybe prompt user to re-authenticate?
            _purchaseEventFlow.value =
                PurchaseEvent.PurchaseErrorGeneric // Or a new token error event
            return
        }


        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // --- TODO: SERVER-SIDE VALIDATION ---
            val isVerifiedByServer = verifyPurchaseOnServer(purchase, idToken) // Pass token
            // ------------------------------------

            if (isVerifiedByServer) {
                Log.i(TAG, "Purchase VERIFIED. Product: ${purchase.products.firstOrNull()}")

                if (!purchase.isAcknowledged) {
                    Log.d(TAG, "Purchase needs acknowledgment: ${purchase.orderId}")
                    val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    // Acknowledge on the current billingClient instance
                    val ackResult = withContext(Dispatchers.IO) {
                        // Ensure client is still valid before acknowledging
                        if (billingClient?.isReady == true) {
                            billingClient?.acknowledgePurchase(acknowledgeParams) // Use safe call
                        } else {
                            Log.e(TAG, "BillingClient not ready for acknowledgment.")
                            BillingResult.newBuilder()
                                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                                .setDebugMessage("Billing client not ready for acknowledgment")
                                .build()
                        }
                    }

                    if (ackResult?.responseCode == BillingClient.BillingResponseCode.OK) { // Check responseCode and safe call
                        Log.i(TAG, "Purchase acknowledged: ${purchase.orderId}")
                        _purchaseEventFlow.value = PurchaseEvent.AcknowledgmentSuccess
                    } else {
                        Log.e(
                            TAG,
                            "Acknowledgment failed: ${ackResult?.debugMessage} (Code: ${ackResult?.responseCode})" // Use safe calls
                        )
                        _purchaseEventFlow.value = PurchaseEvent.AcknowledgmentFailure(
                            ackResult ?: BillingResult.newBuilder()
                                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                                .setDebugMessage("Unknown ack error").build()
                        ) // Provide default result
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
                _purchaseEventFlow.value = PurchaseEvent.PurchaseFailure(
                    BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
                        .setDebugMessage("Server verification failed").build(),
                    "Purchase verification failed."
                )
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.i(TAG, "Purchase is PENDING: ${purchase.orderId}. User needs to complete payment.")
            // Inform UI that purchase is pending. UI should observe _userPurchasesFlow.
            _purchaseEventFlow.value =
                PurchaseEvent.InProgress // Can reuse InProgress or add a new PENDING event
        } else {
            Log.w(
                TAG,
                "Purchase in UNSPECIFIED or other state: ${purchase.purchaseState} for ${purchase.orderId}"
            )
        }
        // Refresh purchases list after handling (might be redundant if Billing Library updates often)
        // queryUserPurchases()
    }


    // TODO: Implement robust server-side verification. This is a placeholder.
    private suspend fun verifyPurchaseOnServer(purchase: Purchase, idToken: String?): Boolean {
        if (idToken == null) {
            Log.e(TAG, "verifyPurchaseOnServer: Firebase ID token is null during verification.")
            return false
        }
        Log.w(
            TAG,
            "verifyPurchaseOnServer: SIMULATING successful verification for ${purchase.orderId}. NOT FOR PRODUCTION!"
        )
        Log.d(
            TAG,
            "Simulating call to backend with token: ${purchase.purchaseToken} and ID token: ${
                idToken.take(5)
            }..."
        )

        // --- REAL IMPLEMENTATION ---
        // Use your actual backend URL
        // val backendUrl = "YOUR_FUNCTION_URL/api/validate-purchase"
        // val userId = FirebaseAuth.getInstance().currentUser?.uid

        // val jsonBody = """{"userId": "$userId", "purchaseToken": "${purchase.purchaseToken}", "productId": "${purchase.products.firstOrNull()}"}"""
        // ... (HTTP client setup and call as in previous comment) ...

        // For testing the backend:
        val simulatedBackendSuccess = true // Assume backend works for now

        if (simulatedBackendSuccess) {
            // In a REAL app, the backend would update Firestore.
            // For testing client flow without real backend call yet:
            externalScope.launch { // Launch in a suitable scope
                try {
                    firestoreRepository.updateUserSubscriptionStatus(
                        FirebaseAuth.getInstance().currentUser?.uid!!, // Ensure UID is not null in practice
                        // You CANNOT get expiryDateMillis reliably client-side from the Purchase object for this purpose.
                        // This MUST come from your backend after it calls the Google Play Developer API.
                        UserSubscriptionStatus( // Assuming you have this data class/interface
                            isPro = true,
                            productId = purchase.products.firstOrNull(),
                            purchaseToken = purchase.purchaseToken,
                            expiryDateMillis = null // Backend provides this!
                        )
                    )
                    Log.d(
                        TAG,
                        "Simulated Firestore update after server verification simulation success."
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Simulated Firestore update failed!", e)
                }
            }
        }

        return simulatedBackendSuccess
    }


    // PurchasesUpdatedListener implementation (remains largely the same, minor cleanup)
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
                    // Update _userPurchasesFlow with *all* valid purchases returned by this callback
                    _userPurchasesFlow.value = purchases.toList() // Convert to immutable list
                    purchases.forEach { purchase ->
                        externalScope.launch { handlePurchase(purchase) }
                    }
                    // Signal success, perhaps pass the list or just a general success
                    _purchaseEventFlow.value = PurchaseEvent.PurchaseSuccess(
                        purchases.firstOrNull() ?: FakeEmptyPurchase()
                    ) // Provide first or a default
                } else {
                    // Should not happen with OK response, but handle defensively
                    Log.w(TAG, "onPurchasesUpdated: OK but purchases list is null.")
                    _userPurchasesFlow.value = emptyList()
                    _purchaseEventFlow.value =
                        PurchaseEvent.PurchaseErrorGeneric // Or specific error
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User cancelled the purchase flow.")
                _purchaseEventFlow.value = PurchaseEvent.PurchaseCancelled
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.w(TAG, "Item already owned.")
                _purchaseEventFlow.value = PurchaseEvent.AlreadyOwned
                // Refresh to ensure local state is correct and UI updates
                // Delay slightly to give Billing Library time to update its cache? Or rely on queryUserPurchases logic.
                externalScope.launch {
                    delay(500) // Small delay before re-querying
                    queryUserPurchases()
                }
            }

            else -> {
                Log.e(
                    TAG,
                    "Purchase error: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})"
                )
                _purchaseEventFlow.value = PurchaseEvent.PurchaseFailure(billingResult)
            }
        }
        // queryUserPurchases() // Avoid calling here, might cause infinite loops or redundancy
    }

    // BillingClientStateListener implementation
    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "BillingClient service disconnected. Client is no longer ready. Set to null")
        // CRITICAL: Set the client to null immediately so ensureBillingClientInitializedAndReadyToConnect creates a NEW one
        billingClient = null
        _purchaseEventFlow.value =
            PurchaseEvent.BillingClientDisconnected // Signal disconnection to UI

        // Implement retry logic here: try connecting again after a delay
        // IMPORTANT: Launch this retry logic in a scope that survives disconnection, like the Singleton's externalScope
        externalScope.launch(Dispatchers.IO) { // Use IO dispatcher for delay and potential network calls
            Log.d(TAG, "Waiting $RECONNECT_DELAY_MS ms before attempting to reconnect...")
            delay(RECONNECT_DELAY_MS)
            Log.d(TAG, "Attempting to reconnect after disconnection...")
            // Set billingClient to null so ensureBillingClientInitializedAndReadyToConnect creates a NEW instance
            connectToGooglePlay() // Start the connection process again
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "BillingClient setup successful. Ready to perform operations.")
            _purchaseEventFlow.value = PurchaseEvent.BillingClientReady // Signal readiness to UI
            queryProductDetails()
            queryUserPurchases()
        } else {
            Log.e(
                TAG,
                "BillingClient setup failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})"
            )
            // Handle setup failure - inform UI, potentially retry connection after a delay
            Log.e(TAG, "BillingClient setup failed. Setting billingClient to null for retry.")
            // CRITICAL: Set the client to null immediately upon setup failure
            billingClient = null
            _purchaseEventFlow.value =
                PurchaseEvent.PurchaseFailure(billingResult, "Billing setup failed.")

            // Implement retry logic similar to onBillingServiceDisconnected if initial setup fails
            externalScope.launch(Dispatchers.IO) {
                Log.d(
                    TAG,
                    "Waiting $RECONNECT_DELAY_MS ms before attempting to reconnect after setup failure..."
                )
                delay(RECONNECT_DELAY_MS)
                Log.d(TAG, "Attempting to reconnect after setup failure...")
                billingClient = null // Ensure new instance
                connectToGooglePlay() // Start the connection process again
            }
        }
    }

    // Simple fake purchase for handling null case defensively
    private class FakeEmptyPurchase : Purchase("{}", "") {
        // Provide default implementations for methods that might be accessed
        override fun getProducts(): MutableList<String> = mutableListOf()
        override fun getPurchaseToken(): String = ""
        override fun getPurchaseState(): Int = PurchaseState.UNSPECIFIED_STATE
        override fun isAcknowledged(): Boolean = false
        override fun getOrderId(): String? = null
        // Add other overrides if needed based on how Purchase objects are used
    }

    // Ensure UserSubscriptionStatus data class exists elsewhere in your project
    data class UserSubscriptionStatus(
        val isPro: Boolean = false, // Default to false
        val productId: String? = null,
        val purchaseToken: String? = null,
        val expiryDateMillis: Long? = null // Use Long for milliseconds
        // Add other relevant fields from Google API if stored server-side and fetched
        // val autoRenewing: Boolean? = null,
        // val purchaseTimeMillis: Long? = null,
        // val orderId: String? = null,
        // val acknowledgementState: Int? = null
    )

    // Dummy FirestoreRepository for compilation if it's not fully implemented yet
    /*
    class FirestoreRepository @Inject constructor() {
         suspend fun updateUserSubscriptionStatus(userId: String, status: UserSubscriptionStatus) {
              Log.d(TAG, "FirestoreRepository: Simulating update for user $userId with status $status")
              // TODO: Real Firestore update logic here
              // Firebase.firestore.collection("users").document(userId).set(status, SetOptions.merge())
         }
     }
    */
}