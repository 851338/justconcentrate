package com.mobichill.justconcentration.listener

import com.mobichill.justconcentration.model.TaskModel

interface SelectionListener {
    fun onItemClick(task: TaskModel, position: Int)
    fun onItemLongClick(task: TaskModel, position: Int): Boolean
    fun isTaskSelected(task: TaskModel): Boolean
    fun isActionModeActive(): Boolean
}