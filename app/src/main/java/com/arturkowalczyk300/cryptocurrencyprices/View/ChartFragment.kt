package com.arturkowalczyk300.cryptocurrencyprices.View

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.EntityCryptocurrencyInfoInTimeRange
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModel
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModelFactory
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.*
import java.lang.Runnable
import java.text.DecimalFormat
import kotlin.collections.ArrayList

class ChartFragment : Fragment(R.layout.fragment_chart) {
    private lateinit var appContext: Context
    private lateinit var viewModel: CryptocurrencyPricesViewModel

    private lateinit var chart: LineChart
    private lateinit var chartValues: ArrayList<Entry>
    private lateinit var groupChartWithOptions: Group
    private lateinit var groupChartMinMaxAvgPrices: Group
    private lateinit var progressBarChartLoading: ProgressBar
    private lateinit var groupUpdating: Group
    private lateinit var chartRadioGroupTimeRange: RadioGroup
    private lateinit var valueFormatter: ValueFormatter
    private lateinit var tvChartMinPrice: TextView
    private lateinit var tvChartAvgPrice: TextView
    private lateinit var tvChartMaxPrice: TextView
    private lateinit var tvMainActivityCryptocurrencySymbol: TextView
    private lateinit var tvMainActivityCryptocurrencyDate: TextView
    private lateinit var tvTrending: TextView
    private lateinit var ivTrending: ImageView
    private lateinit var tvTimePeriod: TextView

    private var chartDataSet = LineDataSet(listOf(), "")
    var isInitialized = false


    override fun onViewCreated(
        view: View, savedInstanceState: Bundle?,
    ) { //view creation already done
        super.onViewCreated(view, savedInstanceState)

        appContext = requireActivity().applicationContext
        initViewModel()

        assignViewsVariablesChart()
        handleChartRadioGroupTimeRangeActions()
        initializeChart()

        isInitialized = true
    }

    private fun initViewModel() {
        val factory = CryptocurrencyPricesViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(
            requireActivity(), factory
        ).get(CryptocurrencyPricesViewModel::class.java)
    }

    fun updateData() {
        getIfInternetConnectionAndObserveLiveDataPriceHistoryForDateRange()
    }

    private fun showNoDataInfo(show: Boolean) { //todo: move it into viewmodel as property
        if (show) {
            viewModel.noCachedDataVisibility = true
            CoroutineScope(Dispatchers.Default).async {
                delay(1000)
                if (viewModel.noCachedDataVisibility) //check again, maybe new record has been added meanwhile
                {
                    chart.data = null
                    chart.invalidate()
                    chart.notifyDataSetChanged()
                    setChartLoadingProgressBarVisibility(false)
                    viewModel.currentlyDisplayedDataUpdatedMinutesAgo.postValue(null)
                    viewModel.noCachedData.postValue(true)

                    setChartVisibility(false) //no valid data to display
                    setChartAxisLabelsVisibility(false)
                }
            }
        } else {
            viewModel.noCachedData.postValue(false)
            viewModel.noCachedDataVisibility = false
        }
    }

    private fun getIfInternetConnectionAndObserveLiveDataPriceHistoryForDateRange() {
        hideMarker()
        setChartLoadingProgressBarVisibility(true)

        viewModel.selectedCryptocurrencyId?.let { _ ->
            if (viewModel.hasInternetConnection)
                viewModel.updateCryptocurrenciesInfoInDateRange()

            viewModel.updateCryptocurrenciesInfoInDateRange()

            val lifecycleOwner = requireActivity()
            val liveData = viewModel.cryptocurrenciesInfoInTimeRange!!

                viewModel.cryptocurrenciesInfoInTimeRange?.observe(
                    lifecycleOwner,
                    object :
                        androidx.lifecycle.Observer<List<EntityCryptocurrencyInfoInTimeRange>> {
                        override fun onChanged(list: List<EntityCryptocurrencyInfoInTimeRange>?) {
                            var isResponseHandled = false
                            if(list!=null && list.isNotEmpty()){
                                if (!list.isNullOrEmpty() && !list.last().prices.list.isNullOrEmpty()) {
                                    var chartData = arrayListOf<Entry>()
                                    list.last().prices.list.forEachIndexed { index, currentRow ->
                                        chartData.add(
                                            Entry(
                                                currentRow.unixTime.toFloat(),
                                                currentRow.value.toFloat()
                                            )
                                        )
                                    }
                                    setChartData(chartData)
                                    setMinAvgMaxPricesValues(chartData)
                                    updateTimePeriod()
                                    updatePriceTrends()
                                    setChartAxisLabelsVisibility(true)
                                    showNoDataInfo(false) //hide
                                    isResponseHandled = true
                                }
                            }
                            if(isResponseHandled)
                                liveData!!.removeObserver(this) //only when valid data is handled
                            else
                                showNoDataInfo(true)
                        }
                    })

        }
    }

