package com.mobichill.justconcentration.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobichill.justconcentration.util.Utils
import com.mobichill.justconcentration.databinding.TaskItemBinding
import com.mobichill.justconcentration.model.TaskModel


class TaskViewHolder(private val binding: TaskItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(taskModel: TaskModel, onItemClick: (TaskModel) ->  Unit) {
        binding.tvDesc.text = taskModel.taskText
        binding.tvTime.text = Utils.convertTimeMillisIntoText(taskModel.alarmTimeMillis)
        binding.btnAction.setOnClickListener{onItemClick(taskModel)}
    }

    companion object {
        fun from(parent: ViewGroup): TaskViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = TaskItemBinding.inflate(layoutInflater, parent, false)
            return TaskViewHolder(binding)
        }
    }
}
