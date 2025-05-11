package com.mobichill.justconcentration.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.mobichill.justconcentration.manager.SubscriptionManager
import com.mobichill.justconcentration.repository.FirestoreRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreRepository()

    val subscriptionManager = SubscriptionManager(application, viewModelScope, firestoreRepository)

    val productDetailsList: StateFlow<List<ProductDetails>> = subscriptionManager.productDetailsFlow
    val userPurchases: StateFlow<List<Purchase>> = subscriptionManager.userPurchasesFlow
    val purchaseEvent: StateFlow<SubscriptionManager.PurchaseEvent?> =
        subscriptionManager.purchaseEventFlow

    private val _isUserPro = MutableLiveData<Boolean>()
    val isUserPro: LiveData<Boolean> = _isUserPro

    init {
        // Observe userPurchases to update isUserPro status
        viewModelScope.launch {
            userPurchases.collect { purchases ->
                // Basic check: is there any active "PRO_SUBSCRIPTION_ID" purchase?
                // More robust check involves expiry date and server validation via UserRepository
                val proStatus = firestoreRepository.isUserPro() // This should reflect the true status
                _isUserPro.postValue(proStatus)
            }
        }
        // Query purchases on init to get current status
        // The SubscriptionManager already calls queryUserPurchases on setup
    }


    fun refreshSubscriptionStatus() {
        // This might trigger a more forceful check in UserRepository if needed
        // For now, just re-querying local purchases via BillingClient might be enough
        // if handlePurchase and server RTDNs are correctly updating the backend/Firestore.
        subscriptionManager.queryUserPurchases()
        viewModelScope.launch { // Re-check pro status from repository
            _isUserPro.postValue(firestoreRepository.isUserPro())
        }
    }

    override fun onCleared() {
        super.onCleared()
        subscriptionManager.destroy()
    }
}