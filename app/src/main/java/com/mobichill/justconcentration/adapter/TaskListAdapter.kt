package com.mobichill.justconcentration.adapter

import android.view.ViewGroup
import com.mobichill.justconcentration.base.BaseAdapter
import com.mobichill.justconcentration.listener.OnItemDismissListener
import com.mobichill.justconcentration.model.TaskModel

class TaskListAdapter(
    private var taskList: MutableList<TaskModel>,
    private val onItemClick: (TaskModel) -> Unit,
    private val onDeleteOrRestore: (TaskModel) -> Unit,
    private val listener: OnItemDismissListener?,
    private var isActiveList: Boolean
) : BaseAdapter<TaskModel, TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        return TaskViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val taskModel = taskList[position]
        holder.onBind(taskModel, onItemClick, isActiveList, onDeleteOrRestore)
    }

    override fun getItemCount() = taskList.size

    fun onItemDismiss(position: Int) {
        listener?.onTaskDeleted(taskList[position])
        taskList.removeAt(position)
        notifyItemRemoved(position)
    }
}