package com.mobichill.justconcentration.view

import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.adapter.TaskListAdapter
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentDeletedTasksBinding
import com.mobichill.justconcentration.helper.TaskItemTouchHelper
import com.mobichill.justconcentration.listener.OnItemDismissListener
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.util.Utils
import com.mobichill.justconcentration.viewmodel.TaskViewModel

class DeletedTasksFragment : BaseViewBindingFragment<FragmentDeletedTasksBinding>() {
    override fun initViewBinding(): FragmentDeletedTasksBinding =
        FragmentDeletedTasksBinding.inflate(layoutInflater)

    private lateinit var taskAdapter: TaskListAdapter
    private val taskViewModel: TaskViewModel by activityViewModels()

    override fun initData() {
    }

    override fun onResume() {
        super.onResume()
        if (activity is TaskActivity)
                (activity as TaskActivity).setupToolbar("Deleted Tasks")
    }

    override fun initView() = with(binding) {
        recyclerView.layoutManager = LinearLayoutManager(context)
        taskAdapter = TaskListAdapter(
            emptyList<TaskModel>().toMutableList(),
            { taskModel ->
                run {} //do nothing
            },
            { taskModel ->
                run {
                    restoreTask(taskModel)
                }
            },
            onItemDismissListener,
            false
        )
        val itemTouchHelper = ItemTouchHelper(TaskItemTouchHelper(taskAdapter))
        itemTouchHelper.attachToRecyclerView(recyclerView)
        recyclerView.adapter = taskAdapter

        taskViewModel.allDeletedTasks.observe(requireActivity()) { tasks ->
            taskAdapter.updateItems(tasks)
        }
    }

    private fun restoreTask(taskModel: TaskModel) {
        taskViewModel.removeOrRestoreTaskWithRoom(taskModel, false)
        taskViewModel.removeOrRestoreTaskWithFireStore(taskModel, false) { complete, error ->
            if (complete) {
                Log.d(TAG, getString(R.string.task_restored))
            } else {
                Log.e(TAG, "Task restored: ${error?.message.orEmpty()}")
            }
        }

        Utils.showToast(requireContext(), getString(R.string.task_restored))
    }

    private val onItemDismissListener = object : OnItemDismissListener {
        override fun onTaskDeleted(task: TaskModel) {
            restoreTask(task)
        }
    }
}