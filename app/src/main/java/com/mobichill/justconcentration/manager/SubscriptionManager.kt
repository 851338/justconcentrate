package com.mobichill.justconcentration.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.api.BackendValidationResponse
import com.mobichill.justconcentration.constants.Constants.BACKEND.BASE_URL
import com.mobichill.justconcentration.constants.Constants.BACKEND.VALIDATE_PURCHASE_ENDPOINT
import com.mobichill.justconcentration.constants.Constants.SUBSCRIPTION.PRO_SUBSCRIPTION_ID
import com.mobichill.justconcentration.constants.MyKtorLogger
import com.mobichill.justconcentration.di.ApplicationCoroutineScope
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationCoroutineScope private val externalScope: CoroutineScope
) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private val TAG = SubscriptionManager::class.java.simpleName
        private const val RECONNECT_DELAY_MS = 3000L
    }

    private var billingClient: BillingClient? = null

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = MyKtorLogger
            level = LogLevel.ALL
        }
    }

    private val _productDetailsFlow = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsFlow = _productDetailsFlow.asStateFlow()

    private val _userPurchasesFlow = MutableStateFlow<List<Purchase>>(emptyList())
    val userPurchasesFlow = _userPurchasesFlow.asStateFlow()

    private val _purchaseEventFlow = MutableStateFlow<PurchaseEvent?>(null)
    val purchaseEventFlow = _purchaseEventFlow.asStateFlow()

    // --- PurchaseEvent Sealed Class ---
    sealed class PurchaseEvent {
        data object InProgress : PurchaseEvent()
        data class PurchaseSuccess(val purchase: Purchase) : PurchaseEvent()
        data class PurchaseFailure(val billingResult: BillingResult, val message: String? = null) : PurchaseEvent()
        data class ServerVerificationFailure(val message: String) : PurchaseEvent()

        data object PurchaseCancelled : PurchaseEvent()
        data object PurchaseErrorGeneric : PurchaseEvent()
        data object AcknowledgmentSuccess : PurchaseEvent()
        data class AcknowledgmentFailure(val billingResult: BillingResult) : PurchaseEvent()
        data object AlreadyOwned : PurchaseEvent()
        data object BillingClientDisconnected : PurchaseEvent()
        data object BillingClientReady : PurchaseEvent()
        data object FirebaseAuthTokenError : PurchaseEvent()
    }
    // --- End PurchaseEvent ---


    init {
        startConnection()
    }

    private fun ensureBillingClientInitializedAndReadyToConnect(): BillingClient {
        if (billingClient == null || !billingClient!!.isReady) {
            Log.d(TAG, "Creating new BillingClient instance or client is not ready.")
            billingClient?.endConnection()

            billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener(this)
                .build()
            Log.d(TAG, "New BillingClient instance created.")
        }
        return billingClient!!
    }

    fun startConnection() {
        Log.d(TAG, "startConnection() called. Checking BillingClient status.")
        val client = ensureBillingClientInitializedAndReadyToConnect()
        if (!client.isReady) {
            Log.d(TAG, "BillingClient is null or not ready. Initiating connection.")
            connectToGooglePlay()
        } else {
            Log.d(TAG, "BillingClient is already ready.")
        }
    }

    private fun connectToGooglePlay() {
        val client = ensureBillingClientInitializedAndReadyToConnect()
        Log.d(TAG, "Starting connection to Google Play Billing...")
        client.startConnection(this)
    }

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
        billingClient = null
        Log.d(TAG, "BillingClient set to null.")
        _productDetailsFlow.value = emptyList()
        _userPurchasesFlow.value = emptyList()
        _purchaseEventFlow.value = PurchaseEvent.BillingClientDisconnected
    }

    private fun isBillingClientReady(): Boolean {
        return billingClient?.isReady == true
    }

    private fun queryProductDetails() {
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
        if (!isBillingClientReady()) {
            Log.e(TAG, "queryUserPurchases: BillingClient not ready.")
            return
        }

        Log.d(TAG, "Querying user's current subscriptions...")
        val subsParams =
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        billingClient?.queryPurchasesAsync(subsParams) { billingResult, activeSubsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(
                    TAG,
                    "Active subscriptions query success: ${activeSubsList.size} found."
                )
                _userPurchasesFlow.value = activeSubsList.filter {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED || it.purchaseState == Purchase.PurchaseState.PENDING
                }.toList()

                // Immediately handle any unacknowledged purchases found
                activeSubsList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
                    .forEach { purchase ->
                        Log.w(
                            TAG,
                            "Found unacknowledged active sub during query: ${purchase.orderId}. Handling it."
                        )
                        externalScope.launch { handlePurchase(purchase) } // This will now call server validation and then acknowledge
                    }

                val pendingPurchases = activeSubsList.filter { it.purchaseState == Purchase.PurchaseState.PENDING }
                if (pendingPurchases.isNotEmpty()) {
                    Log.i(TAG, "Found PENDING purchases: ${pendingPurchases.size}. Update UI based on _userPurchasesFlow.")
                }

            } else {
                Log.e(
                    TAG,
                    "Active subscriptions query failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})"
                )
                _userPurchasesFlow.value = emptyList()
            }
        }
    }


    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        selectedOfferToken: String
    ) {
        if (!isBillingClientReady()) {
            Log.e(TAG, "launchPurchaseFlow: BillingClient not ready.")
            _purchaseEventFlow.value = PurchaseEvent.PurchaseFailure(
                BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.ERROR)
                    .setDebugMessage("Billing client not ready").build(),
                "Billing service not ready. Please try again shortly."
            )
            return
        }

        // Check if user already owns this specific subscription ID via an *acknowledged* purchase
        val currentProSubscription = _userPurchasesFlow.value.find { purchase ->
            purchase.products.contains(PRO_SUBSCRIPTION_ID) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.isAcknowledged // Check acknowledged state here
        }

        if (currentProSubscription != null) {
            Log.w(
                TAG,
                "User already owns an ACTIVE Pro subscription (Order: ${currentProSubscription.orderId}). Informing user."
            )
            _purchaseEventFlow.value = PurchaseEvent.AlreadyOwned
            return
        }

        Log.d(
            TAG,
            "Launching purchase flow for ${productDetails.productId} with offerToken: $selectedOfferToken"
        )
        _purchaseEventFlow.value = PurchaseEvent.InProgress

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(selectedOfferToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(
                TAG,
                "launchBillingFlow failed: ${billingResult?.debugMessage} (Code: ${billingResult?.responseCode})"
            )
            _purchaseEventFlow.value = PurchaseEvent.PurchaseFailure(
                billingResult ?: BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                    .setDebugMessage("Unknown launch error").build()
            )
        } else {
            Log.d(TAG, "launchBillingFlow successful, user is in purchase flow.")
        }
    }

    fun consumePurchaseEvent() {
        _purchaseEventFlow.value = null
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        Log.d(
            TAG,
            "Handling purchase for Order ID: ${purchase.orderId}, Products: ${purchase.products}, State: ${purchase.purchaseState}, Token: ${purchase.purchaseToken}, Acknowledged: ${purchase.isAcknowledged}"
        )

        // Only handle PURCHASES and those needing acknowledgment
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || purchase.isAcknowledged) {
            if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                Log.i(TAG, "HandlePurchase called for PENDING purchase. Will not verify/ack here.")
                // UI should observe _userPurchasesFlow for pending state
            } else if (purchase.isAcknowledged) {
                Log.i(TAG, "HandlePurchase called for already acknowledged purchase. Skipping.")
                // This might happen if onPurchasesUpdated is called multiple times for an already handled purchase
            } else {
                Log.w(TAG, "HandlePurchase called for unexpected state: ${purchase.purchaseState}. Skipping.")
            }
            return
        }

        // Get Firebase Auth ID token for backend verification
        val idToken = try {
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firebase ID token", e)
            null
        }

        if (idToken == null) {
            Log.e(TAG, "Cannot handle purchase: Firebase ID token is null. User not authenticated?")
            _purchaseEventFlow.value = PurchaseEvent.FirebaseAuthTokenError
            // Do NOT proceed with verification or acknowledgment
            return
        }

        Log.d(TAG, "Firebase ID token obtained (starts with ${idToken.take(5)}...).")

        // --- SERVER-SIDE VALIDATION ---
        // This call will validate the token AND update Firestore in the backend
        val verificationResult = verifyPurchaseOnServer(purchase, idToken)
        // ------------------------------------

        if (verificationResult.success) {
            Log.i(
                TAG,
                "Purchase VERIFIED by server. Order ID: ${purchase.orderId}. Backend confirms status updated."
            )

            // The backend has already updated Firestore with the correct status (isPro=true, expiry etc.)
            // Now, acknowledge the purchase if it hasn't been already (redundant check after initial filter, but safe)
            if (!purchase.isAcknowledged) {
                Log.d(TAG, "Proceeding with acknowledgment for: ${purchase.orderId}")
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                val ackResult = withContext(Dispatchers.IO) {
                    try {
                        if (billingClient?.isReady == true) {
                            billingClient?.acknowledgePurchase(acknowledgeParams)
                        } else {
                            Log.e(TAG, "BillingClient not ready for acknowledgment (after verification).")
                            BillingResult.newBuilder()
                                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                                .setDebugMessage("Billing client not ready for acknowledgment")
                                .build()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during acknowledgment", e)
                        BillingResult.newBuilder()
                            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                            .setDebugMessage("Exception during acknowledgment: ${e.message}")
                            .build()
                    }
                }

                if (ackResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Purchase acknowledged successfully: ${purchase.orderId}")
                    _purchaseEventFlow.value = PurchaseEvent.AcknowledgmentSuccess
                    // Acknowledgment successful. Re-query purchases to update the local list state
                    queryUserPurchases() // This will update _userPurchasesFlow, including isAcknowledged status
                } else {
                    Log.e(
                        TAG,
                        "Acknowledgment failed: ${ackResult?.debugMessage} (Code: ${ackResult?.responseCode})"
                    )
                    _purchaseEventFlow.value = PurchaseEvent.AcknowledgmentFailure(
                        ackResult ?: BillingResult.newBuilder()
                            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                            .setDebugMessage("Unknown ack error").build()
                    )
                    // If ack failed *after* server verification, this is a problem.
                    // Firestore is PRO, but purchase isn't ACK. Play might refund later.
                    // Monitor logs. Re-querying might trigger handlePurchase again later.
                }
            } else {
                Log.i(TAG, "Purchase was already acknowledged: ${purchase.orderId}. Firestore already updated by server.")
                _purchaseEventFlow.value = PurchaseEvent.AcknowledgmentSuccess
                queryUserPurchases() // Re-query to be sure local state is accurate
            }


        } else { // Server verification failed
            Log.e(
                TAG,
                "Purchase NOT VERIFIED by server. Order ID: ${purchase.orderId}. Message: ${verificationResult.message}"
            )
            // The backend has already updated Firestore to isPro=false based on its validation failure.
            _purchaseEventFlow.value = PurchaseEvent.ServerVerificationFailure(verificationResult.message)
            // Do NOT acknowledge a purchase that failed server verification.
            // No need to explicitly update Firestore from the client here; the backend did it.
            // Optionally re-query purchases to reflect potential state changes or ensure list is current
            queryUserPurchases()
        }
    }

    // --- REAL SERVER-SIDE VERIFICATION IMPLEMENTATION ---
    private suspend fun verifyPurchaseOnServer(purchase: Purchase, idToken: String): BackendValidationResponse {
        return withContext(Dispatchers.IO) {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    Log.e(TAG, "verifyPurchaseOnServer: Firebase user is null, cannot build request.")
                    return@withContext BackendValidationResponse(
                        success = false,
                        message = "User not authenticated for verification request."
                    )
                }

                Log.d(TAG, "Calling backend validation endpoint: $BASE_URL$VALIDATE_PURCHASE_ENDPOINT")

                val response: BackendValidationResponse = httpClient.post("$BASE_URL$VALIDATE_PURCHASE_ENDPOINT") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $idToken")
                    setBody(
                        mapOf(
                            "userId" to userId,
                            "purchaseToken" to purchase.purchaseToken,
                            "productId" to purchase.products.firstOrNull()
                        )
                    )
                }.body()

                Log.d(TAG, "Backend validation response: success=${response.success}, message=${response.message}, isPro=${response.isPro}, expiry=${response.expiryDateMillis}")

                response // Return the response object

            } catch (e: Exception) {
                Log.e(TAG, "Exception calling backend verification API", e)
                BackendValidationResponse(
                    success = false,
                    message = "Failed to communicate with verification server: ${e.message}"
                )
            }
        }
    }

    // PurchasesUpdatedListener implementation
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
                    _userPurchasesFlow.value = purchases.filter {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED || it.purchaseState == Purchase.PurchaseState.PENDING
                    }.toList()

                    purchases.forEach { purchase ->
                        externalScope.launch { handlePurchase(purchase) }
                    }
                    _purchaseEventFlow.value = PurchaseEvent.PurchaseSuccess(
                        purchases.firstOrNull() ?: FakeEmptyPurchase()
                    )
                } else {
                    Log.w(TAG, "onPurchasesUpdated: OK but purchases list is null.")
                    _userPurchasesFlow.value = emptyList()
                    _purchaseEventFlow.value = PurchaseEvent.PurchaseErrorGeneric
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User cancelled the purchase flow.")
                _purchaseEventFlow.value = PurchaseEvent.PurchaseCancelled
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.w(TAG, "Item already owned. Re-querying purchases.")
                _purchaseEventFlow.value = PurchaseEvent.AlreadyOwned
                externalScope.launch {
                    delay(500)
                    queryUserPurchases()
                }
            }

            else -> {
                Log.e(
                    TAG,
                    "Purchase flow failed with code: ${billingResult.responseCode}, message: ${billingResult.debugMessage}"
                )
                _purchaseEventFlow.value = PurchaseEvent.PurchaseFailure(billingResult)
            }
        }
    }

    // BillingClientStateListener implementation
    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "BillingClient service disconnected. Client is no longer ready. Set to null")
        billingClient = null
        _purchaseEventFlow.value = PurchaseEvent.BillingClientDisconnected

        externalScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Waiting $RECONNECT_DELAY_MS ms before attempting to reconnect...")
            delay(RECONNECT_DELAY_MS)
            Log.d(TAG, "Attempting to reconnect after disconnection...")
            connectToGooglePlay()
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "BillingClient setup successful. Ready to perform operations.")
            _purchaseEventFlow.value = PurchaseEvent.BillingClientReady
            queryProductDetails()
            queryUserPurchases()
        } else {
            Log.e(
                TAG,
                "BillingClient setup failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})"
            )
            Log.e(TAG, "BillingClient setup failed. Setting billingClient to null for retry.")
            billingClient = null
            _purchaseEventFlow.value = PurchaseEvent.PurchaseFailure(billingResult, "Billing setup failed.")

            externalScope.launch(Dispatchers.IO) {
                Log.d(
                    TAG,
                    "Waiting $RECONNECT_DELAY_MS ms before attempting to reconnect after setup failure..."
                )
                delay(RECONNECT_DELAY_MS)
                Log.d(TAG, "Attempting to reconnect after setup failure...")
                connectToGooglePlay()
            }
        }
    }

    // FakeEmptyPurchase and UserSubscriptionStatus data classes
    private class FakeEmptyPurchase : Purchase("{}", "") {
        override fun getProducts(): MutableList<String> = mutableListOf()
        override fun getPurchaseToken(): String = ""
        override fun getPurchaseState(): Int = PurchaseState.UNSPECIFIED_STATE
        override fun isAcknowledged(): Boolean = false
        override fun getOrderId(): String? = null
    }
}