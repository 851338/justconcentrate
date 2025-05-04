package com.mobichill.justconcentration.listener

import com.mobichill.justconcentration.model.TaskModel

interface OnMenuActionListener {
    fun onMarkDone(task: TaskModel)
    fun onDelete(task: TaskModel)
}