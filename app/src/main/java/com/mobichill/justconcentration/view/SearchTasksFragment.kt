package com.mobichill.justconcentration.view

import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobichill.justconcentration.view.adapter.TaskAdapter
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentSearchTasksBinding
import com.mobichill.justconcentration.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

class SearchTasksFragment : BaseViewBindingFragment<FragmentSearchTasksBinding>() {
    private val taskViewModel: TaskViewModel by activityViewModels()
    private lateinit var taskAdapter: TaskAdapter

    override fun initViewBinding(): FragmentSearchTasksBinding =
        FragmentSearchTasksBinding.inflate(layoutInflater)

    override fun initData() {}

    override fun initView() {
        taskAdapter = TaskAdapter(
            { taskModel ->
                run {
                    if (requireActivity() is TaskActivity)
                        (requireActivity() as TaskActivity).openNewOrEditTaskFragment(taskModel)
                }
            },
            null, null
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        binding.recyclerView.adapter = taskAdapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                requireActivity(),
                LinearLayoutManager.VERTICAL
            )
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.searchResults.collect { filteredList ->
                    if (filteredList.isEmpty()) {
                        showEmptyResultsView(true)
                    } else {
                        showEmptyResultsView(false)
                    }
                    taskAdapter.updateItems(filteredList)
                }
            }
        }
    }

    fun showEmptyResultsView(isEmpty: Boolean) = with(binding) {
        emptyMessage.isVisible = isEmpty
        recyclerView.isVisible = !isEmpty
    }
}