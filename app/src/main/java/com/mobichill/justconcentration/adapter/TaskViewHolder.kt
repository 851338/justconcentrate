package com.mobichill.justconcentration.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobichill.justconcentration.databinding.TaskItemBinding
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.util.Utils

class TaskViewHolder(private val binding: TaskItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(
        taskModel: TaskModel?,
        onItemClick: (TaskModel) -> Unit,
        onDelete: (TaskModel) -> Unit
    ) {
        if (taskModel == null)
            return
        binding.deleteButton.visibility = View.VISIBLE

        val button = binding.deleteButton
        button.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    onDelete(taskModel)
                }
            }
        )
        binding.tvDesc.text = taskModel.taskText
        binding.tvTime.text = Utils.convertTimeMillisIntoText(taskModel.alarmTimeMillis)
        binding.root.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    onItemClick(taskModel)
                }
            }
        )
    }

    companion object {
        fun from(parent: ViewGroup): TaskViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = TaskItemBinding.inflate(layoutInflater, parent, false)
            return TaskViewHolder(binding)
        }
    }
}
