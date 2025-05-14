package com.mobichill.justconcentration.helper

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.mobichill.justconcentration.view.adapter.TaskAdapter

class TaskItemTouchHelper(
    private val adapter: TaskAdapter,
    private val isActionModeActive: () -> Boolean
) : ItemTouchHelper.SimpleCallback(
    0, // No drag directions
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
) {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val swipeFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        val dragFlags = 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
//        adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Only allow swipe if action mode is NOT active
        if (!isActionModeActive()) {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                adapter.onItemDismiss(position) // Call adapter method
            }
        } else {
            // If swiped in action mode, don't perform dismissal
            // Notify adapter to redraw the item in its original position
            adapter.notifyItemChanged(viewHolder.adapterPosition)
        }
    }

    // Change swipe behavior/appearance when in action mode
    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        // Disable swipe directions if action mode is active
        return if (isActionModeActive()) 0 else super.getSwipeDirs(recyclerView, viewHolder)
    }
}