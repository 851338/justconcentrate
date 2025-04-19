package com.mobichill.justconcentration.base

import android.content.res.Resources
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.mobichill.justconcentration.base.dp.dp

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
        setPadding()
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

    private fun setPadding() {

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                24.dp + systemInsets.left,
                24.dp + systemInsets.top,
                24.dp + systemInsets.right,
                24.dp + systemInsets.bottom
            )
            insets
        }
    }
}

object dp {
    val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()
}
