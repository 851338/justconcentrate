package com.mobichill.justconcentration.view.viewholder

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.databinding.ItemTaskBinding
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.utils.ConvertUtils
import com.mobichill.justconcentration.listener.OnMenuActionListener
import com.mobichill.justconcentration.listener.OnSingleClickListener

class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
    private val TAG = javaClass.simpleName

    fun onBind(
        taskModel: TaskModel?,
        onItemClick: (TaskModel) -> Unit,
        menuListener: OnMenuActionListener?
    ) {
        val context = binding.root.context
        if (taskModel == null)
            return

        binding.doneView.visibility = if(taskModel.completed) View.VISIBLE else View.GONE
        if (taskModel.alarmTimeMillis == 0L)
            binding.tvAlarm.text = context.getString(R.string.no_alarm_set)
        else binding.tvAlarm.text = context.getString(
            R.string.alarm,
            ConvertUtils.convertTimeMillisIntoText(context, taskModel.alarmTimeMillis)
        )
        binding.tvCreatedAt.text = context.getString(
            R.string.created_at,
            ConvertUtils.convertTimeMillisIntoText(context, taskModel.createdAt)
        )
        binding.tvDesc.text = taskModel.taskText
        binding.root.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    onItemClick(taskModel)
                }
            }
        )
        binding.imageViewMenu.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    showPopupMenu(binding.imageViewMenu, taskModel, menuListener)
                }
            }
        )
    }

    private fun showPopupMenu(view: View, task: TaskModel, menuListener: OnMenuActionListener?) {
        val popup = PopupMenu(view.context, view)
        popup.inflate(R.menu.menu_item_task)

        // Use reflection to force icons to show
        try {
            val fields = popup.javaClass.declaredFields
            for (field in fields) {
                if (field.name == "mPopup") {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popup)
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons =
                        classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "setForceShowIcon: ", e)
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_mark_done -> {
                    menuListener?.onMarkDone(task)
                    true
                }

                R.id.action_delete -> {
                    menuListener?.onDelete(task)
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    companion object {
        fun from(parent: ViewGroup): TaskViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ItemTaskBinding.inflate(layoutInflater, parent, false)
            return TaskViewHolder(binding)
        }
    }
}