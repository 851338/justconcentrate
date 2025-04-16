package com.mobichill.justconcentration.listener

import com.mobichill.justconcentration.model.TaskModel

interface OnItemDismissListener {
    fun onTaskDeleted(task: TaskModel)
}
