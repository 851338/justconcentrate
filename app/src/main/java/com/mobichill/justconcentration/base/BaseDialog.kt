package com.mobichill.justconcentration.base

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.viewbinding.ViewBinding
import com.mobichill.justconcentration.R


abstract class BaseDialog<T : ViewBinding>(context: Context) :
    Dialog(context, R.style.full_screen_dialog) {
    lateinit var binding: T

    private fun getInflatedLayoutNew(inflater: LayoutInflater): View {
        binding = setBinding(inflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getInflatedLayoutNew(layoutInflater))
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        initView()
        listener()
        hideNavigationBar()
    }

    protected fun updateUi(actionChangeUI: () -> Unit) {
        if (isShowing) {
            actionChangeUI()
        } else {
            setOnShowListener {
                actionChangeUI()
            }
        }
    }

    protected open fun hideNavigationBar() {

        val decorView = window?.decorView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API level 30) and above
            decorView?.windowInsetsController?.let { controller ->
                controller.hide(WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Below Android 11
            decorView?.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )

            // Listener để ẩn lại thanh điều hướng khi người dùng tương tác
            decorView?.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    Handler().postDelayed({
                        decorView.systemUiVisibility = (
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                )
                    }, 3000)
                }
            }
        }
    }

    abstract fun setBinding(layoutInflater: LayoutInflater): T

    abstract fun initView()
    abstract fun listener()

}