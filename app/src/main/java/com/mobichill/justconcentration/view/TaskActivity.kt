package com.mobichill.justconcentration.view

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.adapter.TaskListAdapter
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityTaskBinding
import com.mobichill.justconcentration.factory.TaskViewModelFactory
import com.mobichill.justconcentration.helper.AlarmHelper
import com.mobichill.justconcentration.helper.TaskItemTouchHelper
import com.mobichill.justconcentration.listener.OnItemDismissListener
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.util.Utils
import com.mobichill.justconcentration.viewmodel.TaskViewModel

class TaskActivity : BaseViewBindingActivity<ActivityTaskBinding>() {
    private var isMenuOpen = false
    private lateinit var taskAdapter: TaskListAdapter
    val taskViewModelFactory by lazy {
        TaskViewModelFactory()
    }
    val taskViewModel: TaskViewModel by viewModels {
        taskViewModelFactory
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

        btnBack.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                onBackPressed()
            }
        })

        btnClear.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    edtSearch.setText("")
                    taskViewModel.setSearchQuery("")
                }
            }
        )

        edtSearch.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    Utils.showKeyboard(edtSearch)
                }
            }
        )

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

        // Initialize the adapter once with an empty list
        taskAdapter = TaskListAdapter(
            { taskModel -> openNewOrEditTaskFragment(taskModel) },
            { taskModel ->
                Utils.showConfirmDialog(
                    this@TaskActivity,
                    getString(R.string.delete_confirm),
                    getString(R.string.delete_confirm_message),
                    getString(R.string.yes),
                    getString(R.string.cancel)
                ) { deleteTask(taskModel) }
            },
            onItemDismissListener
        )

        // Attach ItemTouchHelper for swipe gestures
        val itemTouchHelper = ItemTouchHelper(TaskItemTouchHelper(taskAdapter))
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Set RecyclerView's layout manager and adapter
        recyclerView.layoutManager = LinearLayoutManager(this@TaskActivity)
        recyclerView.adapter = taskAdapter
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                recyclerView.context,
                LinearLayoutManager.VERTICAL
            )
        )

        // Now observe the LiveData
        taskViewModel.allTasks.observe(this@TaskActivity) { tasks ->
            if (tasks.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyMessage.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyMessage.visibility = View.GONE
                taskAdapter.updateItems(tasks) // Update the adapter with the new tasks
            }
        }

        if (Utils.isNetworkAvailable(this@TaskActivity)) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                try {
                    //sync from room to fireStore
                    FireStoreRepository().syncUnsyncedTasksToFireStore(user.uid)
                    //sync from fireStore to room
                    taskViewModel.syncTasks()
                } catch (e: Exception) {
                    Log.e(TAG, "Sync tasks: ", e)
                }
            }
        }
        super.initView()
    }

    private fun toggleSearch(show: Boolean) = with(binding) {
        if (show && edtSearch.isGone) {
            edtSearch.visibility = View.VISIBLE
            edtSearch.requestFocus()
            Utils.showKeyboard(edtSearch)
        } else if (!show && edtSearch.isVisible) {
            edtSearch.visibility = View.GONE
            btnClear.visibility = View.GONE
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
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            .replace(binding.fragmentContainer.id, SearchTasksFragment())
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
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            .replace(binding.fragmentContainer.id, newOrEditTaskFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun deleteTask(taskModel: TaskModel?) {
        if (taskModel == null)
            return
        var isSynced = false
        if (Utils.isNetworkAvailable(this) && Utils.isUserLoggedIn(this)) {
            isSynced = true
            taskViewModel.deleteTaskFromFireStore(
                taskModel.copy(isSynced = true)
            )
        } else isSynced = false
        taskViewModel.deleteTaskFromRoom(taskModel.copy(isSynced = isSynced))
        cancelAlarm(taskModel.requestCode)
        Utils.showToast(this, getString(R.string.task_deleted))
    }

    private fun cancelAlarm(requestCode: Int) {
        AlarmHelper().cancelAlarm(this, requestCode)
    }

    private val onItemDismissListener = object : OnItemDismissListener {
        override fun onTaskDeleted(task: TaskModel?) {
            Utils.showConfirmDialog(
                this@TaskActivity,
                getString(R.string.delete_confirm),
                getString(R.string.delete_confirm_message),
                getString(R.string.yes),
                getString(R.string.cancel)
            ) { deleteTask(task) }
        }
    }

    override fun onBackPressed() {
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when (current) {
            is NewOrEditTaskFragment ->
                onBackPressedDispatcher.onBackPressed()

            is SearchTasksFragment -> {
                onBackPressedDispatcher.onBackPressed()
                toggleSearch(false)
            }

            else ->
                super.onBackPressed()
        }
    }
}