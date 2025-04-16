package com.mobichill.justconcentration.base

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding

abstract class BaseViewBindingActivity<VB : ViewBinding> : BaseActivity() {
    override val layoutId: Int = 0
//    lateinit var viewModel: VM
    val binding: VB by lazy {
        initViewBinding()
    }

//    private fun getViewModelClass(): Class<VM> {
//        val type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1]
//        return type as Class<VM>
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //cover status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        //padding navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
    }

    protected abstract fun initViewBinding(): VB

    open fun initViewModel() {
//        viewModel = ViewModelProvider(this).get(getViewModelClass())
//        if (viewModel is BaseViewModel<*>) {
//            (viewModel as BaseViewModel<*>).let {
//                it.activity = this
//            }
//        }
    }

    override fun setContentView(layoutResID: Int) {
        initViewModel()
        setContentView(binding.root)
    }
}