package com.mobichill.justconcentration.model
import com.android.billingclient.api.ProductDetails

data class ProductItem(
    val title: String,
    val description: String,
    val price: String,
    val productDetails: ProductDetails
)
