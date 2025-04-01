package com.mobichill.justconcentration.base

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