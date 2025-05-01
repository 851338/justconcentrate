package com.mobichill.justconcentration.view.popup

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.application.MyApp
import com.mobichill.justconcentration.databinding.DialogUserProfileBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils
import com.mobichill.justconcentration.view.HomeActivity
import com.mobichill.justconcentration.view.WelcomeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserPopup(private val context: Context) {
    private val sfUtils: SharedPreferencesUtils by lazy {
        SharedPreferencesUtils(context.applicationContext)
    }
    private lateinit var popupWindow: PopupWindow
    private val binding: DialogUserProfileBinding =
        DialogUserProfileBinding.inflate(LayoutInflater.from(context))

    fun show(anchorView: View) {
        popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        //Get user name from local
        //if user logged in show name else turn name into login button
        if (sfUtils.isUserLoggedIn()) {
            binding.tvLogout.visibility = View.VISIBLE
            val uid = sfUtils.getUserId()
            CoroutineScope(Dispatchers.IO).launch {
                val user = MyApp.instance.userRepository.getUserById(uid)
                //Update UI here
                withContext(Dispatchers.Main) {
                    binding.tvName.text = context.getString(R.string.greeting, user?.name)
                }
            }
        } else {
            binding.tvLogout.visibility = View.GONE
            binding.tvName.text = context.getString(R.string.login)
            binding.tvName.setOnClickListener(
                object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        if (context is HomeActivity) {
                            context.startActivity(Intent(context, WelcomeActivity::class.java))
                        }
                    }
                }
            )
        }
        // Handle Clicks
        binding.tvSubscription.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    when {
                        !Utils.isNetworkAvailable(context) ->
                            Utils.showToast(context, context.getString(R.string.no_internet_connection))
                        !sfUtils.isUserLoggedIn() ->
                            Utils.showToast(context,
                                context.getString(R.string.you_must_log_in_first))
                        else ->
                            popupWindow.dismiss()
                        //TODO subscription
                    }
                }
            }
        )

        binding.tvAbout.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    if (context is HomeActivity)
                        context.openSettingsActivity()
                    popupWindow.dismiss()
                }
            }
        )

        binding.tvLogout.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    if (!Utils.isNetworkAvailable(context))
                        Utils.showToast(context, context.getString(R.string.no_internet_connection))
                    else {
                        FirebaseAuth.getInstance().signOut()
                        sfUtils.saveLoginState(false)
                        Utils.showToast(context, context.getString(R.string.logged_out))
                        popupWindow.dismiss()
                        if (context is HomeActivity)
                            context.setUIAfterLogout()
                        binding.tvLogout.visibility = View.GONE
                        sfUtils.logout()
                        //TODO reset subscription variable
                    }
                }
            }
        )

        // Show the popup
        popupWindow.elevation = 10f
        popupWindow.showAsDropDown(anchorView, 0, 10, Gravity.END)
    }
}
