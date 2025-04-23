package com.mobichill.justconcentration.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.databinding.TaskItemBinding
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.util.Utils

class TaskViewHolder(private val binding: TaskItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(
        taskModel: TaskModel?,
        onItemClick: (TaskModel) -> Unit,
        onDelete: (TaskModel) -> Unit
    ) {
        val context = binding.root.context
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
        if (taskModel.alarmTimeMillis == 0L)
            binding.tvAlarm.text =context.getString(R.string.no_alarm_set)
        else binding.tvAlarm.text = context.getString(
            R.string.alarm,
            Utils.convertTimeMillisIntoText(context, taskModel.alarmTimeMillis)
        )
        binding.tvCreatedAt.text = context.getString(
            R.string.created_at,
            Utils.convertTimeMillisIntoText(context, taskModel.createdAt)
        )
        binding.tvDesc.text = taskModel.taskText
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
