package com.mobichill.justconcentration.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobichill.justconcentration.databinding.TaskItemBinding
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.util.OnSingleClickListener
import com.mobichill.justconcentration.util.Utils


class TaskViewHolder(private val binding: TaskItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(
        taskModel: TaskModel,
        onItemClick: (TaskModel) -> Unit,
        isActiveList: Boolean,
        onDeleteOrRestore: (TaskModel) -> Unit
    ) {
        val isDelete = isActiveList

        binding.restoreButton.visibility = if (isDelete) View.GONE else View.VISIBLE
        binding.deleteButton.visibility = if (isDelete) View.VISIBLE else View.GONE

        val button = if (isDelete) binding.deleteButton else binding.restoreButton
        button.setOnClickListener {
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    onDeleteOrRestore(taskModel)
                }
            }
        }
        binding.tvDesc.text = taskModel.taskText
        binding.tvTime.text = Utils.convertTimeMillisIntoText(taskModel.alarmTimeMillis)
        binding.root.setOnClickListener {
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    onItemClick(taskModel)
                }
            }
        }
    }

    companion object {
        fun from(parent: ViewGroup): TaskViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = TaskItemBinding.inflate(layoutInflater, parent, false)
            return TaskViewHolder(binding)
        }
    }
}
