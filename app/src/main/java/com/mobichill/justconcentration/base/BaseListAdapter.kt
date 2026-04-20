package com.mobichill.justconcentration.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.viewbinding.ViewBinding

abstract class BaseListAdapter<T, VB : ViewBinding>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseViewHolder<T, VB>>(diffCallback) {

    abstract fun createBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): VB
    abstract fun setData(binding: VB, item: T, position: Int)
    open fun bindViews(binding: VB, item: T, position: Int) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T, VB> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = createBinding(inflater, parent, viewType)

        return object : BaseViewHolder<T, VB>(binding) {
            override fun onBindData(data: T, position: Int): Boolean {
                bindViews(binding, data, position)
                setData(binding, data, position)
                this.data = data
                return true
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<T, VB>, position: Int) {
        holder.onBindData(getItem(position), position)
    }
}
