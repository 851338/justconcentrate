package com.mobichill.justconcentration.adapter

import android.view.ViewGroup
import com.mobichill.justconcentration.base.BaseAdapter
import com.mobichill.justconcentration.listener.OnItemDismissListener
import com.mobichill.justconcentration.model.TaskModel

class TaskListAdapter(
    private val onItemClick: (TaskModel) -> Unit,
    private val onDelete: (TaskModel) -> Unit,
    private val listener: OnItemDismissListener?
) : BaseAdapter<TaskModel, TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TaskViewHolder.from(parent)

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.onBind(getItem(position), onItemClick, onDelete)
    }

    fun onItemDismiss(position: Int) {
        listener?.onTaskDeleted(getItem(position))
        removeItem(position)
    }
}