package com.mobichill.justconcentration.base

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter<Model, ViewHolder : RecyclerView.ViewHolder> :
    RecyclerView.Adapter<ViewHolder>() {
    open val ITEM_EMPTY = -1
    open val ITEM_NORMAL = 0
    open val ITEM_OTHER = 1

    open var enableDiffUtil = false
    open fun compareDiffUtil(oldItem: Model, newItem: Model): Boolean = oldItem == newItem

    private var items = mutableListOf<Model>()

    fun getCurrentItems(): List<Model> {
        return items.toList() // Return a copy
    }
    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        return item?.hashCode()?.toLong() ?: System.currentTimeMillis()
    }

    fun getItem(position: Int): Model? {
        return if (position in 0 until itemCount) items[position] else null
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item == null) ITEM_EMPTY else ITEM_NORMAL
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateItem(position: Int, item: Model) {
        if (position in 0 until itemCount) {
            items[position] = item
            notifyItemChanged(position)
        }
    }

    fun updateItems(list: ArrayList<Model>?) {
        if (enableDiffUtil) {
            if (list != null) {
                if (list.isNotEmpty()) {
                    notifyWithDiffUtil(items, list) {
                        items.clear()
                        items.addAll(list)
                        notifyDataSetChanged()
                    }
                } else {
                    items.clear()
                    notifyDataSetChanged()
                }
            }
        } else {
            items.clear()
            if (list != null) {
                if (list.isNotEmpty()) items.addAll(list)
            }
            notifyDataSetChanged()
        }
    }

    fun updateItems(list: List<Model>?) {
        if (enableDiffUtil) {
            if (list != null) {
                if (list.isNotEmpty()) {
                    notifyWithDiffUtil(items, ArrayList(list)) {
                        items.clear()
                        items.addAll(list)
                        notifyDataSetChanged()
                    }
                } else {
                    items.clear()
                    notifyDataSetChanged()
                }
            }
        } else {
            items.clear()
            if (list != null) {
                if (list.isNotEmpty()) items.addAll(list)
            }
            notifyDataSetChanged()
        }
    }

    fun addItem(item: Model) {
        items.add(item)
        notifyItemInserted(itemCount - 1)
    }

    fun addItems(list: List<Model>?) {
        if (list != null) {
            if (list.isNotEmpty()) {
                val positionStart = items.size
                items.addAll(list!!)
                notifyItemRangeInserted(positionStart, itemCount)
            }
        }
    }

    fun addItems(list: ArrayList<Model>?) {
        if (list != null) {
            if (list.isNotEmpty()) {
                val positionStart = items.size
                items.addAll(list)
                notifyItemRangeInserted(positionStart, itemCount)
            }
        }
    }

    fun removeItem(position: Int) {
        if (position in 0 until itemCount) {
            items.removeAt(position)
            notifyItemRangeChanged(position, itemCount - 1)
        }
    }

    //Better performance than notifyDataSetChanged()
    private fun notifyWithDiffUtil(
        oldItems: List<Model>,
        newItems: List<Model>,
        updateData: () -> Unit
    ) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val result = compareDiffUtil(oldItems[oldItemPosition], newItems[newItemPosition])
                //Log.i("areItemsTheSame $oldItemPosition vs $newItemPosition: $result")
                return result
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val result = oldItems[oldItemPosition]?.equals(newItems[newItemPosition]) ?: false
                //Log.i("areContentsTheSame $oldItemPosition vs $newItemPosition: $result")
                return result
            }

            override fun getOldListSize() = oldItems.size

            override fun getNewListSize() = newItems.size

        })
        updateData()
        diff.dispatchUpdatesTo(this)
    }

    fun clear() {
        items.clear()
    }
}