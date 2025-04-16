package com.mobichill.justconcentration.base

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.util.Calendar
import java.util.Locale


abstract class BaseActivity : AppCompatActivity(), BaseActivityListener {

    protected val TAG = javaClass.canonicalName
    private var timeStartOnCreate: Long = 0
    var isPaused = false
    var isOverrideBackPressed = false

    @get:LayoutRes
    abstract val layoutId: Int
    override fun onCreate(savedInstanceState: Bundle?) {
//        ThemeHelper.applyTheme(this) // Apply theme before UI loads
        timeStartOnCreate = System.currentTimeMillis()
        super.onCreate(savedInstanceState)
        isPaused = false
        setTheme()
        setContentView(layoutId)
        initView()
        initData(intent = intent, isNewIntent = false)
        addListener()
        setTransitionOnCreate()
        applyThemeBasedOnTime()
        //
        val res: Resources = resources
        val dm: DisplayMetrics = res.displayMetrics
        val conf = res.configuration
        conf.setLocale(Locale("lo".toLowerCase(Locale.ROOT))) // API 17+ only.
        res.updateConfiguration(conf, dm)
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

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")
        if (isOverrideBackPressed) {
//            finishWithCheckLastStack()
            return
        }
        super.onBackPressed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
//        if (BuildConfig.DEBUG) Log.d("$TAG onConfigurationChanged newConfig = $newConfig")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.d(TAG,"onLowMemory")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG,"$TAG onTrimMemory level = $level")
        System.gc()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        if (BuildConfig.DEBUG) Log.d("$TAG onActivityResult requestCode = $requestCode, resultCode = $resultCode, data = $data")
//        data.printInfo(TAG)
    }

    private fun printTimeOnCreated() {
        Log.e(TAG, " onCreate takes ${System.currentTimeMillis() - timeStartOnCreate} ms")
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.d(TAG, "onUserLeaveHint")
    }

    override fun finish() {
        super.finish()
        setTransitionOnFinnish()
    }

    override fun onFragmentAttached(tag: String) {
        Log.d(TAG, "onFragmentAttached $tag")
    }

    override fun onFragmentDetached(tag: String) {
        Log.d(TAG, "onFragmentDetached $tag")
    }

    fun onNetworkStateChanged(isConnected: Boolean) {
        Log.d(TAG, "onNetworkStateChanged $isConnected")
    }

    fun dp(value: Int): Int {
        return (value * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun addListener() {
//        App.instance.listenerUtils.removerListener(this)
//        App.instance.listenerUtils.addListener(this)
    }

    override fun initData(intent: Intent?, isNewIntent: Boolean) {
//        intent.printInfo("$TAG onNewIntent $isNewIntent")
    }

    override fun initView() {

    }

    override fun setTheme() {
        //TODO override this function in other screen if needed, do not change this code
        Log.i(TAG, "setTheme")
//        StatusBarUtil.setLightMode(this)
        //BarUtils.setStatusBarColor(window, Color.WHITE)
    }

    override fun setTransitionOnCreate() {
//        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        printTimeOnCreated()
    }

    override fun setTransitionOnFinnish() {
//        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    fun setWindowFlag(bits: Int, on: Boolean) {
        val win = window
        val winParams = win.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }

    open fun isCanShowDialog(): Boolean {
        return !this.isFinishing
    }

    private fun applyThemeBasedOnTime() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) // Get current hour (0-23)

        val isNight = hour >= 18 || hour < 6  // Night mode from 6 PM to 6 AM
        val mode = if (isNight) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO

        AppCompatDelegate.setDefaultNightMode(mode)  // Apply the theme
    }
}

interface BaseActivityListener {
    fun initData(intent: Intent?, isNewIntent: Boolean)
    fun initView()
    fun setTheme()
    fun setTransitionOnCreate()
    fun setTransitionOnFinnish()
    fun onFragmentAttached(tag: String)
    fun onFragmentDetached(tag: String)
}