package com.mobichill.justconcentration.api

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class BackendValidationResponse(
    val success: Boolean,
    val message: String,
    val isPro: Boolean? = null, // Backend might return this
    val expiryDateMillis: Long? = null, // Backend MUST return this
    val details: @Contextual Any? = null // If you return raw Google details
)