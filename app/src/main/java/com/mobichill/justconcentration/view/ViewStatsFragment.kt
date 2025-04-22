package com.mobichill.justconcentration.view

import android.graphics.Color
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.gms.ads.AdRequest
import com.google.android.material.datepicker.MaterialDatePicker
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentViewStatsBinding
import com.mobichill.justconcentration.factory.SessionViewModelFactory
import com.mobichill.justconcentration.others.TimeRangeOption
import com.mobichill.justconcentration.viewmodel.SessionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ViewStatsFragment : BaseViewBindingFragment<FragmentViewStatsBinding>() {
    override fun initViewBinding(): FragmentViewStatsBinding =
        FragmentViewStatsBinding.inflate(layoutInflater)

    private lateinit var viewModel: SessionViewModel
    override fun initViewModel() {
        super.initViewModel()
        viewModel = ViewModelProvider(
            this,
            SessionViewModelFactory()
        )[SessionViewModel::class.java]
    }

    override fun initData() {}

    override fun initView() {
        if (activity is HomeActivity)
            (activity as HomeActivity).setupToolbar(getString(R.string.stats_title), true)

        setupSpinner()

        viewModel.sessionStats.observe(viewLifecycleOwner) { sessions ->
            val barEntries = sessions
                .groupBy { LocalDate.parse(it.date) }
                .map { (date, sessionList) ->
                    BarEntry(
                        date.toEpochDay().toFloat(),
                        sessionList.sumOf { it.durationMinutes }.toFloat()
                    )
                }
                .sortedBy { it.x } // sort by date

            updateChart(barEntries)
        }

        //run ads
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    fun updateChart(chartData: List<BarEntry>) = with(binding) {
        // Create a BarDataSet from the chartData (List<BarEntry>)
        val barDataSet = BarDataSet(chartData, "Focus Duration")

        // Customize the dataset appearance (optional)
        barDataSet.color = Color.BLUE // Set bar color
        barDataSet.valueTextColor = Color.WHITE // Set value text color
        barDataSet.valueTextSize = 12f // Set value text size

        // Create a BarData object
        val barData = BarData(barDataSet)

        // Set the bar data to the chart
        barChart.data = barData

        // Customize the chart appearance (optional)
        barChart.setDrawGridBackground(false) // Optional, to hide grid
        barChart.description.isEnabled = false // Disable description text
        barChart.setFitBars(true) // Optional, makes bars fit the width of the chart

        // Optional: Customize X and Y axes
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM // Position of X axis
        xAxis.setDrawGridLines(false) // Hide grid lines for X axis
        xAxis.granularity = 1f // This controls how much space is between bars

        val yAxis = barChart.axisLeft
        yAxis.setDrawGridLines(true) // Show grid lines for Y axis

        // Notify the chart to refresh and animate
        barChart.invalidate() // Refresh the chart
        barChart.animateY(1000) // Optional: animate the Y-axis bars
    }

    private fun setupSpinner() = with(binding) {
        val timeOptions = listOf("Today", "This Week", "This Month", "All Time", "Custom")

        timeRangeSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            timeOptions
        )

        timeRangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val option = TimeRangeOption.entries[position]
                if (option == TimeRangeOption.CUSTOM) {
                    // show date picker dialog
                    showCustomDatePicker { startDate, endDate ->
                        viewModel.onCustomDateRangeSelected(startDate, endDate)
                    }
                } else {
                    viewModel._selectedTimeRange.value = option
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

    }

    fun showCustomDatePicker(onRangeSelected: (startDate: LocalDate, endDate: LocalDate) -> Unit) {
        val dateRangePicker =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select date range")
                .build()

        dateRangePicker.addOnPositiveButtonClickListener { dateRange ->
            val startMillis = dateRange.first ?: return@addOnPositiveButtonClickListener
            val endMillis = dateRange.second ?: return@addOnPositiveButtonClickListener

            val startDate = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val endDate = Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDate()

            onRangeSelected(startDate, endDate)
        }

        dateRangePicker.show(requireActivity().supportFragmentManager, "DATE_RANGE_PICKER")
    }
}