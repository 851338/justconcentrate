package com.mobichill.justconcentration.view.adapter

import android.view.ViewGroup
import com.mobichill.justconcentration.base.BaseAdapter
import com.mobichill.justconcentration.listener.OnItemDismissListener
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.listener.OnMenuActionListener
import com.mobichill.justconcentration.view.viewholder.TaskViewHolder

class TaskAdapter(
    private val onItemClick: (TaskModel) -> Unit,
    private val listener: OnItemDismissListener?,
    private val menuListener: OnMenuActionListener?

) : BaseAdapter<TaskModel, TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TaskViewHolder.from(parent)

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.onBind(getItem(position), onItemClick, menuListener)
    }

    fun onItemDismiss(position: Int) {
        listener?.onTaskDeleted(getItem(position))
        removeItem(position)
    }
}