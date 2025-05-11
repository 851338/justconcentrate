package com.mobichill.justconcentration.view

import android.graphics.Color
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.gms.ads.AdRequest
import com.google.android.material.datepicker.MaterialDatePicker
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingActivity
import com.mobichill.justconcentration.constants.Constants.OTHERS.DATE_FORMATTER
import com.mobichill.justconcentration.constants.TimeRangeOption
import com.mobichill.justconcentration.databinding.ActivityViewStatsBinding
import com.mobichill.justconcentration.factory.SessionViewModelFactory
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.utils.Utils.openActivity
import com.mobichill.justconcentration.viewmodel.SessionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class ViewStatsActivity : BaseViewBindingActivity<ActivityViewStatsBinding>() {
    private val sessionViewModel: SessionViewModel by viewModels {
        SessionViewModelFactory(application)
    }
    override fun initViewBinding(): ActivityViewStatsBinding =
        ActivityViewStatsBinding.inflate(layoutInflater)

    override fun initView() {
        super.initView()
        setupSpinner()
        setObservers()
        setAds()
        setBtnOnClick()
    }

    private fun setBtnOnClick() {
        binding.btnBack.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        )

        binding.upgradeButton.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    openSubscriptionActivity()
                }
            }
        )
    }

    fun openSubscriptionActivity() = openActivity<SubscriptionActivity>()

    private fun setAds() {
        //run ads
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    private fun setObservers() {
        // Observer user Pro status
        sessionViewModel.isUserPro.observe(this) { isPro ->
            Log.d(TAG, "Pro status changed in Activity: $isPro")
            if (isPro) {
                // User is Pro
                binding.proFeaturesSection.visibility = View.VISIBLE // Show Pro stats section
                binding.upgradeButton.visibility = View.GONE
                // Maybe enable certain interactions or show more detailed charts
            } else {
                // User is Free
                binding.proFeaturesSection.visibility = View.GONE // Hide Pro stats section
                binding.upgradeButton.visibility = View.VISIBLE
                // Show placeholders or a message for Pro stats
                binding.taskCompletionRateText.text =
                    getString(R.string.upgrade_to_pro_for_task_completion_rate)
                binding.overdueCountText.text = "" // Clear
                binding.lateCountText.text = "" // Clear
            }
        }
        // Showing chart bar
        sessionViewModel.barChartData.observe(this) { sessions ->
            val nonNullSessions = sessions ?: emptyList()
            Log.d(
                TAG,
                "barChartData observer onChanged CALLED with ${nonNullSessions.size} sessions."
            )
            val aggregatedData =
                nonNullSessions.groupBy { LocalDate.parse(it.date, DATE_FORMATTER) }
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
            updateBarChart(barEntries, datesByIndex)
        }

        // Showing total time
        sessionViewModel.getTotalFocusTime.observe(this)
        { time ->
            binding.totalFocusTime.text = getString(R.string.total_focus_time, time)
        }

        // Showing session number
        sessionViewModel.sessionCount.observe(this)
        { count ->
            binding.sessionCount.text =
                getString(R.string.sessions_completed, count, if (count != 1) "s" else "")
        }

        // Showing streak
        sessionViewModel.currentStreak.observe(this)
        { streak ->
            binding.currentStreak.text =
                getString(R.string.current_streak_day, streak, if (streak != 1) "s" else "")
        }

        // --- For Pro Status ---
        // Observe data for the Focus Trend Line Chart
        sessionViewModel.focusTrendData.observe(this) { trendDataMap ->
            // Update your Line Chart library with this map (Date -> Duration)
            updateFocusTrendChart(
                binding.focusTrendLineChart,
                if (sessionViewModel.isUserPro.value == true) trendDataMap else null
            )
        }

        // Observe Session Success Rate
        sessionViewModel.sessionSuccessRate.observe(this) { rate ->
            // Update a TextView, e.g., "%.1f%%".format(rate)
            if (sessionViewModel.isUserPro.value == true && rate != null)
                binding.sessionSuccessRateText.text =
                    getString(R.string.session_success_rate_format, rate)
        }

        // Observe Task Completion Rate
        sessionViewModel.taskCompletionRate.observe(this) { rate ->
            if (sessionViewModel.isUserPro.value == true && rate != null)
                binding.taskCompletionRateText.text =
                    getString(R.string.task_completion_rate_format, rate)
        }

        // Observe Overdue Task Analysis
        sessionViewModel.overdueTaskAnalysis.observe(this) { analysisResult ->
            // Update TextViews with counts like analysisResult.currentlyOverdue, etc.
            if (sessionViewModel.isUserPro.value == true && analysisResult != null) {
                val overDue = analysisResult.currentlyOverdue
                val late = analysisResult.completedLate
                binding.overdueCountText.text =
                    getString(R.string.over_due, overDue, if (overDue != 1) "s" else "")
                binding.lateCountText.text =
                    getString(R.string.completed_late, late, if (late != 1) "s" else "")
                // ... potentially update visibility or styling based on results
            }
        }
    }

    private fun updateBarChart(
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
                return dates[value]?.format(DateTimeFormatter.ofPattern("MMM dd, yy")) // Lookup date by index then get & format
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

    private fun updateFocusTrendChart(chart: LineChart, trendDataMap: Map<String, Int>?) {
        Log.d("LineChartDebug", "updateFocusTrendChart called. trendDataMap: $trendDataMap")
        if (trendDataMap.isNullOrEmpty()) {
            chart.clear() // Clear previous data
            chart.data = null // Ensure data object is null
            chart.setNoDataText("No focus data available for the selected period.")
            chart.invalidate() // Refresh the chart to show "No data" text
            return
        }

        // 1. Prepare data entries and labels (SORTED BY DATE)
        val entries = mutableListOf<Entry>()
        val xAxisLabels = mutableListOf<String>() // Store formatted date labels

        // Sort the map by date keys
        val sortedData = trendDataMap.entries
            .mapNotNull { entry ->
                try {
                    LocalDate.parse(entry.key, DATE_FORMATTER) to entry.value
                } catch (e: Exception) {
                    Log.e(TAG, "Passing date key:${entry.key} & value:${entry.value} error: ", e)
                    null
                }
            }
            .sortedBy { it.first } // Sort by LocalDate

        // Create Entries and Labels from sorted data
        sortedData.forEachIndexed { index, pair ->
            val date = pair.first
            val durationMinutes = pair.second
            entries.add(Entry(index.toFloat(), durationMinutes.toFloat()))

            // Format the date for the X-axis label (e.g., "Mon 10/28")
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val formattedDate = date.format(DateTimeFormatter.ofPattern("M/d")) // e.g., 10/28
            xAxisLabels.add("$dayOfWeek $formattedDate")
        }

        // IF ENTRIES IS EMPTY AFTER PROCESSING (e.g., all parsing failed), THEN ALSO CLEAR
        if (entries.isEmpty()) {
            Log.d(
                "LineChartDebug",
                "Entries list is empty after processing trendDataMap. Clearing chart."
            )
            chart.clear()
            chart.data = null
            chart.setNoDataText("Could not process focus data for the selected period.") // Or original message
            chart.invalidate()
            return
        }

        // 2. Create LineDataSet
        val dataSet = LineDataSet(entries, getString(R.string.focus_duration_label))

        // --- Style the DataSet ---
        dataSet.color = ContextCompat.getColor(this, R.color.chart_primary)
        dataSet.valueTextColor = Color.BLACK
        dataSet.lineWidth = 2f
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.chart_secondary))
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 10f
        dataSet.setDrawValues(true) // Show duration values on the chart points

        // Format values shown on chart points (optional, e.g., add "m")
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return entry?.y?.toInt()?.toString() ?: "" // Just show the integer minute value
                // return "${entry?.y?.toInt() ?: ""}m" // Or add "m"
            }
        }

        // 3. Create LineData
        val lineDataSets = mutableListOf<ILineDataSet>()
        lineDataSets.add(dataSet)
        val lineData = LineData(lineDataSets)

        // 4. Configure X-Axis
        val xAxis: XAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        val desiredGranularity = if (entries.size > 14) 3f else if (entries.size > 7) 2f else 1f
        xAxis.granularity = desiredGranularity
        xAxis.setDrawGridLines(false) // Hide vertical grid lines
        xAxis.isGranularityEnabled = true // Control label skipping
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(
                value: Float,
                axis: com.github.mikephil.charting.components.AxisBase?
            ): String {
                val index = value.toInt()
                // Return the pre-formatted label if index is valid
                return if (index >= 0 && index < xAxisLabels.size) {
                    xAxisLabels[index]
                } else {
                    "" // Return empty string for invalid indices
                }
            }
        }
        // Adjust label count if needed, though granularity=1f usually works with index
        // xAxis.setLabelCount(xAxisLabels.size, true)

        // 5. Configure Y-Axis (Left)
        val yAxisLeft = chart.axisLeft
        yAxisLeft.axisMinimum = 0f // Duration cannot be negative
        yAxisLeft.setDrawGridLines(true) // Show horizontal grid lines
        // Format Y-axis labels (optional, e.g., add " min")
        yAxisLeft.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(
                value: Float,
                axis: com.github.mikephil.charting.components.AxisBase?
            ): String {
                return "${value.toInt()} min"
            }
        }

        // 6. Configure Y-Axis (Right) - Disable it
        chart.axisRight.isEnabled = false

        // 7. Configure Chart Appearance
        chart.description.isEnabled = false // Hide description label
        chart.legend.isEnabled = true // Show legend (usually below chart)
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true) // Allow pinch zooming

        // 8. Set Data and Refresh
        chart.data = lineData
        chart.animateX(500) // Optional: Add a simple animation
        // chart.invalidate() // No need to call invalidate() after setting data if animateX is used
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
                        sessionViewModel.onCustomDateRangeSelected(startDate, endDate)
                    }
                } else {
                    sessionViewModel.setTimeRange(option)
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