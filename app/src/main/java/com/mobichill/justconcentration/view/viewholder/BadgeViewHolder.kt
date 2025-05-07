package com.mobichill.justconcentration.view.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.databinding.ItemBadgeBinding
import com.mobichill.justconcentration.model.BadgeModel
import com.mobichill.justconcentration.listener.OnSingleClickListener

class BadgeViewHolder(private val binding: ItemBadgeBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun onBind(
        badge: BadgeModel?,
        onItemClick: (BadgeModel) -> Unit
    ) = with(binding) {
        if (badge == null)
            return
        val actualResId = badge.getDrawableResourceId(root.context)
        imageBadgeIcon.setImageResource(actualResId)
        textBadgeName.text = badge.name
        textBadgeDescription.text = badge.description

        if (badge.isUnlocked) {
            root.alpha = 1.0f
            imageBadgeIcon.alpha = 1.0f
            layoutProgress.visibility = View.GONE
        } else {
            imageBadgeIcon.alpha = 0.4f
            binding.root.alpha = 0.7f
            if (badge.goal > 0) {
                layoutProgress.visibility = View.VISIBLE
                val progress = (badge.progress * 100) / badge.goal
                progressBadge.progress = progress
                binding.textProgress.text =
                    root.context.getString(R.string.progress_format, badge.progress, badge.goal)
            } else {
                layoutProgress.visibility = View.GONE
            }
        }
        imageProIndicator.visibility = if (badge.isPro) View.VISIBLE else View.GONE

        root.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    onItemClick(badge)
                }
            })
    }

    companion object {
        fun from(parent: ViewGroup): BadgeViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ItemBadgeBinding.inflate(layoutInflater, parent, false)
            return BadgeViewHolder(binding)
        }
    }
}