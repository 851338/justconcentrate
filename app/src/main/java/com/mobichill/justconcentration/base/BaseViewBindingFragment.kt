package com.mobichill.justconcentration.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

abstract class BaseViewBindingFragment<VB : ViewBinding> : BaseFragment() {
    override val layoutId: Int = 0
//    lateinit var viewModel: VM

    val binding: VB by lazy {
        initViewBinding()
    }
//
//    private fun getViewModelClass(): Class<VM> {
//        val type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1]
//        return type as Class<VM>
//    }

    protected abstract fun initViewBinding(): VB
    open fun initViewModel() {
//        viewModel = ViewModelProvider(this).get(getViewModelClass())
//        if (viewModel is BaseViewModel<*>) {
//            (viewModel as BaseViewModel<*>).let {
//                it.activity = activity
//            }
//        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        initViewModel()
        return binding.root
    }

    override fun isCanShowDialog(): Boolean {
        return activity != null && !requireActivity().isFinishing && isAdded //&& ::viewModel.isInitialized
    }
}
