package com.mobichill.justconcentration.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment(), BaseFragmentListener {
    protected val TAG = javaClass.simpleName
    var isVisibleToUser: Boolean = false
    private var timeStartOnCreate: Long = 0

    @get:LayoutRes
    abstract val layoutId: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        arguments.printInfo("$TAG onViewCreated")
        initData()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
        printTimeOnCreated()
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        isVisibleToUser = true
        Log.i(TAG, "onResume")
    }

    override fun onPause() {
        Log.i(TAG, "onPause")
        isVisibleToUser = false
        super.onPause()
    }

    override fun onDestroyView() {
        Log.i(TAG, "onDestroyView")
//        App.instance.listenerUtils.removerListener(this)
        super.onDestroyView()
    }

    override fun onDestroy() {
        //Log.i("$TAG onDestroy")
        super.onDestroy()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.i(TAG, "onAttach")
        timeStartOnCreate = System.currentTimeMillis()
        if (context is BaseActivity) {
            if (TAG != null) {
                context.onFragmentAttached(TAG)
            }
        }
    }

    override fun onDetach() {
        //Log.i("$TAG onDetach")
        if (activity is BaseActivity) {
            if (TAG != null) {
                (activity as BaseActivity).onFragmentDetached(TAG)
            }
        }
        super.onDetach()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        if (BuildConfig.DEBUG) Log.i("$TAG onActivityResult requestCode = $requestCode, resultCode = $resultCode, data = $data")
    }

    private fun printTimeOnCreated() {
        Log.e(TAG, "onCreate takes ${System.currentTimeMillis() - timeStartOnCreate} ms")
    }

//    fun addListener() {
//        App.instance.listenerUtils.removerListener(this)
//        App.instance.listenerUtils.addListener(this)
//    }

    open fun isCanShowDialog(): Boolean {
        return activity != null && !requireActivity().isFinishing && isAdded
    }
}

interface BaseFragmentListener {
    fun initData()
    fun initView()
}