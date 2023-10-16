package com.arturkowalczyk300.cryptocurrencyprices.view

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.arturkowalczyk300.cryptocurrencyprices.model.room.InfoWithinTimeRangeEntity
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.viewModel.MainViewModel
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.lang.Runnable
import java.text.DecimalFormat
import kotlin.collections.ArrayList

enum class ChartMode {
    NORMAL,
    VOLUME,
    CANDLESTICK
}


@AndroidEntryPoint
class ChartFragment : Fragment(R.layout.fragment_chart) {
    @ApplicationContext
    lateinit var appContext: Context
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var linearChart: LineChart
    private lateinit var candlestickChart: CandleStickChart
    private lateinit var chartValues: ArrayList<Entry>
    private lateinit var groupChartWithOptions: Group
    private lateinit var groupChartMinMaxAvgPrices: Group
    private lateinit var progressBarChartLoading: ProgressBar
    private lateinit var groupUpdating: Group
    private lateinit var chartRadioGroupTimeRange: RadioGroup
    private lateinit var chartRadioGroupMode: RadioGroup
    private lateinit var valueFormatter: ValueFormatter
    private lateinit var tvChartMinPrice: TextView
    private lateinit var tvChartAvgPrice: TextView
    private lateinit var tvChartMaxPrice: TextView
    private lateinit var tvMainActivityCryptocurrencySymbol: TextView
    private lateinit var tvMainActivityCryptocurrencyDate: TextView
    private lateinit var tvTrending: TextView
    private lateinit var ivTrending: ImageView
    private lateinit var tvTimePeriod: TextView

    private var chartPriceDataSet = LineDataSet(listOf(), "")

    private var chartMode: ChartMode = ChartMode.NORMAL
    override fun onViewCreated(
        view: View, savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        appContext = requireActivity().applicationContext

        assignViewsVariablesChart()
        handleChartRadioGroupTimeRangeActions()
        handleChartRadioGroupMode()
        initializeLinearChart()
        initializeCandlestickChart()
        observeDataUpdatedVariable()

        viewModel.isChartFragmentInitialized = true
    }

    private fun observeDataUpdatedVariable() {
        viewModel.isCurrencyChartDataLoadedFromCache.observe(
            requireActivity()
        ) { loaded ->
            if (loaded){
                observeChartData()
                observeOhlcChartData()
                }

            setUpdatingProgressBarVisibility(!loaded)
        }
    }

    private fun observeChartData() {
        val liveData = viewModel.cryptocurrencyChartData!!

        viewModel.cryptocurrencyChartData?.observe(
            requireActivity(),
            object :
                Observer<InfoWithinTimeRangeEntity?> {
                override fun onChanged(info: InfoWithinTimeRangeEntity?) {
                    var isResponseHandled = false
                    if (info != null) {
                        setChartData(info)
                        updateTimePeriod()
                        updatePriceTrends()
                        setChartAxisLabelsVisibility(true)
                        showNoDataInfo(false) //hide
                        isResponseHandled = true
                    }
                    if (isResponseHandled)
                        liveData.removeObserver(this) //only when valid data is handled
                    else {
                        //showNoDataInfo(true) //TODO: restore this
                    }
                }
            })
    }

    private fun observeOhlcChartData() {
        val liveData = viewModel.cryptocurrencyOhlcData!!

        viewModel.cryptocurrencyOhlcData?.observe(
            requireActivity(),
            object :
                Observer<InfoWithinTimeRangeEntity?> {
                override fun onChanged(info: InfoWithinTimeRangeEntity?) {
                    var isResponseHandled = false
                    if (info != null) {
                        setChartData(info)
                        updateTimePeriod()
                        updatePriceTrends()
                        setChartAxisLabelsVisibility(true)
                        //showNoDataInfo(false) //hide
                        isResponseHandled = true
                    }
                    if (isResponseHandled)
                        liveData.removeObserver(this) //only when valid data is handled
                    else {
                        //showNoDataInfo(true)
                    }
                }
            })
    }

