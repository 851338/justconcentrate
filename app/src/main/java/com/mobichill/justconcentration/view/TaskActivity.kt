package com.mobichill.justconcentration.view

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.adapter.TaskListAdapter
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityTaskBinding
import com.mobichill.justconcentration.factory.TaskViewModelFactory
import com.mobichill.justconcentration.helper.TaskItemTouchHelper
import com.mobichill.justconcentration.listener.OnItemDismissListener
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.util.OnSingleClickListener
import com.mobichill.justconcentration.util.Utils
import com.mobichill.justconcentration.viewmodel.TaskViewModel

class TaskActivity : BaseViewBindingActivity<ActivityTaskBinding>() {
    private var isMenuOpen = false
    private lateinit var taskAdapter: TaskListAdapter
    private val taskViewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(this@TaskActivity)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(getString(R.string.my_tasks))
    }

    fun setupToolbar(title: String) {
        binding.abTitle.text = title
    }

    override fun initViewBinding(): ActivityTaskBinding =
        ActivityTaskBinding.inflate(layoutInflater)

    override fun initView(): Unit = with(binding) {
        fabMain.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                toggleFabMenu()
            }
        })

        fabAdd.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                toggleFabMenu()
                openNewOrEditTaskFragment(null)
            }
        })

        fabSearch.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                toggleFabMenu()
                toggleSearch(true)
                openSearchTasksFragment()
            }
        })

        fabDeletedTasks.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                toggleFabMenu()
                openDeletedTasksFragment()
            }
        })

        btnBack.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                onBackPressed()
            }
        })

        btnClear.setOnClickListener {
            edtSearch.setText("")
            taskViewModel.setSearchQuery("")
        }

        edtSearch.setOnClickListener {
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    Utils.showKeyboard(edtSearch)
                }
            }
        }

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    btnClear.visibility = View.INVISIBLE
                } else {
                    btnClear.visibility = View.VISIBLE
                }
                taskViewModel.setSearchQuery(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        recyclerView.layoutManager = LinearLayoutManager(this@TaskActivity)
        taskAdapter = TaskListAdapter(
            emptyList<TaskModel>().toMutableList(),
            { taskModel ->
                run {
                    openNewOrEditTaskFragment(taskModel)
                }
            },
            { taskModel ->
                run {
                    deleteTask(taskModel)
                }
            },
            onItemDismissListener,
            true
        )
        val itemTouchHelper = ItemTouchHelper(TaskItemTouchHelper(taskAdapter))
        itemTouchHelper.attachToRecyclerView(recyclerView)
        recyclerView.adapter = taskAdapter

        taskViewModel.allTasks.observe(this@TaskActivity) { tasks ->
            if (tasks.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyMessage.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyMessage.visibility = View.GONE
                taskAdapter.updateItems(tasks)
            }
        }
        taskViewModel.syncTasks()
        super.initView()
    }

    private fun toggleSearch(show: Boolean) = with(binding) {
        if (show && edtSearch.isGone) {
            edtSearch.visibility = View.VISIBLE
            edtSearch.requestFocus()
            Utils.showKeyboard(edtSearch)
        } else if (!show && edtSearch.isVisible) {
            edtSearch.visibility = View.GONE
            Utils.hideKeyboard(edtSearch)
        }
    }

    private fun toggleFabMenu() = with(binding) {
        val rotateOpen = ObjectAnimator.ofFloat(fabMain, View.ROTATION, 0f, 135f)
        val rotateClose = ObjectAnimator.ofFloat(fabMain, View.ROTATION, 135f, 0f)

        if (isMenuOpen) {
            rotateClose.start()
            fabGroup.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(200)
                .withEndAction {
                    fabGroup.visibility = View.GONE
                }.start()
        } else {
            fabGroup.visibility = View.VISIBLE
            fabGroup.alpha = 0f
            fabGroup.translationY = 100f
            rotateOpen.start()
            fabGroup.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start()
        }
        isMenuOpen = !isMenuOpen
    }

    private fun openSearchTasksFragment() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, SearchTasksFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun openDeletedTasksFragment() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, DeletedTasksFragment())
            .addToBackStack(null)
            .commit()
    }

    fun openNewOrEditTaskFragment(taskModel: TaskModel?) {
        val newOrEditTaskFragment = NewOrEditTaskFragment().apply {
            arguments = Bundle().apply {
                putParcelable("task_key", taskModel) // Pass task to open editor
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, newOrEditTaskFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun deleteTask(taskModel: TaskModel) {
        taskViewModel.removeOrRestoreTaskWithRoom(taskModel, true)
        taskViewModel.removeOrRestoreTaskWithFireStore(taskModel, true) { complete, error ->
            if (complete) {
                Log.d(TAG, getString(R.string.task_removed))
            } else {
                Log.e(TAG, "Task removed: ${error?.message.orEmpty()}")
            }
        }

        Utils.showToast(this, getString(R.string.task_removed))
    }


    private val onItemDismissListener = object : OnItemDismissListener {
        override fun onTaskDeleted(task: TaskModel) {
            deleteTask(task)
        }
    }

    override fun onBackPressed() {
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when (current) {
            is NewOrEditTaskFragment, is DeletedTasksFragment ->
                onBackPressedDispatcher.onBackPressed()

            is SearchTasksFragment -> {
                onBackPressedDispatcher.onBackPressed()
                toggleSearch(false)
            }
            else ->
                super.onBackPressed()
        }
    }

    fun showExitConfirmation(onConfirmed: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Discard changes?")
            .setMessage("You have unsaved changes. Do you want to discard them?")
            .setPositiveButton("Yes") { _, _ -> onConfirmed() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}