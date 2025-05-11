package com.mobichill.justconcentration.view.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.ProductDetails
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.databinding.ItemProductDetailBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.utils.Utils

class ProductViewHolder(private val binding: ItemProductDetailBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        productDetails: ProductDetails,
        selectedOfferTokens: MutableMap<String, String?>,
        onSubscribeClicked: (productDetails: ProductDetails, offerToken: String) -> Unit
    ) = with(binding) {
        val context = root.context

        tvProductTitle.text = productDetails.title
        tvProductDescription.text = productDetails.description

        rgOffers.removeAllViews()
        selectedOfferTokens.remove(productDetails.productId)

        val offers = productDetails.subscriptionOfferDetails

        if (offers.isNullOrEmpty()) {
            tvOffersTitle.visibility = View.GONE
            rgOffers.visibility = View.GONE
            btnSubscribe.isEnabled = false // No offers, disable subscribe
            btnSubscribe.text = context.getString(R.string.no_offers_available)
        } else {
            tvOffersTitle.visibility = View.VISIBLE
            rgOffers.visibility = View.VISIBLE
            btnSubscribe.isEnabled = true
            btnSubscribe.text = context.getString(R.string.subscribe)

            offers.forEachIndexed { index, offerDetails ->
                val radioButton = RadioButton(context)
                // Construct offer text (e.g., "Monthly: $4.99")
                // Assuming one pricing phase for simplicity here, you might need more complex logic
                val price =
                    offerDetails.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice
                        ?: context.getString(R.string.n_a)
                val billingPeriod =
                    offerDetails.pricingPhases.pricingPhaseList.firstOrNull()?.billingPeriod
                        ?: ""
                val planName = offerDetails.basePlanId.replaceFirstChar { it.uppercaseChar() }
                val billingInfo = formatBillingPeriod(billingPeriod)
                radioButton.text =
                    context.getString(R.string.plan, planName, price, billingInfo)

                radioButton.tag = offerDetails.offerToken // Store offer token
                radioButton.id = View.generateViewId()

                rgOffers.addView(radioButton)

                if (index == 0) { // Select the first offer by default
                    radioButton.isChecked = true
                    // Store the token of the default selected offer
                    selectedOfferTokens[productDetails.productId] = offerDetails.offerToken
                }
            }

            rgOffers.setOnCheckedChangeListener { group, checkedId ->
                val selectedRadioButton = group.findViewById<RadioButton>(checkedId)
                selectedOfferTokens[productDetails.productId] =
                    selectedRadioButton.tag as? String
                btnSubscribe.isEnabled = selectedOfferTokens[productDetails.productId] != null
            }
            btnSubscribe.isEnabled = selectedOfferTokens[productDetails.productId] != null
        }

        btnSubscribe.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                val selectedToken = selectedOfferTokens[productDetails.productId]
                if (selectedToken != null) {
                    onSubscribeClicked(productDetails, selectedToken)
                } else {
                    // Should not happen if there are offers and one is pre-selected
                    Utils.showToast(context, context.getString(R.string.please_select_an_offer))
                }
            }
        })
    }


    private fun formatBillingPeriod(period: String): String {
        return when (period) {
            "P1W" -> "Week"
            "P1M" -> "Month"
            "P3M" -> "3 Months"
            "P6M" -> "6 Months"
            "P1Y" -> "Year"
            else -> period // raw format if not recognized
        }
    }

    companion object {
        fun from(parent: ViewGroup): ProductViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ItemProductDetailBinding.inflate(layoutInflater, parent, false)
            return ProductViewHolder(binding)
        }
    }
}