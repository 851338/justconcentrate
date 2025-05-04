package com.mobichill.justconcentration.view.adapter

import android.view.ViewGroup
import com.mobichill.justconcentration.base.BaseAdapter
import com.mobichill.justconcentration.model.BadgeModel
import com.mobichill.justconcentration.view.viewholder.BadgeViewHolder

class BadgeAdapter(
    private val onItemClick: (BadgeModel) -> Unit,
) : BaseAdapter<BadgeModel, BadgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = BadgeViewHolder.from(parent)

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.onBind(getItem(position), onItemClick)
    }
}