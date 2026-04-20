package com.mobichill.justconcentration.view.language

import android.content.Intent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.constants.Constants
import com.mobichill.justconcentration.databinding.ActivityLanguageBinding
import com.mobichill.justconcentration.view.MainActivity
import com.mobichill.justconcentration.view.WelcomeActivity
import com.mobichill.justconcentration.view.popup.LoadingDialog

class LanguageActivity : BaseViewBindingActivity<ActivityLanguageBinding>() {

    private val viewModel: LanguageViewModel by viewModels { LanguageViewModelFactory(application, PreferenceRepository(this)) }

    private var languageAdapter: LanguageAdapter? = null
    private val translatingDialog by lazy { LoadingDialog(this, true) }

    override fun initViewBinding(): ActivityLanguageBinding = ActivityLanguageBinding.inflate(layoutInflater)

    override fun initView() {
        super.initView()
        val isSetting = intent.getBooleanExtra(Constants.IS_SETTING, false)
        viewModel.setSettingMode(isSetting)

        setupToolBar()
        setupRecyclerView()
        observableViewModel()
        setOnClickListener()

        // Handle back press to revert UI state if not confirmed
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.isSettingMode) {
                    viewModel.revertToInitialSavedLanguage()
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupToolBar() = with(binding) {
        textLang.text =
            getString(if (viewModel.isSettingMode) R.string.string_language else R.string.choose_language)
        ivCheckLeft.visibility = if (viewModel.isSettingMode) View.VISIBLE else View.GONE
        if (viewModel.isSettingMode) {
            val params = textLang.layoutParams as ConstraintLayout.LayoutParams
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
            params.marginStart = resources.getDimensionPixelSize(R.dimen._54dp)
            textLang.layoutParams = params
        } else {
            val params = textLang.layoutParams as ConstraintLayout.LayoutParams
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.marginStart = resources.getDimensionPixelSize(R.dimen._0dp)
            textLang.layoutParams = params
        }
    }

    private fun setupRecyclerView() {
        languageAdapter = LanguageAdapter(
            onClickItemLanguage = { selectedLanguageModel ->
                viewModel.onLanguageSelected(selectedLanguageModel)
            },
            onToggleCollapse = { isoLanguage ->
                viewModel.onToggleCollapse(isoLanguage)
            }
        )
        binding.rvLanguage.layoutManager = LinearLayoutManager(this)
        binding.rvLanguage.adapter = languageAdapter
    }

    private fun observableViewModel() {
        viewModel.languageList.observe(this) { list ->
            languageAdapter?.submitList(list)
        }

        // Observe _currentlyViewedLanguage for tick button visibility
        viewModel.currentlyViewedLanguage.observe(this) { selectedModel ->
            binding.ivCheckRight.isVisible = selectedModel != null
        }

        viewModel.showLoading.observe(this) { show ->
            if (show) {
                if (!translatingDialog.isShowing) {
                    translatingDialog.show()
                }
            } else {
                if (translatingDialog.isShowing) {
                    translatingDialog.dismiss()
                }
            }
        }
        
        viewModel.navigateToNextScreen.observe(this) { event ->
            if (event.getContentIfNotHandled() == true) {
                 finish()
            } else if (event.peekContent() == false) {
                 startActivity(Intent(this, MainActivity::class.java))
                 finish()
            }
        }

    }

    private fun setOnClickListener() {
        binding.ivCheckRight.setOnClickListener {
            viewModel.onConfirmSelectionClicked()
        }
        binding.ivCheckLeft.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (translatingDialog.isShowing) {
            translatingDialog.dismiss()
        }
        super.onDestroy()
    }
}