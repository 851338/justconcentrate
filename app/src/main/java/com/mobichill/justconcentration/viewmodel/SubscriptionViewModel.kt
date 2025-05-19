package com.mobichill.justconcentration.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.manager.SubscriptionManager
import com.mobichill.justconcentration.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    application: Application,
    private val firestoreRepository: FirestoreRepository,
    val subscriptionManager: SubscriptionManager
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
        subscriptionManager.startConnection()
        // Observe userPurchases to update isUserPro status

        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                firestoreRepository.getUserModelFlow(userId)
                    .map { userModel -> // Map the UserModel to the 'isPro' boolean
                        userModel?.subscriptionStatus ?: false
                    }
                    .collect { isPro ->
                        Log.d(TAG, "User isPro status updated from Firestore: $isPro")
                        _isUserPro.postValue(isPro) // Update the LiveData on the main thread
                    }
            } else {
                Log.w(TAG, "User not logged in, cannot observe Firestore status.")
                _isUserPro.postValue(false)
            }
        }
        // Query purchases on init to get current status
        // The SubscriptionManager already calls queryUserPurchases on setup
    }

    fun refreshSubscriptionStatus() {
        Log.d(TAG, "Refresh subscription status requested.")
        subscriptionManager.queryUserPurchases()
    }

    fun consumePurchaseEvent() {
        subscriptionManager.consumePurchaseEvent()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel onCleared(). Ending BillingClient connection.")
        subscriptionManager.endConnection()
    }
}