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
import com.mobichill.justconcentration.listener.SelectionListener
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchTasksFragment : BaseViewBindingFragment<FragmentSearchTasksBinding>() {
    private val taskViewModel: TaskViewModel by activityViewModels()
    private lateinit var taskAdapter: TaskAdapter

    override fun initViewBinding(): FragmentSearchTasksBinding =
        FragmentSearchTasksBinding.inflate(layoutInflater)

    override fun initData() {}

    override fun initView() {
        taskAdapter = TaskAdapter(searchSelectionListener, null, null)

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

    private fun showEmptyResultsView(isEmpty: Boolean) = with(binding) {
        emptyMessage.isVisible = isEmpty
        recyclerView.isVisible = !isEmpty
    }

    private val searchSelectionListener = object : SelectionListener {
        override fun onItemClick(task: TaskModel, position: Int) {
            (requireActivity() as? TaskActivity)?.openNewOrEditTaskFragment(task)
        }

        override fun onItemLongClick(task: TaskModel, position: Int): Boolean {
            return false
        }

        override fun isTaskSelected(task: TaskModel): Boolean {
            return false
        }

        override fun isActionModeActive(): Boolean {
            return false
        }
    }
}