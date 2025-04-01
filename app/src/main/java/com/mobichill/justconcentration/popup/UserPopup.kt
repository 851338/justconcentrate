package com.mobichill.justconcentration.popup

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.databinding.DialogUserProfileBinding
import com.mobichill.justconcentration.util.Utils

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
        //Get data from local
        binding.tvName.text = Utils.getUserNameFromSF(context)

        // Handle Clicks
        binding.tvSubscription.setOnClickListener {
            if (!Utils.isNetworkAvailable(context))
                Utils.showToast(context, context.getString(R.string.no_internet_connection))
            else {
                //TODO
                popupWindow.dismiss()
            }
        }

        binding.tvAbout.setOnClickListener {
            Utils.showToast(context, "About Clicked")
            popupWindow.dismiss()
        }

        binding.tvLogout.setOnClickListener {
            if (!Utils.isNetworkAvailable(context))
                Utils.showToast(context, context.getString(R.string.no_internet_connection))
            else {
                FirebaseAuth.getInstance().signOut()
                Utils.saveLoginState(context, false)
                Utils.showToast(context, context.getString(R.string.logged_out))
                Utils.clearUserInfoPref(context)
                popupWindow.dismiss()
            }
        }

        // Show the popup
        popupWindow.elevation = 10f
        popupWindow.showAsDropDown(anchorView, 0, 10, Gravity.END)
    }

    fun dismiss() {
        popupWindow.dismiss()
    }
}
