package com.mobichill.justconcentration.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import com.mobichill.justconcentration.manager.ProBadgeManager
import com.mobichill.justconcentration.manager.SubscriptionManager
import com.mobichill.justconcentration.repository.FirestoreRepository
import com.mobichill.justconcentration.viewmodel.SessionViewModel.Companion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map // Import map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    application: Application,
    private val firestoreRepository: FirestoreRepository, // Keep FirestoreRepository
    val subscriptionManager: SubscriptionManager,
    private val proBadgeManager: ProBadgeManager
) : AndroidViewModel(application) {

    companion object {
        private val TAG = SubscriptionViewModel::class.java.simpleName
    }

    val productDetailsList: StateFlow<List<ProductDetails>> = subscriptionManager.productDetailsFlow
    val purchaseEvent: StateFlow<SubscriptionManager.PurchaseEvent?> =
        subscriptionManager.purchaseEventFlow

    private val _isUserPro = MutableLiveData<Boolean>()
    val isUserPro: LiveData<Boolean> = _isUserPro

    init {
        Log.d(TAG, "SubscriptionViewModel init block START")
        subscriptionManager.startConnection() // Start BillingClient connection

        // Observing Firestore  user's status changes
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                firestoreRepository.getUserModelFlow(userId)
                    .map { userModel ->
                        val topLevelStatus = userModel?.subscriptionStatus == true
                        val expiryDateMillis = userModel?.subscriptionDetails?.expiryDateMillis ?: 0L
                        val isActive = expiryDateMillis > System.currentTimeMillis()
                        topLevelStatus && isActive
                    }
                    .collect { isPro ->
                        // This block runs whenever the Firestore document changes or initially
                        Log.d(TAG, "User isPro status updated from Firestore flow: $isPro")
                        _isUserPro.postValue(isPro) // Update the LiveData
                    }
            } else {
                Log.w(TAG, "User not logged in, cannot observe Firestore status.")
                _isUserPro.postValue(false)
            }
        }

        Log.d(TAG, "SubscriptionViewModel init block END")
    }

    fun refreshSubscriptionStatus() {
        Log.d(TAG, "Manual subscription status refresh requested.")
        subscriptionManager.queryUserPurchases()

        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
               firestoreRepository.fetchUserFromFirestore(userId)
            } else {
                Log.w(TAG, "User not logged in, cannot refresh status from Firestore.")
            }
        }
    }

    fun consumePurchaseEvent() {
        subscriptionManager.consumePurchaseEvent()
    }

    fun onProSubscriptionActivated() {
        viewModelScope.launch {
            Log.d(TAG, "SessionViewModel: Pro subscription activated. Awarding supporter badge.")
            proBadgeManager.awardProSupporterBadge()
            // Trigger check for loyalist badges too, as their start date might now be set
            proBadgeManager.checkAndUpdateLoyalistBadges()

            // Important: Re-check and update _isUserPro LiveData
            // as the underlying Firestore data should now reflect Pro status.
            val proStatus = firestoreRepository.isUserPro() // Re-fetch from repository
            _isUserPro.postValue(proStatus)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel onCleared(). Ending BillingClient connection.")
        subscriptionManager.endConnection()
    }
}