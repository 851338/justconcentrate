package com.mobichill.justconcentration.view

import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobichill.justconcentration.adapter.TaskListAdapter
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityTaskBinding
import com.mobichill.justconcentration.factory.TaskViewModelFactory
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.viewmodel.TaskViewModel

class TaskActivity : BaseViewBindingActivity<ActivityTaskBinding>() {
    //    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskListAdapter

    override fun initViewBinding(): ActivityTaskBinding =
        ActivityTaskBinding.inflate(layoutInflater)

    override fun initView(): Unit = with(binding) {
        val taskViewModel: TaskViewModel by viewModels {
            TaskViewModelFactory(this@TaskActivity)
        }

        btnBack.setOnClickListener { onBackPressed() }
        fabTask.setOnClickListener { openNewOrEditTaskFragment(null) }

        binding.recyclerView.layoutManager = LinearLayoutManager(this@TaskActivity)
        taskAdapter = TaskListAdapter(emptyList()) { taskModel ->
            run {
                openNewOrEditTaskFragment(taskModel)
            }
        }
        binding.recyclerView.adapter = taskAdapter

        taskViewModel.allTasks.observe(this@TaskActivity) { tasks ->
            taskAdapter.updateItems(tasks)
        }
        taskViewModel.syncTasks()
        super.initView()
    }

    private fun openNewOrEditTaskFragment(taskModel: TaskModel?) {
        val newOrEditTaskFragment = NewOrEditTaskFragment().apply {
            arguments = Bundle().apply {
                putParcelable("task_key", taskModel) // Pass task to open editor
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentAddTask.id, newOrEditTaskFragment)
            .addToBackStack(null)
            .commit()
    }
}