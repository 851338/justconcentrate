package com.mobichill.justconcentration.view

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import android.view.ActionMode
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.databinding.ActivityTaskBinding
import com.mobichill.justconcentration.helper.AlarmHelper
import com.mobichill.justconcentration.helper.TaskItemTouchHelper
import com.mobichill.justconcentration.listener.OnItemDismissListener
import com.mobichill.justconcentration.listener.OnMenuActionListener
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.listener.SelectionListener
import com.mobichill.justconcentration.manager.AdsManager
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils
import com.mobichill.justconcentration.view.adapter.TaskAdapter
import com.mobichill.justconcentration.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskActivity : BaseViewBindingActivity<ActivityTaskBinding>() {
    private var isMenuOpen = false
    private lateinit var taskAdapter: TaskAdapter

    private val taskViewModel: TaskViewModel by viewModels()

    private val sfUtils: SharedPreferencesUtils by lazy {
        SharedPreferencesUtils(applicationContext)
    }

    @Inject
    lateinit var alarmHelper: AlarmHelper

    @Inject
    lateinit var adsManager: AdsManager

    // --- Selection State ---
    // Store IDs of selected tasks
    private val selectedTaskIds = HashSet<String>()
    private var currentActionMode: ActionMode? = null
    // -----------------------

    // --- ActionMode Callback Implementation ---
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Inflate the menu for the CAB
            mode.menuInflater.inflate(R.menu.context_action_menu, menu)
            setupToolbar(getString(R.string.my_tasks)) // Restore normal title initially if needed, or just let CAB title take over
            binding.layoutActionBar.isVisible = false // Hide your custom action bar
            return true // Return true to show the CAB
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Update the CAB title
            updateActionModeTitle(mode)
            // You could enable/disable menu items here based on selectedTaskIds.size
            // e.g., val editItem = menu.findItem(R.id.action_edit)
            // editItem.isVisible = selectedTaskIds.size == 1
            menu.findItem(R.id.action_mark_done).isVisible =
                selectedTaskIds.isNotEmpty() // Mark done only if at least one selected
            menu.findItem(R.id.action_delete).isVisible =
                selectedTaskIds.isNotEmpty() // Delete only if at least one selected
            return true // Return true if menu has changed
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    showDeleteConfirmationForSelectedItems()
                    mode.finish() // Action handled, exit mode
                    true
                }

                R.id.action_mark_done -> {
                    showMarkDoneConfirmationForSelectedItems()
                    mode.finish() // Action handled, exit mode
                    true
                }
                // Add more actions here
                else -> false // Action not handled
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            // CAB is closing, clear selection and restore UI
            clearSelection()
            currentActionMode = null // Nullify the mode object
            binding.layoutActionBar.isVisible = true // Show your custom action bar again
            setupToolbar(getString(R.string.my_tasks)) // Restore the default title
        }
    }
    // ------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
            when {
                currentActionMode != null -> {
                    currentActionMode?.finish()
                }

                current is NewOrEditTaskFragment -> {
                    this.isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }

                current is SearchTasksFragment -> {
                    this.isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    toggleSearch(false)
                }

                else -> {
                    this.isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        lifecycleScope.launch {
            adsManager.loadAndShowBannerAd(binding.adView)
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentActionMode == null) {
            setupToolbar(getString(R.string.my_tasks))
        }
        adsManager.onResume(binding.adView)
    }

    override fun onPause() {
        super.onPause()
        adsManager.onPause(binding.adView)
    }

    override fun onDestroy() {
        super.onDestroy()
        currentActionMode?.finish()
        if (isFinishing)
            adsManager.onDestroy(binding.adView)
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
                if (currentActionMode != null) {
                    currentActionMode?.finish()
                } else {
                    onBackPressedDispatcher.onBackPressed()
                }
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
                if (currentActionMode == null) {
                    if (!s.isNullOrEmpty()) {
                        btnClear.visibility = View.VISIBLE
                    } else {
                        btnClear.visibility = View.INVISIBLE
                    }
                    taskViewModel.setSearchQuery(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Initialize the adapter once with an empty list
        taskAdapter = TaskAdapter(selectionListener, onItemDismissListener, onMenuActionListener)


        // Attach ItemTouchHelper for swipe gestures
        val itemTouchHelper =
            ItemTouchHelper(TaskItemTouchHelper(taskAdapter) { currentActionMode == null })

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
                if (currentActionMode != null && tasks.none { selectedTaskIds.contains(it.id) }) {
                    currentActionMode?.finish()
                } else if (currentActionMode != null) {
                    // If some selected items might still be in the list, update mode title/menu
                    currentActionMode?.invalidate()
                }
            }
        }
    }

    // --- SelectionListener methods implementation ---

    private val selectionListener = object : SelectionListener {
        override fun onItemClick(task: TaskModel, position: Int) {
            if (currentActionMode != null) {
                // If in selection mode, toggle selection
                toggleSelection(task, position)
            } else {
                // If not in selection mode, perform default click action
                openNewOrEditTaskFragment(task)
            }
        }

        override fun onItemLongClick(task: TaskModel, position: Int): Boolean {
            if (currentActionMode == null) {
                // If not in selection mode, start selection mode and select the item
                startSelectionMode(task, position)
                return true // Consume the long click
            } else {// If already in selection mode, you could toggle selection on long press too,
                // or do nothing and let the click listener handle it. Let's just consume it
                // so the click listener doesn't fire twice immediately.
                toggleSelection(task, position) // Optionally toggle on long click too
                return true // Consume the long click
            }
        }

        override fun isTaskSelected(task: TaskModel): Boolean {
            return selectedTaskIds.contains(task.id)
        }

        override fun isActionModeActive(): Boolean {
            return currentActionMode != null
        }
    }

    // --- Selection Management Methods ---

    private fun startSelectionMode(task: TaskModel, position: Int) {
        if (currentActionMode == null) {
            // Start the CAB
            currentActionMode = startActionMode(actionModeCallback)
        }
        // Select the clicked item
        toggleSelection(task, position)
    }

    private fun toggleSelection(task: TaskModel, position: Int) {
        val isSelected = selectedTaskIds.contains(task.id)
        if (isSelected) {
            selectedTaskIds.remove(task.id)
        } else {
            selectedTaskIds.add(task.id)
        }

        // Notify adapter that this specific item view needs to update its background
        taskAdapter.notifyItemChanged(position)

        // Update the action mode title and menu
        if (selectedTaskIds.isEmpty()) {
            currentActionMode?.finish() // No items selected, exit mode
        } else {
            updateActionModeTitle(currentActionMode)
            currentActionMode?.invalidate() // Request onPrepareActionMode to update menu items
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearSelection() {
        // Need to notify adapter for all items that were previously selected
        // to reset their background. Getting current positions is tricky
        // if the underlying list changed. Easiest is to just tell the adapter
        // to rebind visible items or the whole list.
        val positionsToUpdate = mutableListOf<Int>()
        // Find positions of tasks that *were* selected using the current adapter list
        taskAdapter.currentList.forEachIndexed { index, taskModel ->
            if (selectedTaskIds.contains(taskModel.id)) {
                positionsToUpdate.add(index)
            }
        }

        selectedTaskIds.clear() // Clear the set *after* getting positions

        // Notify specific items if possible, or just notifyDataSetChanged if complex
        if (positionsToUpdate.isNotEmpty()) {
            // This is efficient but assumes positions are still valid
            for (pos in positionsToUpdate) {
                taskAdapter.notifyItemChanged(pos)
            }
        } else {
            // Fallback: notify the whole dataset
            taskAdapter.notifyDataSetChanged()
        }


        // Invalidate action mode to update UI (e.g., title becomes 0)
        currentActionMode?.invalidate()
    }

    private fun updateActionModeTitle(mode: ActionMode?) {
        mode?.title = if (selectedTaskIds.isEmpty()) {
            "" // Or a default like "Select items"
        } else {
            resources.getQuantityString(
                R.plurals.selected_items, // Define this plural string
                selectedTaskIds.size,
                selectedTaskIds.size
            )
        }
    }

    // --- Action Implementation for Selected Items ---

    // Show confirmation dialog before deleting multiple items
    private fun showDeleteConfirmationForSelectedItems() {
        if (selectedTaskIds.isEmpty()) return
        Utils.showConfirmDialog(
            this@TaskActivity,
            getString(R.string.delete_confirm),
            resources.getQuantityString(
                R.plurals.delete_selected_confirm_message,
                selectedTaskIds.size,
                selectedTaskIds.size
            ), // Define this plural string
            getString(R.string.yes),
            getString(R.string.cancel)
        ) {
            deleteSelectedTasks()
        }
    }

    private fun deleteSelectedTasks() {
        if (selectedTaskIds.isEmpty()) return

        // Get the actual TaskModel objects for the selected IDs
        // It's safest to get these from the current ViewModel data or adapter list
        val tasksToDelete =
            taskViewModel.allTasks.value?.filter { selectedTaskIds.contains(it.id) } ?: emptyList()

        // Perform deletion for each selected task
        // Need to adapt your existing deleteTask function or create a new one
        tasksToDelete.forEach { task ->
            deleteTask(task) // Reusing the existing single delete logic
        }
    }

    private fun showMarkDoneConfirmationForSelectedItems() {
        if (selectedTaskIds.isEmpty()) return
        Utils.showConfirmDialog(
            this@TaskActivity,
            getString(R.string.done_confirm), // Assuming you have this string
            resources.getQuantityString(
                R.plurals.done_selected_confirm_message,
                selectedTaskIds.size,
                selectedTaskIds.size
            ), // Define this plural string
            getString(R.string.yes),
            getString(R.string.cancel)
        ) {
            doneSelectedTasks()
        }
    }

    private fun doneSelectedTasks() {
        if (selectedTaskIds.isEmpty()) return

        // Get the actual TaskModel objects for the selected IDs
        val tasksToMarkDone =
            taskViewModel.allTasks.value?.filter { selectedTaskIds.contains(it.id) } ?: emptyList()

        val now = System.currentTimeMillis()
        tasksToMarkDone.forEach { task ->
            // Update the task and reuse/adapt your doneTask logic
            val updatedTask = task.copy(
                completed = true,
                completedAt = now
            )
            doneTask(updatedTask) // Reusing existing single done logic
        }
    }

    // --- OnMenuActionListener method implementation ---
    private val onMenuActionListener = object : OnMenuActionListener {
        override fun onDelete(task: TaskModel) {
            if (currentActionMode == null) {
                Utils.showConfirmDialog(
                    this@TaskActivity,
                    getString(R.string.delete_confirm),
                    getString(R.string.delete_confirm_message),
                    getString(R.string.yes),
                    getString(R.string.cancel)
                ) { deleteTask(task) }
            }
        }

        override fun onMarkDone(task: TaskModel) {
            if (currentActionMode == null) {
                Utils.showConfirmDialog(
                    this@TaskActivity,
                    getString(R.string.done_confirm),
                    getString(R.string.done_confirm_message),
                    getString(R.string.yes),
                    getString(R.string.cancel)
                ) { doneTask(task) }
            }
        }
    }

    // --- OnItemDismissListener method implementation ---
    private val onItemDismissListener = object : OnItemDismissListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onTaskDeleted(task: TaskModel?) {
            if (currentActionMode == null) {
                Utils.showConfirmDialog(
                    this@TaskActivity,
                    getString(R.string.delete_confirm),
                    getString(R.string.delete_confirm_message),
                    getString(R.string.yes),
                    getString(R.string.cancel)
                ) { deleteTask(task) }
            } else {
                Log.d(TAG, "Swipe ignored: In action mode")
                taskAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun toggleSearch(show: Boolean) = with(binding) {
        if (show && edtSearch.isGone) {
            if (currentActionMode == null) {
                edtSearch.visibility = View.VISIBLE
                edtSearch.requestFocus()
                Utils.showKeyboard(edtSearch)
            }
        } else if (!show && edtSearch.isVisible) {
            edtSearch.visibility = View.GONE
            btnClear.visibility = View.GONE
            Utils.hideKeyboard(edtSearch)
        }
    }

    private fun toggleFabMenu() = with(binding) {
        if (currentActionMode != null) return@with
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
        if (currentActionMode != null) return
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
        var isSyncedSuccessfully: Boolean
        if (sfUtils.isUserLoggedIn() && Utils.isNetworkAvailable(this@TaskActivity)) {
            try {
                Log.d(TAG, "Attempting Firestore sync for session ${taskModel.id}")
                // Sync the potentially modified sessionToSave
                taskViewModel.deleteTaskFromFirestore(taskModel.copy(isSynced = true)) // Try Firestore with isSynced=true
                isSyncedSuccessfully = true // Mark as synced ONLY if Firestore call succeeds
                Log.d(TAG, "Firestore sync SUCCESS for session ${taskModel.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Firestore sync FAILED for session ${taskModel.id}", e)
                isSyncedSuccessfully = false // Ensure it's false on Firestore failure
            }
        } else {
            Log.d(TAG, "Skipping Firestore sync (Conditions not met) for session ${taskModel.id}")
            isSyncedSuccessfully = false // Explicitly false if conditions aren't met
        }
        try {
            Log.d(TAG, "Saving final state to Room ${taskModel.id}, Synced: $isSyncedSuccessfully)")
            taskViewModel.deleteTaskFromRoom(taskModel.copy(isSynced = isSyncedSuccessfully))
            cancelAlarm(taskModel.requestCode)
            // Only show toast for single deletion, not during multi-select action
            if (currentActionMode == null) {
                Utils.showToast(this@TaskActivity, getString(R.string.task_deleted))
            }
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
            completedAt = now
        )
        var isSyncedSuccessfully: Boolean
        if (sfUtils.isUserLoggedIn() && Utils.isNetworkAvailable(this@TaskActivity)) {
            try {
                Log.d(TAG, "Attempting Firestore sync for session ${taskModel.id}")
                // Sync the potentially modified sessionToSave
                taskViewModel.updateTaskToFirestore(updatedTask.copy(isSynced = true))
                Log.d(TAG, "Firestore sync SUCCESS for session ${taskModel.id}")
                isSyncedSuccessfully = true // Mark as synced ONLY if Firestore call succeeds
            } catch (e: Exception) {
                Log.e(TAG, "Firestore sync FAILED for session ${taskModel.id}", e)
                isSyncedSuccessfully = false // Ensure it's false on Firestore failure
            }
        } else {
            Log.d(TAG, "Skipping Firestore sync (Conditions not met) for session ${taskModel.id}")
            isSyncedSuccessfully = false // Explicitly false if conditions aren't met
        }
        try {
            Log.d(TAG, "Saving final state to Room ${taskModel.id}, Synced: $isSyncedSuccessfully)")
            taskViewModel.updateTaskToRoom(updatedTask.copy(isSynced = isSyncedSuccessfully))
            taskViewModel.updateAfterTaskCompletion(updatedTask.completedAt)
            cancelAlarm(taskModel.requestCode)
            // Only show toast for single done, not during multi-select action
            if (currentActionMode == null) {
                Utils.showToast(this, getString(R.string.task_done))
            }
            Log.d(TAG, "Room save successful for task ${taskModel.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Room save FAILED for session ${taskModel.id}", e)
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        alarmHelper.cancelAlarm(this, requestCode)
    }

    // Case choose ringtone of NewOrEditTaskFragment
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
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