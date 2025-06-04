package com.mobichill.justconcentration.model

data class SubscriptionDetails(
    var productId: String? = null,
    var purchaseToken: String? = null,
    var expiryDateMillis: Long? = null,
    var startDateMillis: Long? = null
)