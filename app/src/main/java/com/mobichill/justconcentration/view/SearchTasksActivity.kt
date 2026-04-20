package com.mobichill.justconcentration.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobichill.justconcentration.view.adapter.TaskAdapter
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.base.application.MyApp
import com.mobichill.justconcentration.databinding.FragmentSearchTasksBinding
import com.mobichill.justconcentration.factory.TaskViewModelFactory
import com.mobichill.justconcentration.manager.BannerAdManager
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

class SearchTasksActivity : BaseViewBindingActivity<FragmentSearchTasksBinding>() {
    private val taskViewModelFactory by lazy {
        TaskViewModelFactory(
            FireStoreRepository(),
            MyApp.instance.taskRepository
        )
    }
    private val taskViewModel: TaskViewModel by viewModels {
        taskViewModelFactory
    }
    private lateinit var taskAdapter: TaskAdapter

    override fun initViewBinding(): FragmentSearchTasksBinding =
        FragmentSearchTasksBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialQuery = intent.getStringExtra(EXTRA_QUERY).orEmpty()
        taskViewModel.setSearchQuery(initialQuery)
    }

    override fun initData(intent: Intent?, isNewIntent: Boolean) {}

    override fun initView() {
        super.initView()
        taskAdapter = TaskAdapter(
            { taskModel ->
                startActivity(
                    com.mobichill.justconcentration.view.NewOrEditTaskActivity
                        .newIntent(this, taskModel)
                )
            },
            null, null
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = taskAdapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
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

        BannerAdManager.loadBanner(binding.adView)
    }

    fun showEmptyResultsView(isEmpty: Boolean) = with(binding) {
        emptyMessage.isVisible = isEmpty
        recyclerView.isVisible = !isEmpty
    }

    companion object {
        private const val EXTRA_QUERY = "extra_query"

        fun newIntent(context: android.content.Context, query: String): Intent {
            return Intent(context, SearchTasksActivity::class.java)
                .putExtra(EXTRA_QUERY, query)
        }
    }
}
