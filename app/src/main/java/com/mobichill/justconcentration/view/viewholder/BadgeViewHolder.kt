package com.mobichill.justconcentration.view.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
        val actualResId = badge.getDrawableResourceId(binding.root.context)
        imageBadgeIcon.setImageResource(actualResId)
        textBadgeName.text = badge.name
        textBadgeDescription.text = badge.description

        if (badge.isUnlocked) {
            itemView.alpha = 1.0f
            progressBadge.visibility = View.GONE
        } else {
            itemView.alpha = 0.5f
            if (badge.goal > 0) {
                progressBadge.visibility = View.VISIBLE
                val progress = (badge.progress * 100) / badge.goal
                progressBadge.progress = progress
            } else {
                progressBadge.visibility = View.GONE
            }
        }
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