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
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.ads.AdRequest
import com.google.android.material.datepicker.MaterialDatePicker
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentViewStatsBinding
import com.mobichill.justconcentration.factory.SessionViewModelFactory
import com.mobichill.justconcentration.others.Constants.OTHERS.DATE_FORMATTER
import com.mobichill.justconcentration.others.TimeRangeOption
import com.mobichill.justconcentration.viewmodel.SessionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        // Showing chart bar
        viewModel.sessionStats.observe(viewLifecycleOwner) { sessions ->
            val barEntries = sessions
                .groupBy { LocalDate.parse(it.date, DATE_FORMATTER) }
                .map { (date, sessionList) ->
                    BarEntry(
                        date.toEpochDay().toFloat(),
                        sessionList.sumOf { it.durationMinutes }.toFloat()
                    )
                }
                .sortedBy { it.x } // sort by date

            updateChart(barEntries)
        }
        // Showing total time
        viewModel.getTotalFocusTime.observe(viewLifecycleOwner) { time ->
            binding.totalFocusTime.text = getString(R.string.total_focus_time, time)
        }
        // Showing session number
        viewModel.sessionCount.observe(viewLifecycleOwner) { count ->
            binding.sessionCount.text = getString(R.string.sessions_completed, count)
        }
        // Showing streak
        viewModel.currentStreak.observe(viewLifecycleOwner) { streak ->
            binding.currentStreak.text =
                getString(R.string.current_streak_day, streak, if (streak != 1) "s" else "")
        }
        //run ads
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    private fun updateChart(chartData: List<BarEntry>) = with(binding) {
        val barDataSet = BarDataSet(chartData, getString(R.string.session_stats_chart_label))

        barDataSet.color = Color.BLUE // Set bar color
        barDataSet.valueTextColor = Color.WHITE // Set value text color
        barDataSet.valueTextSize = 12f // Set value text size

        // Create a BarData object
        val barData = BarData(barDataSet)

        // Set the bar data to the chart
        barChart.data = barData

        // Customize the chart appearance
        barChart.setDrawGridBackground(false) // To hide grid
        barChart.description.isEnabled = false // Disable description text
        barChart.setFitBars(true) // Makes bars fit the width of the chart

        // Customize X and Y axes
        val xAxis = barChart.xAxis
        //
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val date = LocalDate.ofEpochDay(value.toLong())
                return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            }
        }
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

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            timeOptions
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        timeRangeSpinner.adapter = adapter

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

            val startDate =
                Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val endDate =
                Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDate()

            onRangeSelected(startDate, endDate)
        }

        dateRangePicker.show(requireActivity().supportFragmentManager, "DATE_RANGE_PICKER")
    }
}