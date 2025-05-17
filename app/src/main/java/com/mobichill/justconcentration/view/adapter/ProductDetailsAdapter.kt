package com.mobichill.justconcentration.view.adapter

import android.view.ViewGroup
import com.android.billingclient.api.ProductDetails
import com.mobichill.justconcentration.base.BaseAdapter
import com.mobichill.justconcentration.view.viewholder.ProductViewHolder

class ProductDetailsAdapter(
    private val onSubscribeClicked: (productDetails: ProductDetails, offerToken: String) -> Unit
) : BaseAdapter<ProductDetails, ProductViewHolder>() {

    // To keep track of selected offer for each product
    private val selectedOfferTokens = mutableMapOf<String, String?>() // productId to offerToken

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ProductViewHolder.from(parent)

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val productDetails = getItem(position)
        if (productDetails != null) {
            holder.bind(productDetails, selectedOfferTokens, onSubscribeClicked)
        }
    }

    fun updateData(newProductDetailsList: List<ProductDetails>) {
        selectedOfferTokens.clear()
        updateItems(newProductDetailsList)
    }

}