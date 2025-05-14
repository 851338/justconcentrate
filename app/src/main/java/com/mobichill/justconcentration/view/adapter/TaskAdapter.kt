package com.mobichill.justconcentration.view.adapter

import android.view.ViewGroup
import com.mobichill.justconcentration.base.BaseAdapter
import com.mobichill.justconcentration.listener.OnItemDismissListener
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.listener.OnMenuActionListener
import com.mobichill.justconcentration.listener.SelectionListener
import com.mobichill.justconcentration.view.viewholder.TaskViewHolder

class TaskAdapter(
    private val selectionListener: SelectionListener,
    private val itemDismissListener: OnItemDismissListener?,
    private val menuActionListener: OnMenuActionListener?
) : BaseAdapter<TaskModel, TaskViewHolder>() {

    val currentList: List<TaskModel>
        get() = getCurrentItems()

    override fun compareDiffUtil(oldItem: TaskModel, newItem: TaskModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TaskViewHolder.from(parent)

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val taskModel = getItem(position) ?: return
        holder.onBind(taskModel, selectionListener, menuActionListener)
    }

    fun onItemDismiss(position: Int) {
        val task = getItem(position)
        if (task != null) {
            itemDismissListener?.onTaskDeleted(task) // Delegate swipe delete action to Activity
        }
    }
}