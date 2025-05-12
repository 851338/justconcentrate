package com.mobichill.justconcentration.view

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivitySubscriptionBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.manager.SubscriptionManager
import com.mobichill.justconcentration.view.adapter.ProductDetailsAdapter
import com.mobichill.justconcentration.viewmodel.SessionViewModel
import com.mobichill.justconcentration.viewmodel.SubscriptionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubscriptionActivity : BaseViewBindingActivity<ActivitySubscriptionBinding>() {
    override fun initViewBinding(): ActivitySubscriptionBinding =
        ActivitySubscriptionBinding.inflate(layoutInflater)

    private val subscriptionViewModel: SubscriptionViewModel by viewModels()

    private val sessionViewModel: SessionViewModel by viewModels()

    private lateinit var productDetailsAdapter: ProductDetailsAdapter

    private var refreshAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        initRefreshAnimation()
    }

    private fun setupRecyclerView() {
        productDetailsAdapter = ProductDetailsAdapter(emptyList()) { productDetails, offerToken ->
            Log.d(TAG, "Subscribe clicked for ${productDetails.productId} with token $offerToken")
            subscriptionViewModel.subscriptionManager.launchPurchaseFlow(
                this,
                productDetails,
                offerToken
            )
        }
        binding.rvProductDetails.apply {
            layoutManager = LinearLayoutManager(this@SubscriptionActivity)
            adapter = productDetailsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        )

        binding.btnRefreshStatus.setOnClickListener {
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    subscriptionViewModel.refreshSubscriptionStatus()
                    // Loading will be hidden by observers typically
                }
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    subscriptionViewModel.productDetailsList.collect { products ->
                        showLoading(false) // Assuming loading was for fetching products
                        if (products.isEmpty()) {
                            binding.tvNoProducts.visibility = View.VISIBLE
                            binding.rvProductDetails.visibility = View.GONE
                        } else {
                            binding.tvNoProducts.visibility = View.GONE
                            binding.rvProductDetails.visibility = View.VISIBLE
                            productDetailsAdapter.updateData(products)
                        }
                        Log.d(TAG, "Product details updated: ${products.size} items")
                    }
                }

                launch {
                    subscriptionViewModel.isUserPro.observe(this@SubscriptionActivity) { isPro ->
                        binding.tvSubscriptionStatus.text = if (isPro) "PRO User" else "Free User"
                        Log.d(TAG, "User pro status updated: $isPro")
                    }
                }

                launch {
                    subscriptionViewModel.purchaseEvent.collect { event ->
                        event?.let {
                            handlePurchaseEvent(it)
                            // Consider consuming the event in ViewModel or setting it to null after handling
                            // viewModel.consumePurchaseEvent() // Add a method in VM if needed
                        }
                    }
                }
            }
        }
    }

    private fun handlePurchaseEvent(event: SubscriptionManager.PurchaseEvent) {
        Log.d(TAG, "Purchase Event: $event")
        when (event) {
            is SubscriptionManager.PurchaseEvent.InProgress -> {
                showLoading(true, "Processing purchase...")
            }

            is SubscriptionManager.PurchaseEvent.PurchaseSuccess -> {
                showLoading(false)
                showSnackbar("Purchase successful! Order ID: ${event.purchase.orderId}")
                subscriptionViewModel.refreshSubscriptionStatus() // Refresh status after purchase
                sessionViewModel.onProSubscriptionActivated()
            }

            is SubscriptionManager.PurchaseEvent.PurchaseFailure -> {
                showLoading(false)
                showSnackbar("Purchase failed: ${event.message ?: event.billingResult.debugMessage} (Code: ${event.billingResult.responseCode})")
            }

            is SubscriptionManager.PurchaseEvent.PurchaseCancelled -> {
                showLoading(false)
                showSnackbar("Purchase cancelled by user.")
            }

            is SubscriptionManager.PurchaseEvent.AlreadyOwned -> {
                showLoading(false)
                showSnackbar("You already own this subscription.")
                subscriptionViewModel.refreshSubscriptionStatus()
            }

            is SubscriptionManager.PurchaseEvent.AcknowledgmentSuccess -> {
                // This might be too noisy for user, good for logs
                Log.i(TAG, "Purchase Acknowledgment successful.")
                // Optionally show a subtle confirmation or just rely on pro status update
            }

            is SubscriptionManager.PurchaseEvent.AcknowledgmentFailure -> {
                showLoading(false)
                showSnackbar("Failed to acknowledge purchase. Please contact support. (Code: ${event.billingResult.responseCode})")
            }

            is SubscriptionManager.PurchaseEvent.PurchaseErrorGeneric -> {
                showLoading(false)
                showSnackbar("An error occurred. Please try again.")
            }
        }
    }

    private fun showLoading(isLoading: Boolean, message: String? = null) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvLoadingMessage.apply {
            text = message
            visibility = if (isLoading && message == null) View.VISIBLE else View.GONE
        }

        if (isLoading && message != null) startRefreshAnimation() else stopRefreshAnimation()
    }

    private fun initRefreshAnimation() {
        refreshAnimator =
            ObjectAnimator.ofFloat(binding.btnRefreshStatus, View.ROTATION, 0f, 360f).apply {
                duration = 1000 // 1 second for a full rotation
                repeatCount = ValueAnimator.INFINITE // Rotate indefinitely
                interpolator = LinearInterpolator() // Smooth, constant speed rotation
            }
    }

    private fun startRefreshAnimation() {
        binding.btnRefreshStatus.isClickable = false // Disable clicking during animation
        if (refreshAnimator?.isRunning == false) {
            refreshAnimator?.start()
        }
    }

    private fun stopRefreshAnimation() {
        refreshAnimator?.cancel()
        binding.btnRefreshStatus.rotation = 0f // Reset rotation
        binding.btnRefreshStatus.isClickable = true // Re-enable clicking
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.coordinatorLayout, message, Snackbar.LENGTH_LONG).show()
    }
}