package com.mobichill.justconcentration.view

import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobichill.justconcentration.adapter.TaskListAdapter
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentSearchTasksBinding
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

class SearchTasksFragment : BaseViewBindingFragment<FragmentSearchTasksBinding>() {
    private val taskViewModel: TaskViewModel by activityViewModels()
    private lateinit var taskAdapter: TaskListAdapter

    override fun initViewBinding(): FragmentSearchTasksBinding =
        FragmentSearchTasksBinding.inflate(layoutInflater)

    override fun initData() {

    }

    override fun initView() {
        binding.recyclerView.adapter = taskAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        taskAdapter = TaskListAdapter(
            emptyList<TaskModel>().toMutableList(),
            { taskModel ->
                run {
                    if (requireActivity() is TaskActivity)
                        (requireActivity() as TaskActivity).openNewOrEditTaskFragment(taskModel)
                }
            },
            { taskModel ->
                run {
                    //do nothing
                }
            },
            null
        )

        binding.recyclerView.adapter = taskAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.searchResults.collect { filteredList ->
                    if (filteredList.isEmpty()) {
                        showEmptyResultsView(true)
                    } else {
                        showEmptyResultsView(false)
                    }
                    taskAdapter.addItems(filteredList)
                }
            }
        }
    }

    fun showEmptyResultsView(isEmpty: Boolean) = with(binding) {
        emptyMessage.isVisible = isEmpty
        recyclerView.isVisible = !isEmpty
    }
}