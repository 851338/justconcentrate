package com.mobichill.justconcentration.view.viewholder // Make sure your package is correct

import android.content.Context
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

    // Define Offer Types for clarity and sorting priority (Add this enum outside the class or here)
    enum class OfferType(val priority: Int) {
        // Lower priority means it sorts earlier
        TRIAL(0), // Offers starting with a free trial
        DISCOUNTED_INTRO(1), // Offers starting with a discounted intro phase
        STANDARD(2), // Offers starting with a standard paid phase
        UNKNOWN(3) // Unknown or malformed offers last
    }

    // Helper to map billing periods to human-readable durations
    private fun formatBillingPeriod(period: String): String {
        // Use context to get localized strings if needed, or keep simple
        return when (period) {
            "P1D" -> "Day"
            "P1W" -> "Week"
            "P1M" -> "Month"
            "P3M" -> "3 Months"
            "P6M" -> "6 Months"
            "P1Y" -> "Year"
            // Add other periods if necessary (e.g., P2W, P2M)
            else -> period // Return raw if not recognized
        }
    }

    // Helper to convert billing period string to a sortable integer value (e.g., approx. days)
    private fun billingPeriodToSortValue(period: String): Int {
        return when {
            period.startsWith("P") -> {
                // Extract the number and the period unit
                val numStr = period.substring(1, period.length - 1)
                val unitChar = period.last()
                val num = numStr.toIntOrNull() ?: 0

                when (unitChar) {
                    'D' -> num
                    'W' -> num * 7
                    'M' -> num * 30 // Approximate
                    'Y' -> num * 365 // Approximate
                    else -> Int.MAX_VALUE // Unknown period, treat as longest/last
                }
            }
            else -> Int.MAX_VALUE // Not in P<n>D/W/M/Y format
        }
    }

    // Helper to determine the type of offer *based on the first phase* for sorting
    private fun getOfferType(offerDetails: ProductDetails.SubscriptionOfferDetails): OfferType {
        val phases = offerDetails.pricingPhases.pricingPhaseList
        if (phases.isEmpty()) return OfferType.UNKNOWN

        val firstPhase = phases.first()

        // Check for Free Trial: Price 0, not infinite recurring
        if (firstPhase.priceAmountMicros == 0L && firstPhase.recurrenceMode != ProductDetails.RecurrenceMode.INFINITE_RECURRING) {
            return OfferType.TRIAL
        }

        // Check for Discounted Introductory Phase: Price > 0, Finite recurring
        // This checks if the *first* phase is a discounted intro
        if (firstPhase.priceAmountMicros > 0L && firstPhase.recurrenceMode == ProductDetails.RecurrenceMode.FINITE_RECURRING) {
            return OfferType.DISCOUNTED_INTRO
        }

        // Otherwise, assume it's a Standard Paid Plan (first phase is infinite or non-recurring paid)
        return OfferType.STANDARD
    }


    // Helper function to generate the FULL display text for the radio button
    private fun getOfferDisplayString(
        context: Context,
        offerDetails: ProductDetails.SubscriptionOfferDetails
    ): String {
        val offerType = getOfferType(offerDetails) // Determine type for prefix logic
        val phases = offerDetails.pricingPhases.pricingPhaseList

        // Safety check
        if (phases.isEmpty()) {
            return context.getString(R.string.n_a)
        }

        val descriptionBuilder = StringBuilder()

        // Iterate through all phases to build the description string
        for (i in phases.indices) {
            val phase = phases[i]

            // Add separator BEFORE subsequent phases
            if (i > 0) {
                descriptionBuilder.append("\n")
                descriptionBuilder.append(context.getString(R.string.phase_description_separator))
                descriptionBuilder.append(" ")            }

            // Append description based on phase type
            when {
                // Free trial phase (Price 0, Finite or Non-Recurring)
                phase.priceAmountMicros == 0L && phase.recurrenceMode != ProductDetails.RecurrenceMode.INFINITE_RECURRING -> {
                    val trialPeriod = formatBillingPeriod(phase.billingPeriod)
                    descriptionBuilder.append(context.getString(R.string.free_trial_for_period, trialPeriod))
                }
                // Discounted finite recurring phase (Price > 0, Finite Recurring)
                phase.priceAmountMicros > 0L && phase.recurrenceMode == ProductDetails.RecurrenceMode.FINITE_RECURRING -> {
                    val periodUnit = formatBillingPeriod(phase.billingPeriod)
                    val price = phase.formattedPrice
                    val count = phase.billingCycleCount
                    descriptionBuilder.append(context.getString(R.string.discounted_period_at_price, count, periodUnit, price))
                }
                // Standard infinite recurring phase (Price > 0, Infinite Recurring)
                phase.priceAmountMicros > 0L && phase.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING -> {
                    val periodUnit = formatBillingPeriod(phase.billingPeriod)
                    val price = phase.formattedPrice
                    descriptionBuilder.append(context.getString(R.string.period_price_standard, periodUnit, price))
                }
                // Handle other potential phase types (like NON_RECURRING paid phase)
                phase.priceAmountMicros > 0L && phase.recurrenceMode == ProductDetails.RecurrenceMode.NON_RECURRING -> {
                    val price = phase.formattedPrice
                    descriptionBuilder.append(context.getString(R.string.one_time_payment, price))
                }
                // Handle unexpected phase types/configurations
                else -> {
                    descriptionBuilder.append(context.getString(R.string.unknown_price_format))
                }
            }
        }

        val combinedPhaseText = descriptionBuilder.toString()

        // Add the prefix ONLY if the offer type is NOT TRIAL
        return if (offerType == OfferType.TRIAL) {
            // For TRIAL offers, just show the combined phase description
            combinedPhaseText
        } else {
            val basePlanId = offerDetails.basePlanId
            // Clean up basePlanId for the prefix display
            val planNamePartInPrefix = basePlanId
                .removePrefix("subscription-")
                .replace("month", " month")
                .replace("year", " year")
                .replace("day", " day")
                .replace("week", " week")
                .trim()

            val displayPlanName = planNamePartInPrefix.ifEmpty { basePlanId }

            // Combine prefix and phase description
            context.getString(R.string.subscription_plan_format_cleaned, displayPlanName, combinedPhaseText)
        }
    }


    // Helper function to compare offers for sorting
    private fun compareOffers(
        o1: ProductDetails.SubscriptionOfferDetails,
        o2: ProductDetails.SubscriptionOfferDetails
    ): Int {
        val type1 = getOfferType(o1) // Sort primarily by the type based on the FIRST phase
        val type2 = getOfferType(o2)

        val typeComparison = type1.priority.compareTo(type2.priority)
        if (typeComparison != 0) return typeComparison

        // If types are the same, sort by the duration and then price of the *FIRST* phase
        val phases1 = o1.pricingPhases.pricingPhaseList
        val phases2 = o2.pricingPhases.pricingPhaseList

        if (phases1.isEmpty() || phases2.isEmpty()) {
            // Should be covered by UNKNOWN type priority, but safety check
            return 0
        }

        val firstPhase1 = phases1.first()
        val firstPhase2 = phases2.first()

        // Sort by the duration of the FIRST phase
        val periodValue1 = billingPeriodToSortValue(firstPhase1.billingPeriod)
        val periodValue2 = billingPeriodToSortValue(firstPhase2.billingPeriod)

        val durationComparison = periodValue1.compareTo(periodValue2)
        if (durationComparison != 0) return durationComparison

        // If durations are the same, sort by price of the FIRST phase
        val price1 = firstPhase1.priceAmountMicros
        val price2 = firstPhase2.priceAmountMicros
        return price1.compareTo(price2)
    }


    fun bind(
        productDetails: ProductDetails,
        selectedOfferTokens: MutableMap<String, String?>,
        onSubscribeClicked: (productDetails: ProductDetails, offerToken: String) -> Unit
    ) = with(binding) {
        val context = root.context

        // Assuming format is "Actual Title (package name and other info)"
        val cleanedTitle = productDetails.title.substringBefore("(").trim()
        tvProductTitle.text =
            cleanedTitle.ifEmpty { productDetails.title } // Fallback if parsing fails

        tvProductDescription.text = productDetails.description

        rgOffers.removeAllViews()
        // Remove previous selection for this product when binding,
        // or ensure it defaults if the selected offer is no longer available
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
            btnSubscribe.text = context.getString(R.string.subscribe) // Reset button text

            // Sort the offers list
            val offersWithPhases =
                offers.filter { it.pricingPhases.pricingPhaseList.isNotEmpty() }
            // Sort by OfferType priority (based on first phase), then first phase duration, then first phase price
            val sortedOffers = offersWithPhases.sortedWith(::compareOffers)

            var firstOfferToken: String? = null

            sortedOffers.forEachIndexed { index, offerDetails ->
                val radioButton = RadioButton(context)

                // Use the helper to generate the full radio button text with cleanup
                radioButton.text = getOfferDisplayString(context, offerDetails)

                radioButton.tag = offerDetails.offerToken // Store offer token
                radioButton.id = View.generateViewId() // Use generateViewId for unique IDs

                rgOffers.addView(radioButton)

                // Select the first offer in the *sorted* list by default
                if (index == 0) {
                    radioButton.isChecked = true
                    firstOfferToken = offerDetails.offerToken
                }
            }

            // Store the token of the default selected offer *after* adding all buttons
            selectedOfferTokens[productDetails.productId] = firstOfferToken


            rgOffers.setOnCheckedChangeListener { group, checkedId ->
                // Check if checkedId is a valid view ID added by us (not -1 which happens on clear)
                if (checkedId != -1) {
                    val selectedRadioButton = group.findViewById<RadioButton>(checkedId)
                    selectedOfferTokens[productDetails.productId] =
                        selectedRadioButton.tag as? String
                } else {
                    // This case happens when removeAllViews is called or checked is cleared
                    selectedOfferTokens[productDetails.productId] = null
                }
                // Ensure button is only enabled if an offer is selected
                btnSubscribe.isEnabled = selectedOfferTokens[productDetails.productId] != null
            }

            // Initial state: button should be enabled if the default selection worked
            btnSubscribe.isEnabled = selectedOfferTokens[productDetails.productId] != null
        }

        btnSubscribe.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                val selectedToken = selectedOfferTokens[productDetails.productId]
                if (selectedToken != null) {
                    onSubscribeClicked(productDetails, selectedToken)
                } else {
                    Utils.showToast(context, context.getString(R.string.please_select_an_offer))
                }
            }
        })
    }


    // ... companion object ...
    companion object {
        fun from(parent: ViewGroup): ProductViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ItemProductDetailBinding.inflate(layoutInflater, parent, false)
            return ProductViewHolder(binding)
        }
    }
}