package com.mobichill.justconcentration.view

import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
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
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.base.application.MyApp
import com.mobichill.justconcentration.constants.Constants.OTHERS.DATE_FORMATTER
import com.mobichill.justconcentration.constants.TimeRangeOption
import com.mobichill.justconcentration.databinding.ActivityViewStatsBinding
import com.mobichill.justconcentration.factory.SessionViewModelFactory
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.viewmodel.SessionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ViewStatsActivity : BaseViewBindingActivity<ActivityViewStatsBinding>() {
    private lateinit var viewModel: SessionViewModel

    override fun initViewBinding(): ActivityViewStatsBinding =
        ActivityViewStatsBinding.inflate(layoutInflater)

    override fun initViewModel() {
        super.initViewModel()
        viewModel = ViewModelProvider(
            this,
            SessionViewModelFactory(MyApp.instance.concentrateSessionRepository)
        )[SessionViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initView() {
        super.initView()
        setupSpinner()
        // Testing
        viewModel.allSessions.observeForever { sessions ->
            Log.d("LiveDataDebug", "Sessions updated:")
            sessions.forEach { Log.d("LiveDataDebug", it.toString()) }
        }
        // Showing chart bar
        viewModel.sessionStats.observe(this) { sessions ->
            Log.d("ChartDebug", "SessionStats Observer: Received ${sessions.size} sessions.")
            // Group by date and calculate SUMS for completed and incomplete separately
            val aggregatedData =
                sessions.groupBy { LocalDate.parse(it.date, DATE_FORMATTER) }
                    .mapValues { (_, sessionList) ->
                        val completedSum = sessionList.filter { it.wasCompleted }
                            .sumOf { it.durationMinutes }.toFloat()
                        val incompleteSum = sessionList.filter { !it.wasCompleted }
                            .sumOf { it.durationMinutes }.toFloat()
                        // Pair of sums: first = completed, second = incomplete
                        Pair(completedSum, incompleteSum)
                    }
                    .toList()
                    .sortedBy { it.first } // Sort by date

            // Create BarEntries with stacks and store dates for formatter
            val barEntries = mutableListOf<BarEntry>()
            // Map date to index to avoid epochDate too big
            val datesByIndex = mutableMapOf<Float, LocalDate>()

            aggregatedData.forEachIndexed { index, (date, sums) ->
                val xValue = index.toFloat() // Use index 0, 1, 2... as X
                val completedDuration = sums.first
                val incompleteDuration = sums.second

                // Create a stacked BarEntry:
                barEntries.add(
                    BarEntry(
                        xValue,
                        floatArrayOf(completedDuration, incompleteDuration)
                    )
                )
                datesByIndex[xValue] = date  // Store date by index
                Log.d(
                    "ChartDebugEntry", // Changed tag slightly to distinguish
                    "Date: $date, Index: $xValue, Completed: $completedDuration, Incomplete: $incompleteDuration"
                )
            }
            Log.d(
                "ChartDebug",
                "Processed ${barEntries.size} Stacked BarEntries. Max X (index): ${barEntries.lastOrNull()?.x}"
            )
            updateChart(barEntries, datesByIndex)
        }
        // Showing total time
        viewModel.getTotalFocusTime.observe(this)
        { time ->
            binding.totalFocusTime.text = getString(R.string.total_focus_time, time)
        }
        // Showing session number
        viewModel.sessionCount.observe(this)
        { count ->
            binding.sessionCount.text = getString(R.string.sessions_completed, count)
        }
        // Showing streak
        viewModel.currentStreak.observe(this)
        { streak ->
            binding.currentStreak.text =
                getString(R.string.current_streak_day, streak, if (streak != 1) "s" else "")
        }

        binding.btnBack.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    onBackPressed()
                }
            }
        )

        //run ads
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    private fun updateChart(
        chartData: List<BarEntry>,
        dates: Map<Float, LocalDate>
    ) = with(binding) {
        // --- THREAD CHECK ---
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e("ChartDebug", "!!! updateChart CALLED ON WRONG THREAD !!!")
            // --- Google studio suggestion: ---
            // Consider posting to the main thread instead of just returning
            // Handler(Looper.getMainLooper()).post { updateChart(chartData, dates) }
            return@with
        }
        Log.d("ChartDebug", "-> updateChart START - Data size: ${chartData.size}")

        val barDataSet = BarDataSet(chartData, getString(R.string.session_stats_chart_label))

        barDataSet.setColors(
            Color.rgb(0, 0, 255),
            Color.rgb(255, 0, 0)
        ) // Set bar color for floatArrayOf(completedDuration, incompleteDuration)
        barDataSet.stackLabels = arrayOf(
            getString(R.string.session_stats_chart_stack_completed), // "Completed"
            getString(R.string.session_stats_chart_stack_incomplete) // "Incomplete"
        )

        barDataSet.valueTextColor = Color.BLACK // Set value text color
        barDataSet.valueTextSize = 10f // Set value text size
        // Create a BarData object and set the bar data to the chart
        val barData = BarData(barDataSet)
        barChart.data = barData

        // Get the actual width being used
        val barWidth = barData.barWidth
        val halfBarWidth = barWidth / 2f

        // Customize the chart appearance
        barChart.setDrawGridBackground(false) // To hide grid
        barChart.description.isEnabled = false // Disable description text
        // Enable dragging, disable zooming
        barChart.isDragEnabled = true
        barChart.setScaleEnabled(false)
        barChart.setPinchZoom(false)

        // Customize X and Y axes
        val xAxis = barChart.xAxis
        // Formatter by index
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // x value is now the index (0.0, 1.0, etc.)
                return dates[value]?.format(DateTimeFormatter.ofPattern("MMM dd")) // Lookup date by index then get & format
                    ?: run {
                        Log.w("ChartFormatter", "No date found for index: $value")
                        "" // Return empty or value.toString() if index not found
                    }
            }
        }
        xAxis.position = XAxis.XAxisPosition.BOTTOM // Position of X axis
        xAxis.setDrawGridLines(false) // Hide grid lines for X axis
        xAxis.granularity = 1f // This controls how much space is between bars

        val yAxis = barChart.axisLeft
        yAxis.setDrawGridLines(true) // Show grid lines for Y axis

        // Notify the chart to refresh and animate
        barChart.invalidate()

        if (chartData.isNotEmpty()) {
            // Determine the index of the first bar (always 0f here)
            val firstVisibleIndex = 0f
            // Determine the index of the last bar we want fully visible
            val lastIntendedVisibleIndex =
                (chartData.size - 1f).coerceAtMost(6f) // Show up to 7 bars (indices 0-6)

            /// Calculate the coordinate range needed
            val minXVisible = firstVisibleIndex - halfBarWidth
            val maxXVisible = lastIntendedVisibleIndex + halfBarWidth

            Log.d(
                "ChartDebug",
                "Setting visible X range (edges): $minXVisible to $maxXVisible for indices $firstVisibleIndex to $lastIntendedVisibleIndex"
            )
            barChart.setVisibleXRange(minXVisible, maxXVisible)

            Log.d("ChartDebug", "Calling animateY()")
            barChart.animateY(1000)
        } else {
            // Handle empty data
            barChart.clear()
            barChart.invalidate()
            Log.d("ChartDebug", "No data")
        }

        Log.d("ChartDebug", "<- updateChart END")
    }

    private fun setupSpinner() = with(binding) {
        val timeOptions = listOf("Today", "This Week", "This Month", "All Time", "Custom")

        val adapter = ArrayAdapter(
            this@ViewStatsActivity,
            R.layout.item_spinner,
            timeOptions
        )
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
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

    private fun showCustomDatePicker(onRangeSelected: (startDate: LocalDate, endDate: LocalDate) -> Unit) {
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

        dateRangePicker.show(this.supportFragmentManager, "DATE_RANGE_PICKER")
    }
}