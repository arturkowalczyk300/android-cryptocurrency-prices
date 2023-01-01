package com.arturkowalczyk300.cryptocurrencyprices.View

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.EntityCryptocurrenciesHistoricalPrices
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModel
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class ChartFragment : Fragment(R.layout.fragment_chart) {
    private lateinit var appContext: Context
    private lateinit var viewModel: CryptocurrencyPricesViewModel

    private lateinit var chart: LineChart
    private lateinit var chartValues: ArrayList<Entry>
    private lateinit var groupChartWithOptions: androidx.constraintlayout.widget.Group
    private lateinit var groupChartMinMaxAvgPrices: androidx.constraintlayout.widget.Group
    private lateinit var progressBarChartLoading: ProgressBar
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
    private var historialPricesliveDatas: MutableList<LiveData<List<EntityCryptocurrenciesHistoricalPrices>>> =
        mutableListOf<LiveData<List<EntityCryptocurrenciesHistoricalPrices>>>()

    private var chartDataSet = LineDataSet(listOf(), "")


    override fun onViewCreated(
        view: View, savedInstanceState: Bundle?
    ) { //view creation already done
        super.onViewCreated(view, savedInstanceState)

        appContext = requireActivity().applicationContext
        initViewModel()

        assignViewsVariablesChart()
        handleChartRadioGroupTimeRangeActions()
        initializeChart()
        //getAndObserveLiveDataPriceHistoryForDateRange()
    }

    private fun initViewModel() {
        val factory = CryptocurrencyPricesViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(
            requireActivity(), factory
        ).get(CryptocurrencyPricesViewModel::class.java)
    }

    fun updateDataIfConnectedToInternet()
    {
        if(viewModel.hasInternetConnection)
        getAndObserveLiveDataPriceHistoryForDateRange()
    }

    private fun getAndObserveLiveDataPriceHistoryForDateRange() {
        hideMarker()
        setChartLoadingProgressBarVisibility(true)

        viewModel.selectedCryptocurrencyId?.let { cryptocurrencyId ->
            viewModel.updatePriceHistoryForSelectedDateRange()

            historialPricesliveDatas.add( //keep references to remove all observers later
                viewModel.getHistoricalPricesOfCryptocurrencyInTimeRange(
                    cryptocurrencyId
                )
            )

            val lifecycleOwner = requireActivity()

            historialPricesliveDatas.last()
                .observe(lifecycleOwner, androidx.lifecycle.Observer { list -> //TODO: modify it

                    val chosenItem = list.sortedByDescending { it.timeRangeFrom }.find {
                        ((it.timeRangeTo - it.timeRangeFrom) / 3600 / 24).toInt() ==
                                viewModel.selectedDaysToSeeOnChart!!
                    }

                    chosenItem?.let {
                        if (!it.prices.list.isNullOrEmpty()) {
                            //create list
                            var list = arrayListOf<Entry>()
                            it.prices.list.forEachIndexed { index, currentRow ->
                                list.add(
                                    Entry(
                                        currentRow.unixTime.toFloat(),
                                        currentRow.value.toFloat()
                                    )
                                )
                            }
                            setChartData(list)
                            setMinAvgMaxPricesValues(list)
                            updateTimePeriod()
                            updatePriceTrends()
                            setChartAxisLabelsVisibility(true)

                            //remove observers to prevent multiple calls
                            historialPricesliveDatas.forEach { ld ->
                                ld.removeObservers(
                                    lifecycleOwner
                                )
                            }
                        } else {
                            setChartVisibility(false) //no valid data to display
                            setChartAxisLabelsVisibility(false)
                        }
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
                //set date range parameters
                val calendar = Calendar.getInstance()
                if (viewModel.showArchivalData) {
                    calendar.time = viewModel.showArchivalDataRange
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
                val dateEnd = calendar.time

                var countOfDays: Int = 0

                when (chartRadioGroupTimeRange.checkedRadioButtonId) {
                    R.id.chartRadioButtonTimeRangeOneYear -> calendar.add(Calendar.YEAR, -1)
                        .also { countOfDays = 365 }
                    R.id.chartRadioButtonTimeRangeOneMonth -> calendar.add(Calendar.MONTH, -1)
                        .also { countOfDays = 31 }
                    R.id.chartRadioButtonTimeRangeOneWeek -> calendar.add(
                        Calendar.DAY_OF_MONTH,
                        -7
                    )
                        .also { countOfDays = 7 }
                    R.id.chartRadioButtonTimeRange24Hours -> calendar.add(
                        Calendar.DAY_OF_MONTH,
                        -1
                    )
                        .also { countOfDays = 1 }
                }
                val dateStart = calendar.time

                viewModel.selectedUnixTimeFrom = (dateStart.time / 1000)
                viewModel.selectedUnixTimeTo = (dateEnd.time / 1000)
                viewModel.selectedDaysToSeeOnChart = countOfDays

                updateDataIfConnectedToInternet()
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
        //chart.animateX(1000)
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
                progressBarChartLoading.visibility = View.GONE
            }, 200)

        }
    }

    fun hideMarker() {
        chart.highlightValue(null)
    }

    fun getGlobalVisibleRectOfChart(): Rect {
        var rect = Rect()
        chart.getGlobalVisibleRect(rect)
        return rect
    }
}