    private fun setMinAvgMaxPricesValues(values: ArrayList<Entry>) {
        val min: Float = (values.minByOrNull { it.y }?.y) ?: -1.0f
        val max: Float = (values.maxByOrNull { it.y }?.y) ?: -1.0f

        val avg = values.map { it.y }.average()

        tvChartMinPrice.text = valueFormatter.getFormattedValue(min)
        tvChartMaxPrice.text = valueFormatter.getFormattedValue(max)
        tvChartAvgPrice.text = valueFormatter.getFormattedValue(avg.toFloat())
    }


    private fun handleChartRadioGroupTimeRangeActions() {
        val savedId = chartRadioGroupTimeRange.checkedRadioButtonId
        chartRadioGroupTimeRange.clearCheck() //before listener has been set, to avoid double call

        chartRadioGroupTimeRange.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->

            if (checkedId != -1) { //if anything selected
                var countOfDays: Int = 0

                when (chartRadioGroupTimeRange.checkedRadioButtonId) {
                    R.id.chartRadioButtonTimeRange24Hours -> countOfDays = 1
                    R.id.chartRadioButtonTimeRangeOneWeek -> countOfDays = 7
                    R.id.chartRadioButtonTimeRangeOneMonth -> countOfDays = 31
                    R.id.chartRadioButtonTimeRangeOneYear -> countOfDays = 365
                }

                viewModel.selectedDaysToSeeOnChart = countOfDays
                viewModel.recalculateTimeRange()


                updateData()
            }
        })

        val radioButtonView = requireView().findViewById<RadioButton>(savedId)
        radioButtonView.performClick() //avoid double call of listener
    }

    private fun initializeChart() {
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setTouchEnabled(true)
        chart.setDrawBorders(false)

        updateTimePeriod()
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        chart.setNoDataText(getString(R.string.chart_no_cached_data))
        chart.getPaint(Chart.PAINT_INFO).textSize = 35f

        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawLabels(false)
        chart.xAxis.setDrawAxisLine(false)

        chart.axisLeft.setDrawAxisLine(false)
        chart.axisLeft.textSize = 15f //increase default text size
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.textColor = ContextCompat.getColor(appContext, R.color.chart_font_color)
        chart.axisLeft.setLabelCount(6, true)

        chart.axisRight.setDrawAxisLine(false)
        chart.axisRight.isEnabled = false

        chart.marker = ChartMarkerView(appContext, R.layout.chart_marker_view)

        valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val digitsNumber = 6
                val valueConverted: String = String.format("%.5f", value)

                var stringToReturn = ""

                if (valueConverted.isNotEmpty() && valueConverted.isNotBlank()) {
                    if (valueConverted.length >= digitsNumber) {
                        stringToReturn = valueConverted.substring(0, digitsNumber)
                        if (value >= 10000) stringToReturn = stringToReturn.replace(".", "")
                    } else stringToReturn =
                        valueConverted.substring(0, valueConverted.length - 1)
                }

                if (stringToReturn.last() == ',') { //delete lonely comma at end of number string if exists
                    stringToReturn = stringToReturn.substring(0, stringToReturn.length - 1)
                }

                return stringToReturn
            }
        }

        chart.axisLeft.valueFormatter = valueFormatter

        setChartVisibility(false)
        setChartLoadingProgressBarVisibility(false)
        setUpdatingProgressBarVisibility(true) //data not updated yet

        viewModel.isDataUpdatedSuccessfully.observe(requireActivity()) { success ->
            setUpdatingProgressBarVisibility(!success)
        }
    }

    private fun setChartData(values: ArrayList<Entry>) {
        chartDataSet = LineDataSet(values, "")
        chartValues = values

        chartDataSet.color = Color.BLUE
        chartDataSet.setDrawCircles(false)
        chartDataSet.setDrawHorizontalHighlightIndicator(false)
        chartDataSet.setDrawVerticalHighlightIndicator(true)
        chartDataSet.lineWidth = 3f
        chartDataSet.setDrawValues(false)

        if (chart.data == null) {
            val data = LineData(chartDataSet)
            chart.data = data
        } else {
            chart.clearValues()
            chart.data.clearValues()
            chart.data.addDataSet(chartDataSet)
        }
        chart.notifyDataSetChanged()
        chart.invalidate()

        setChartVisibility(true)
        setChartLoadingProgressBarVisibility(false)
    }

    private fun updateTimePeriod() {
        tvTimePeriod.text = when (chartRadioGroupTimeRange.checkedRadioButtonId) {
            R.id.chartRadioButtonTimeRangeOneYear -> "One year"
            R.id.chartRadioButtonTimeRangeOneMonth -> "One month"
            R.id.chartRadioButtonTimeRangeOneWeek -> "One week"
            R.id.chartRadioButtonTimeRange24Hours -> "24 hours"
            else -> "Unknown time period"
        }
    }

    private fun updatePriceTrends() {
        val trend: Float = ((chartValues.last().y / chartValues.first().y) - 1.0f) * 100.0f
        val df = DecimalFormat("#.##")

        tvTrending.text = "${df.format(trend)}%"

        val imgSource = when {
            trend < 0.0f -> {
                R.drawable.ic_trending_down
            }
            trend == 0.0f -> {
                R.drawable.ic_trending_flat
            }
            else -> {
                R.drawable.ic_trending_up
            }
        }
        ivTrending.setImageResource(imgSource)
    }

    private fun assignViewsVariablesChart() {
        val currentView = requireView()
        chart = currentView.findViewById(R.id.chart)
        groupChartWithOptions = currentView.findViewById(R.id.groupChartWithOptions)
        groupChartMinMaxAvgPrices = currentView.findViewById(R.id.groupChartMinMaxAvgPrices)
        progressBarChartLoading = currentView.findViewById(R.id.progressBarChartLoading)
        chartRadioGroupTimeRange = currentView.findViewById(R.id.chartRadioGroupTimeRange)
        groupUpdating = currentView.findViewById(R.id.groupUpdating)
        tvChartMinPrice = currentView.findViewById(R.id.tvMinPrice)
        tvChartAvgPrice = currentView.findViewById(R.id.tvAvgPrice)
        tvChartMaxPrice = currentView.findViewById(R.id.tvMaxPrice)
        tvTrending = currentView.findViewById(R.id.tvTrending)
        ivTrending = currentView.findViewById(R.id.imageViewTrending)
        tvTimePeriod = currentView.findViewById(R.id.tvTimePeriod)
        tvMainActivityCryptocurrencySymbol =
            requireActivity().findViewById(R.id.tvCryptocurrencySymbol)
        tvMainActivityCryptocurrencyDate =
            requireActivity().findViewById(R.id.tvCryptocurrencyDate)
    }

    fun setChartVisibility(visible: Boolean) {
        if (visible) {
            chart.axisLeft.setDrawLabels(true)
            groupChartWithOptions.postDelayed(Runnable { //show with delay
                groupChartWithOptions.visibility = View.VISIBLE
                groupChartMinMaxAvgPrices.visibility = View.VISIBLE
            }, 200)
        } else {
            chartDataSet.isVisible = false
            groupChartWithOptions.visibility = View.GONE
            groupChartMinMaxAvgPrices.visibility = View.GONE
            chart.invalidate()
        }
    }

    private fun setChartAxisLabelsVisibility(visible: Boolean) {
        chart.axisLeft.setDrawLabels(visible)
    }

    fun setChartLoadingProgressBarVisibility(visible: Boolean) {
        if (visible) progressBarChartLoading.visibility = View.VISIBLE
        else {
            progressBarChartLoading.postDelayed(Runnable { //hide with delay
                progressBarChartLoading.visibility = View.INVISIBLE
            }, 200)
        }
    }

    fun setUpdatingProgressBarVisibility(visible: Boolean) {
        if (visible) groupUpdating.visibility = View.VISIBLE
        else {
            groupUpdating.postDelayed(Runnable { //hide with delay
                groupUpdating.visibility = View.INVISIBLE
            }, 200)
        }
    }

    fun hideMarker() {
        chart.let {
            it.highlightValue(null)
        }
    }

    fun getGlobalVisibleRectOfChart(): Rect {
        var rect = Rect()
        chart.getGlobalVisibleRect(rect)
        return rect
    }
}