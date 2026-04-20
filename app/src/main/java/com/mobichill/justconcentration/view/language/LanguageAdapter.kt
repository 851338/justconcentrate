package com.mobichill.justconcentration.view.language

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseListAdapter
import com.mobichill.justconcentration.constants.Constants.TYPE_ENGLISH
import com.mobichill.justconcentration.constants.Constants.TYPE_OTHER
import com.mobichill.justconcentration.constants.Constants.TYPE_PORTUGUESE
import com.mobichill.justconcentration.constants.Constants.TYPE_SPANISH
import com.mobichill.justconcentration.databinding.ItemLanguageBinding
import com.mobichill.justconcentration.databinding.ItemLanguageEngBinding
import com.mobichill.justconcentration.databinding.ItemLanguagePortugueseBinding
import com.mobichill.justconcentration.databinding.ItemLanguageSpanishBinding

class LanguageAdapter(
    private val onClickItemLanguage: (LanguageModel) -> Unit,
    private val onToggleCollapse: (String) -> Unit
) : BaseListAdapter<LanguageModel, ViewBinding>(diffUtil) {
    companion object {
        private val diffUtil = object : DiffUtil.ItemCallback<LanguageModel>() {
            override fun areItemsTheSame(oldItem: LanguageModel, newItem: LanguageModel): Boolean {
                return oldItem.isoLanguage == newItem.isoLanguage
            }

            override fun areContentsTheSame(
                oldItem: LanguageModel,
                newItem: LanguageModel
            ): Boolean {
                return oldItem.languageName == newItem.languageName &&
                        oldItem.isCheck == newItem.isCheck &&
                        oldItem.isCollapse == newItem.isCollapse &&
                        oldItem.image == newItem.image &&
                        oldItem.isType == newItem.isType &&
                        oldItem.isOp1 == newItem.isOp1 &&
                        oldItem.isOp2 == newItem.isOp2 &&
                        oldItem.isOp3 == newItem.isOp3 &&
                        oldItem.secondTitle == newItem.secondTitle
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).isoLanguage) {
            "en" -> TYPE_ENGLISH
            "es" -> TYPE_SPANISH
            "pt" -> TYPE_PORTUGUESE
            else -> TYPE_OTHER
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ViewBinding {
        return when (viewType) {
            TYPE_ENGLISH -> ItemLanguageEngBinding.inflate(inflater, parent, false)
            TYPE_SPANISH -> ItemLanguageSpanishBinding.inflate(inflater, parent, false)
            TYPE_PORTUGUESE -> ItemLanguagePortugueseBinding.inflate(inflater, parent, false)
            else -> ItemLanguageBinding.inflate(inflater, parent, false)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun setData(binding: ViewBinding, item: LanguageModel, layoutPosition: Int) {
        val ctx = binding.root.context
        when (binding) {
            is ItemLanguageBinding -> {
                item.image?.let { binding.imgLanguage.setImageDrawable(ctx.getDrawable(it)) }
                binding.imgLanguage.borderColor = ContextCompat.getColor(ctx, if (item.isCheck) R.color.blue else R.color.black)
                binding.tvTitleLanguage.text = item.languageName
                binding.tvTitleSecond.text = item.secondTitle
                binding.checkboxLanguage.isChecked = item.isCheck
            }

            is ItemLanguageEngBinding -> {
                binding.tvTitleLanguage.text = item.languageName
                binding.imgLanguage.borderColor = ContextCompat.getColor(ctx, if (item.isCheck) R.color.blue else R.color.black)
                item.image?.let { binding.imgLanguage.setImageDrawable(ctx.getDrawable(it)) }

                val isExpanded = item.isCollapse
                // This will control the Flow and the LineDrawingView visibility
                toggleVisibility(binding, isExpanded)

                binding.checkboxLanguageUk.isChecked = item.isOp1 && item.isCheck
                binding.checkboxLanguageUs.isChecked = item.isOp2 && item.isCheck
                binding.checkboxLanguageIndia.isChecked = item.isOp3 && item.isCheck

                // CRUCIAL: Update the custom drawing view if expanded
                if (isExpanded) {
                    // Post to ensure all views are measured and laid out before getting their coordinates
                    binding.root.post {
                        // Ensure all views are not null before passing
                        val containerView = binding.container
                        val btnUkView = binding.btnUk
                        val btnUsView = binding.btnUs
                        val btnIndiaView = binding.btnIndia

                        binding.lineDrawingView.updateDrawingParameters(
                            containerView, // Pass the parent container view
                            btnUkView,      // Pass the sub-option views
                            btnUsView,
                            btnIndiaView
                        )
                    }
                }
            }

            is ItemLanguageSpanishBinding -> {
                binding.tvTitleLanguage.text = item.languageName
                binding.imgLanguage.borderColor = ContextCompat.getColor(ctx, if (item.isCheck) R.color.blue else R.color.black)
                item.image?.let { binding.imgLanguage.setImageDrawable(ctx.getDrawable(it)) }

                val isExpanded = item.isCollapse
                toggleVisibility(binding, isExpanded)
                binding.checkboxLanguageUs.isChecked = item.isOp1 && item.isCheck
                binding.checkboxLanguageEs.isChecked = item.isOp3 && item.isCheck

                if (isExpanded) {
                    binding.root.post {
                        val containerView = binding.container
                        val btnUsView = binding.btnUs // Assuming this is for Spanish US
                        val btnEsView = binding.btnEs // Assuming this is for Spanish Spain

                        binding.lineDrawingView.updateDrawingParameters(
                            containerView,
                            btnUsView,
                            btnEsView
                        )
                    }
                }
            }

            is ItemLanguagePortugueseBinding -> {
                binding.tvTitleLanguage.text = item.languageName
                binding.imgLanguage.borderColor = ContextCompat.getColor(ctx, if (item.isCheck) R.color.blue else R.color.black)
                item.image?.let { binding.imgLanguage.setImageDrawable(ctx.getDrawable(it)) }

                val isExpanded = item.isCollapse
                toggleVisibility(binding, isExpanded)
                binding.checkboxLanguageBr.isChecked = item.isOp1 && item.isCheck
                binding.checkboxLanguageCh.isChecked = item.isOp2 && item.isCheck
                binding.checkboxLanguagePt.isChecked = item.isOp3 && item.isCheck

                if (isExpanded) {
                    binding.root.post {
                        val containerView = binding.container
                        val btnBrView = binding.btnBr
                        val btnChView = binding.btnCh
                        val btnPtView = binding.btnPt

                        binding.lineDrawingView.updateDrawingParameters(
                            containerView,
                            btnBrView,
                            btnChView,
                            btnPtView
                        )
                    }
                }
            }
        }
    }

    override fun bindViews(binding: ViewBinding, item: LanguageModel, position: Int) {
        when (binding) {
            is ItemLanguageBinding -> {
                binding.root.setOnClickListener { onClickItemLanguage(item.copy(isType = 0)) }
                binding.checkboxLanguage.setOnClickListener { onClickItemLanguage(item.copy(isType = 0)) }
            }

            is ItemLanguageEngBinding -> {
                binding.root.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                // For "main" elements of the parent view, clicking them should toggle collapse
                binding.container.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                binding.imgLanguage.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                binding.tvTitleLanguage.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                binding.btnDrop.setOnClickListener { onToggleCollapse(item.isoLanguage) } // The dropdown arrow
                // Sub-option clicks
                bindClicks(
                    item, "en",
                    binding.btnUk to 1, binding.checkboxLanguageUk to 1,
                    binding.btnUs to 2, binding.checkboxLanguageUs to 2,
                    binding.btnIndia to 3, binding.checkboxLanguageIndia to 3,
                )
            }

            is ItemLanguageSpanishBinding -> {
                binding.root.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                binding.container.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                binding.imgLanguage.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                binding.tvTitleLanguage.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                binding.btnDrop.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                bindClicks(
                    item, "es",
                    binding.btnUs to 1, binding.checkboxLanguageUs to 1, // Assuming btn_us here maps to a Spanish US option
                    binding.btnEs to 3, binding.checkboxLanguageEs to 3, // Assuming btn_es here maps to a Spanish Spain option
                )
            }

            is ItemLanguagePortugueseBinding -> {
                binding.root.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                binding.container.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                binding.imgLanguage.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                binding.tvTitleLanguage.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                binding.btnDrop.setOnClickListener { onToggleCollapse(item.isoLanguage) }
                bindClicks(
                    item, "pt",
                    binding.btnBr to 1, binding.checkboxLanguageBr to 1,
                    binding.btnCh to 2, binding.checkboxLanguageCh to 2,
                    binding.btnPt to 3, binding.checkboxLanguagePt to 3,
                )
            }
        }
    }

    private fun bindClicks(
        item: LanguageModel,
        iso: String,
        vararg pairs: Pair<View, Int>
    ) {
        pairs.forEach { (view, type) ->
            view.setOnClickListener {
                val model = item.copy(
                    isoLanguage = iso,
                    isType = type
                )
                onClickItemLanguage(model)
            }
        }
    }

    private fun toggleVisibility(binding: ViewBinding, collapse: Boolean) {
        val visible = if (collapse) View.VISIBLE else View.GONE
        when (binding) {
            is ItemLanguageEngBinding -> {
                binding.flowEngOptions.visibility = visible
                binding.lineDrawingView.visibility = visible
            }
            is ItemLanguageSpanishBinding -> {
                binding.flowEngOptions.visibility = visible
                binding.lineDrawingView.visibility = visible
            }
            is ItemLanguagePortugueseBinding -> {
                binding.flowEngOptions.visibility = visible
                binding.lineDrawingView.visibility = visible
            }
        }
    }
}