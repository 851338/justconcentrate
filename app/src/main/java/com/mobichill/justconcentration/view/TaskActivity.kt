package com.mobichill.justconcentration.view

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
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
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityTaskBinding
import com.mobichill.justconcentration.factory.TaskViewModelFactory
import com.mobichill.justconcentration.helper.AlarmHelper
import com.mobichill.justconcentration.helper.TaskItemTouchHelper
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils
import com.mobichill.justconcentration.listener.OnItemDismissListener
import com.mobichill.justconcentration.listener.OnMenuActionListener
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.view.adapter.TaskAdapter
import com.mobichill.justconcentration.viewmodel.TaskViewModel

class TaskActivity : BaseViewBindingActivity<ActivityTaskBinding>() {
    private var isMenuOpen = false
    private lateinit var taskAdapter: TaskAdapter
    val taskViewModelFactory by lazy {
        TaskViewModelFactory()
    }
    val taskViewModel: TaskViewModel by viewModels {
        taskViewModelFactory
    }

    private val sfUtils: SharedPreferencesUtils by lazy {
        SharedPreferencesUtils(applicationContext)
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
        super.initView()
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
        taskAdapter = TaskAdapter(
            { taskModel -> openNewOrEditTaskFragment(taskModel) },
            onItemDismissListener,
            onMenuActionListener
        )

        // Attach ItemTouchHelper for swipe gestures
        val itemTouchHelper = ItemTouchHelper(TaskItemTouchHelper(taskAdapter))
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Set RecyclerView's layout manager and adapter
        recyclerView.layoutManager = LinearLayoutManager(this@TaskActivity)
        recyclerView.adapter = taskAdapter
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                this@TaskActivity,
                LinearLayoutManager.VERTICAL
            )
        )

        // Observe the LiveData
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

        // Sync feature
        val isSynced = sfUtils.isSettingsSyncEnabled()
        if (!isSynced) return
        if (Utils.isNetworkAvailable(this@TaskActivity)) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                try {
                    // Sync from room to fireStore
                    FireStoreRepository().syncUnsyncedTasksToFireStore(user.uid)
                    // Sync from fireStore to room
                    taskViewModel.syncTasks()
                } catch (e: Exception) {
                    Log.e(TAG, "Sync tasks: ", e)
                }
            }
        }
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
        var isSyncedSuccessfully = false
        if (sfUtils.isUserLoggedIn() && Utils.isNetworkAvailable(this@TaskActivity)) {
            try {
                Log.d(TAG, "Attempting FireStore sync for session ${taskModel.id}")
                // Sync the potentially modified sessionToSave
                taskViewModel.deleteTaskFromFireStore(taskModel.copy(isSynced = true)) // Try FireStore with isSynced=true
                isSyncedSuccessfully = true // Mark as synced ONLY if FireStore call succeeds
                Log.d(TAG, "FireStore sync SUCCESS for session ${taskModel.id}")
            } catch (e: Exception) {
                Log.e(TAG, "FireStore sync FAILED for session ${taskModel.id}", e)
                isSyncedSuccessfully = false // Ensure it's false on FireStore failure
            }
        } else {
            Log.d(TAG, "Skipping FireStore sync (Conditions not met) for session ${taskModel.id}")
            isSyncedSuccessfully = false // Explicitly false if conditions aren't met
        }
        try {
            Log.d(TAG, "Saving final state to Room ${taskModel.id}, Synced: $isSyncedSuccessfully)")
            taskViewModel.deleteTaskFromRoom(taskModel.copy(isSynced = isSyncedSuccessfully))
            cancelAlarm(taskModel.requestCode)
            Utils.showToast(this@TaskActivity, getString(R.string.task_deleted))
            Log.d(TAG, "Room save successful for task ${taskModel.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Room save FAILED for session ${taskModel.id}", e)
        }
    }

    private fun doneTask(taskModel: TaskModel?) {
        if (taskModel == null)
            return
        if (taskModel.completed) {
            Utils.showToast(this, getString(R.string.task_already_done))
            return
        }
        val now = System.currentTimeMillis()
        val updatedTask = taskModel.copy(
            completed = true,
            completedAt = now,
            lastModified = now
        )
        var isSyncedSuccessfully = false
        if (sfUtils.isUserLoggedIn() && Utils.isNetworkAvailable(this@TaskActivity)) {
            try {
                Log.d(TAG, "Attempting FireStore sync for session ${taskModel.id}")
                // Sync the potentially modified sessionToSave
                taskViewModel.updateTaskToFireStore(updatedTask.copy(isSynced = true))
                Log.d(TAG, "FireStore sync SUCCESS for session ${taskModel.id}")
                isSyncedSuccessfully = true // Mark as synced ONLY if FireStore call succeeds
            } catch (e: Exception) {
                Log.e(TAG, "FireStore sync FAILED for session ${taskModel.id}", e)
                isSyncedSuccessfully = false // Ensure it's false on FireStore failure
            }
        } else {
            Log.d(TAG, "Skipping FireStore sync (Conditions not met) for session ${taskModel.id}")
            isSyncedSuccessfully = false // Explicitly false if conditions aren't met
        }
        try {
            Log.d(TAG, "Saving final state to Room ${taskModel.id}, Synced: $isSyncedSuccessfully)")
            taskViewModel.updateTaskToRoom(updatedTask.copy(isSynced = isSyncedSuccessfully))
            taskViewModel.updateAfterTaskCompletion(updatedTask.completedAt)
            cancelAlarm(taskModel.requestCode)
            Utils.showToast(this, getString(R.string.task_done))
            Log.d(TAG, "Room save successful for task ${taskModel.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Room save FAILED for session ${taskModel.id}", e)
        }
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

    private val onMenuActionListener = object : OnMenuActionListener {
        override fun onDelete(task: TaskModel) {
            Utils.showConfirmDialog(
                this@TaskActivity,
                getString(R.string.delete_confirm),
                getString(R.string.delete_confirm_message),
                getString(R.string.yes),
                getString(R.string.cancel)
            ) { deleteTask(task) }
        }

        override fun onMarkDone(task: TaskModel) {
            Utils.showConfirmDialog(
                this@TaskActivity,
                getString(R.string.done_confirm),
                getString(R.string.done_confirm_message),
                getString(R.string.yes),
                getString(R.string.cancel)
            ) { doneTask(task) }
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


    // Case choose ringtone of NewOrEditTaskFragment
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        val uri: Uri? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            else
                @Suppress("DEPRECATION")
                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) ?: return
        if (uri == null)
            return
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (current !is NewOrEditTaskFragment) return
        current.checkAndSaveAudioFile(uri)
    }
}