    private fun showNoDataInfo(show: Boolean) {
        if (show) {
            viewModel.noDataCachedVisibility = true
            CoroutineScope(Dispatchers.Default).async {
                delay(1000)
                if (viewModel.noDataCachedVisibility) //check again, maybe new record has been added meanwhile
                {
                    setLinearChartVisibility(true)
                    linearChart.data = null
                    linearChart.invalidate()
                    linearChart.notifyDataSetChanged()
                    setChartLoadingProgressBarVisibility(false)
                    viewModel.setCurrentlyDisplayedDataUpdatedMinutesAgo(null)

                    //setChartVisibility(false) //no valid data to display
                    setChartAxisLabelsVisibility(false)
                }
            }
        } else {
            viewModel.noDataCachedVisibility = false
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
        chartRadioGroupTimeRange.setOnCheckedChangeListener { _, checkedId ->

            if (checkedId != -1) { //if anything is selected
                var countOfDays: Int = 0

                when (chartRadioGroupTimeRange.checkedRadioButtonId) {
                    R.id.chartRadioButtonTimeRange24Hours -> countOfDays = 1
                    R.id.chartRadioButtonTimeRangeOneWeek -> countOfDays = 7
                    R.id.chartRadioButtonTimeRangeOneMonth -> countOfDays = 31
                    R.id.chartRadioButtonTimeRangeOneYear -> countOfDays = 365
                }

                viewModel.selectedDaysToSeeOnChart = countOfDays
                viewModel.recalculateTimeRange()
                viewModel.requestUpdateAllData()
            }
        }

        viewModel.selectedDaysToSeeOnChart = 1 //default value
    }

    private fun handleChartRadioGroupMode() {
        chartRadioGroupMode.setOnCheckedChangeListener { _, checkedId ->

            if (checkedId != -1) { //if anything is selected
                when (chartRadioGroupMode.checkedRadioButtonId) {
                    R.id.chartRadioButtonModeNormal -> {
                        chartMode = ChartMode.NORMAL
                    }

                    R.id.chartRadioButtonModeVolume -> {
                        chartMode = ChartMode.VOLUME
                    }

                    R.id.chartRadioButtonModeCandlestick -> {
                        chartMode = ChartMode.CANDLESTICK
                    }
                }

                viewModel.historicalDataMode =
                    if (chartMode == ChartMode.CANDLESTICK) MainViewModel.HistoricalDataMode.OHLC
                    else MainViewModel.HistoricalDataMode.NORMAL

                if (viewModel.cryptocurrencyChartData?.value != null)
                    setChartData(viewModel.cryptocurrencyChartData!!.value!!)

                viewModel.recalculateTimeRange()
                viewModel.requestUpdateAllData()
            }
        }
    }

    private fun initializeLinearChart() {
        linearChart.setBackgroundColor(Color.TRANSPARENT)
        linearChart.setTouchEnabled(true)
        linearChart.setDrawBorders(false)

        updateTimePeriod()
        linearChart.description.isEnabled = false
        linearChart.legend.isEnabled = false

        linearChart.setNoDataText(getString(R.string.chart_no_cached_data))
        linearChart.getPaint(Chart.PAINT_INFO).textSize = 35f

        linearChart.xAxis.setDrawGridLines(false)
        linearChart.xAxis.setDrawLabels(false)
        linearChart.xAxis.setDrawAxisLine(false)

        linearChart.axisLeft.setDrawAxisLine(false)
        linearChart.axisLeft.textSize = 15f //increase default text size
        linearChart.axisLeft.setDrawGridLines(false)
        linearChart.axisLeft.textColor =
            ContextCompat.getColor(appContext, R.color.chart_font_color)
        linearChart.axisLeft.setLabelCount(6, true)

        linearChart.axisRight.setDrawAxisLine(false)
        linearChart.axisRight.isEnabled = false

        linearChart.marker = ChartMarkerView(appContext, R.layout.chart_marker_view)

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

        linearChart.axisLeft.valueFormatter = valueFormatter

        setLinearChartVisibility(false)
        setChartLoadingProgressBarVisibility(false)
    }

    private fun initializeCandlestickChart() {
        candlestickChart.isHighlightPerDragEnabled = true
        candlestickChart.setDrawBorders(true)
//        candlestickChart.setBorderColor(resources.getColor(R.color.lightGray))

        val yAxis = candlestickChart.axisLeft
        val rightAxis = candlestickChart.axisRight
        yAxis.setDrawGridLines(false)
        rightAxis.setDrawGridLines(false)
        candlestickChart.requestDisallowInterceptTouchEvent(true)

        val xAxis = candlestickChart.xAxis

        xAxis.setDrawGridLines(false)
        xAxis.setDrawLabels(false)
        rightAxis.textColor = Color.WHITE
        yAxis.setDrawLabels(false)
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.setAvoidFirstLastClipping(true)

        val l = candlestickChart.legend
        l.isEnabled = false
    }

    private fun setChartData(chartData: InfoWithinTimeRangeEntity) {
        var pricesData = arrayListOf<Entry>()
        chartData.prices?.list?.forEachIndexed { _, currentRow ->
            pricesData.add(
                Entry(
                    currentRow.unixTime.toFloat(),
                    currentRow.value.toFloat()
                )
            )
        }
        setMinAvgMaxPricesValues(pricesData)

        when (chartMode) {
            ChartMode.NORMAL ->
                setChartPriceData(pricesData)

            ChartMode.VOLUME ->
                setChartVolumeData(chartData)

            ChartMode.CANDLESTICK -> {
                setCandlestickChartData(chartData)
            }
        }
    }

    private fun setCandlestickChartData(chartData: InfoWithinTimeRangeEntity) {
        setLinearChartVisibility(false)
        setCandlestickChartVisibility(true)

        if (chartData.ohlcData == null)
            return

        val yValuesCandleStick = chartData.ohlcData!!.mapIndexed {idx, it->
            CandleEntry(
                //it.timestamp.toFloat(), TODO: make chart handle this value
                idx.toFloat(), //TODO: delete after fix above
                it.high.toFloat(),
                it.low.toFloat(),
                it.open.toFloat(),
                it.close.toFloat()
            )
        }

        //set data
        val set1 = CandleDataSet(yValuesCandleStick, "DataSet1")
        set1.color = Color.rgb(80, 80, 80)
        set1.shadowColor = resources.getColor(R.color.colorLightGray2)
        set1.shadowWidth = 5.0f
        set1.decreasingColor = resources.getColor(R.color.colorRed)
        set1.decreasingPaintStyle = Paint.Style.FILL
        set1.increasingColor = resources.getColor(R.color.colorAccent)
        set1.increasingPaintStyle = Paint.Style.FILL
        set1.neutralColor = Color.LTGRAY
        set1.setDrawValues(false)

        val data = CandleData(set1)
        candlestickChart.data = data
        candlestickChart.invalidate()
    }

    private fun setChartPriceData(values: ArrayList<Entry>) {
        setLinearChartVisibility(true)
        setCandlestickChartVisibility(false)

        chartPriceDataSet = LineDataSet(values, "")
        chartValues = values

        chartPriceDataSet.color = Color.BLUE
        chartPriceDataSet.setDrawCircles(false)
        chartPriceDataSet.setDrawHorizontalHighlightIndicator(false)
        chartPriceDataSet.setDrawVerticalHighlightIndicator(true)
        chartPriceDataSet.lineWidth = 3f
        chartPriceDataSet.setDrawValues(false)

        if (linearChart.data == null) {
            val data = LineData(chartPriceDataSet)
            linearChart.data = data
        } else {
            linearChart.clearValues()
            linearChart.data.clearValues()
            linearChart.data.addDataSet(chartPriceDataSet)
        }
        linearChart.notifyDataSetChanged()
        linearChart.invalidate()

        setLinearChartVisibility(true)
        setChartLoadingProgressBarVisibility(false)
    }

    private fun setChartVolumeData(data: InfoWithinTimeRangeEntity) {
        setLinearChartVisibility(true)
        setCandlestickChartVisibility(false)

        var values = arrayListOf<Entry>()
        data.total_volumes?.list?.forEachIndexed { _, currentRow ->
            values.add(
                Entry(
                    currentRow.unixTime.toFloat(),
                    currentRow.value.toFloat()
                )
            )
        }

        chartPriceDataSet = LineDataSet(values, "")
        chartValues = values

        chartPriceDataSet.color = Color.RED
        chartPriceDataSet.setDrawCircles(false)
        chartPriceDataSet.setDrawHorizontalHighlightIndicator(false)
        chartPriceDataSet.setDrawVerticalHighlightIndicator(true)
        chartPriceDataSet.lineWidth = 3f
        chartPriceDataSet.setDrawValues(false)

        if (linearChart.data == null) {
            val data = LineData(chartPriceDataSet)
            linearChart.data = data
        } else {
            linearChart.clearValues()
            linearChart.data.clearValues()
            linearChart.data.addDataSet(chartPriceDataSet)
        }
        linearChart.notifyDataSetChanged()
        linearChart.invalidate()

        setLinearChartVisibility(true)
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
        if(chartValues.isEmpty())
            return

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
        linearChart = currentView.findViewById(R.id.linearChart)
        candlestickChart = currentView.findViewById(R.id.candlestickChart)
        groupChartWithOptions = currentView.findViewById(R.id.groupChartWithOptions)
        groupChartMinMaxAvgPrices = currentView.findViewById(R.id.groupChartMinMaxAvgPrices)
        progressBarChartLoading = currentView.findViewById(R.id.progressBarChartLoading)
        chartRadioGroupTimeRange = currentView.findViewById(R.id.chartRadioGroupTimeRange)
        chartRadioGroupMode = currentView.findViewById(R.id.chartRadioGroupMode)
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

    fun setLinearChartVisibility(visible: Boolean) {
        if (visible) {
            linearChart.axisLeft.setDrawLabels(true)
            groupChartWithOptions.postDelayed(Runnable { //show with delay
                groupChartWithOptions.visibility = View.VISIBLE
                groupChartMinMaxAvgPrices.visibility = View.VISIBLE
            }, 200)
        } else {
            chartPriceDataSet.isVisible = false
            groupChartWithOptions.visibility = View.GONE
            groupChartMinMaxAvgPrices.visibility = View.GONE
            linearChart.invalidate()
        }
    }

    private fun setCandlestickChartVisibility(visible: Boolean) {
        if (visible) {
            candlestickChart.visibility = View.VISIBLE
        } else {
            candlestickChart.visibility = View.GONE
        }
    }

    private fun setChartAxisLabelsVisibility(visible: Boolean) {
        linearChart.axisLeft.setDrawLabels(visible)
    }

    private fun setChartLoadingProgressBarVisibility(visible: Boolean) { //for cached data
        if (visible) progressBarChartLoading.visibility = View.VISIBLE
        else {
            progressBarChartLoading.postDelayed(Runnable { //hide with delay
                progressBarChartLoading.visibility = View.INVISIBLE
            }, 200)
        }
    }

    private fun setUpdatingProgressBarVisibility(visible: Boolean) { //for data from remote
        if (visible) groupUpdating.visibility = View.VISIBLE
        else {
            groupUpdating.postDelayed(Runnable { //hide with delay
                groupUpdating.visibility = View.INVISIBLE
            }, 200)
        }
    }

    fun hideMarker() {
        if (this::linearChart.isInitialized) {
            linearChart.let {
                it.highlightValue(null)
            }
        }
    }

    fun getGlobalVisibleRectOfChart(): Rect {
        var rect = Rect()
        linearChart.getGlobalVisibleRect(rect)
        return rect
    }
}

