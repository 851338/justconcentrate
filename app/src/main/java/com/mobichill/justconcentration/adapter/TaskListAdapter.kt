package com.mobichill.justconcentration.adapter

import android.view.ViewGroup
import com.mobichill.justconcentration.base.BaseAdapter
import com.mobichill.justconcentration.model.TaskModel

class TaskListAdapter(
    private var taskList: List<TaskModel>,
    private val onItemClick: (TaskModel) -> Unit)
    : BaseAdapter<TaskModel, TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        return TaskViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val taskModel = taskList[position]
        holder.onBind(taskModel, onItemClick)
    }

    override fun getItemCount() = taskList.size
}