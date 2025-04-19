package com.mobichill.justconcentration.popup

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.databinding.DialogUserProfileBinding
import com.mobichill.justconcentration.helper.RoomHelper
import com.mobichill.justconcentration.repository.RoomRepository
import com.mobichill.justconcentration.util.OnSingleClickListener
import com.mobichill.justconcentration.util.Utils
import com.mobichill.justconcentration.view.HomeActivity
import com.mobichill.justconcentration.view.WelcomeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserPopup(private val context: Context) {
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
        if (Utils.isUserLoggedIn(context)) {
            binding.tvLogout.visibility = View.VISIBLE
            val uid = Utils.getUserIdFromSF(context)
            CoroutineScope(Dispatchers.IO).launch {
                val user = RoomRepository(RoomHelper.getInstance(context)).getUserById(uid)
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
                    if (!Utils.isNetworkAvailable(context))
                        Utils.showToast(context, context.getString(R.string.no_internet_connection))
                    else {
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
                        context.openAboutFragment()
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
                        Utils.saveLoginState(context, false)
                        Utils.showToast(context, context.getString(R.string.logged_out))
                        popupWindow.dismiss()
                        if (context is HomeActivity)
                            context.setUIAfterLogout()
                        binding.tvLogout.visibility = View.GONE
                        Utils.clearUserInfoPref(context)
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
