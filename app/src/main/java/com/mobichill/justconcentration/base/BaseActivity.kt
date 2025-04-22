package com.mobichill.justconcentration.base

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.mobichill.justconcentration.BuildConfig
import com.mobichill.justconcentration.helper.ThemeHelper


abstract class BaseActivity : AppCompatActivity(), BaseActivityListener {

    protected val TAG = javaClass.canonicalName
    private var timeStartOnCreate: Long = 0
    var isPaused = false
    private val isOverrideBackPressed = false

    @get:LayoutRes
    abstract val layoutId: Int
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.setTheme(context = this, isDarkMode = false)
        timeStartOnCreate = System.currentTimeMillis()
        super.onCreate(savedInstanceState)
        isPaused = false
        setTheme()
        setContentView(layoutId)
        initView()
        initData(intent = intent, isNewIntent = false)
        addListener()
    }

    override fun onStart() {
        super.onStart()
        isPaused = false
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        isPaused = false
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        isPaused = true
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        isPaused = true
        super.onStop()
    }

    override fun onDestroy() {
        isPaused = true
        Log.d(TAG, "onDestroy")
//        KeyboardUtils.fixSoftInputLeaks(this)
//        App.instance.listenerUtils.removerListener(this)
        super.onDestroy()
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart")
//        intent.printInfo("$TAG onRestart")
    }

    override fun recreate() {
        super.recreate()
        Log.d(TAG, "recreate")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
//        if (BuildConfig.DEBUG) Log.d("$TAG onConfigurationChanged newConfig = $newConfig")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.d(TAG, "onLowMemory")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "$TAG onTrimMemory level = $level")
        System.gc()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (BuildConfig.DEBUG)
            Log.d(
                TAG,
                "onActivityResult requestCode = $requestCode, resultCode = $resultCode, data = $data"
            )
//        data.printInfo(TAG)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.d(TAG, "onUserLeaveHint")
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")
        if (isOverrideBackPressed) {
//            finishWithCheckLastStack()
            return
        }
        super.onBackPressed()
    }

    override fun onFragmentAttached(tag: String) {
        Log.d(TAG, "onFragmentAttached $tag")
    }

    override fun onFragmentDetached(tag: String) {
        Log.d(TAG, "onFragmentDetached $tag")
    }

//    fun onNetworkStateChanged(isConnected: Boolean) {
//        Log.d(TAG, "onNetworkStateChanged $isConnected")
//    }

    private fun addListener() {
//        App.instance.listenerUtils.removerListener(this)
//        App.instance.listenerUtils.addListener(this)
    }

    override fun initData(intent: Intent?, isNewIntent: Boolean) {
//        intent.printInfo("$TAG onNewIntent $isNewIntent")
    }

    override fun initView() {}

    override fun setTheme() {
        //TODO override this function in other screen if needed, do not change this code
        Log.i(TAG, "setTheme")
//        StatusBarUtil.setLightMode(this)
        //BarUtils.setStatusBarColor(window, Color.WHITE)
    }
}

interface BaseActivityListener {
    fun initData(intent: Intent?, isNewIntent: Boolean)
    fun initView()
    fun setTheme()
    fun onFragmentAttached(tag: String)
    fun onFragmentDetached(tag: String)
}