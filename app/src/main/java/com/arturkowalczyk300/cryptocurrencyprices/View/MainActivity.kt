package com.arturkowalczyk300.cryptocurrencyprices.View

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesEntityDb
import com.arturkowalczyk300.cryptocurrencyprices.NetworkAccessLiveData
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModel
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: CryptocurrencyPricesViewModel
    private lateinit var tvSelectedCurrencyId: TextView
    private lateinit var etDate: EditText
    private lateinit var btnGet: Button
    private lateinit var btnPrevRecord: Button
    private lateinit var btnNextRecord: Button
    private lateinit var tvCurrentAndMaxIndex: TextView
    private lateinit var tvCryptocurrencySymbol: TextView
    private lateinit var tvCryptocurrencyDate: TextView
    private lateinit var tvCryptocurrencyPrice: TextView
    private lateinit var tvNoInternetConnection: TextView
    private lateinit var chart: LineChart
    private lateinit var llChartWithOptions: LinearLayout
    private lateinit var progressBarChartLoading: ProgressBar
    private lateinit var chartRadioGroupTimeRange: RadioGroup

    private var isCurrenciesListInitialized: Boolean = false
    private var hasInternetConnection: Boolean = false
    private var datePickerDialog: DatePickerDialog? = null

    private var currentRecordIndex: Int = 0
    private var maxRecordIndex: Int = 0

    private var listOfRecords: List<CryptocurrencyPricesEntityDb>? = null
    private val currentSelectedDate: Calendar = Calendar.getInstance()

    private var listOfCryptocurrenciesNames: ArrayList<String> = ArrayList()
    private lateinit var defaultDateFormatter: SimpleDateFormat


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        defaultDateFormatter = SimpleDateFormat(getString(R.string.defaultDateFormat))

        assignViewsVariables()
        initViewModel()
        handleNoNetworkInfo()
        handleCryptocurrencyChoice()
        initializeDatePicker()
        handleChartRadioGroupTimeRangeActions()
        addButtonsOnClickListeners()
        observeLiveData()
        updateIndexInfo()

        initializeChart()
    }

    private fun handleChartRadioGroupTimeRangeActions() {
        chartRadioGroupTimeRange.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            requestPriceHistoryForDateRange()
        })
    }

    private fun addButtonsOnClickListeners() {
        btnGet.setOnClickListener {
            if (isCurrenciesListInitialized && hasInternetConnection) {
                var date: Date
                try {
                    date = defaultDateFormatter.parse(etDate.text.toString())
                    viewModel.requestPriceData(
                        tvSelectedCurrencyId.text.toString(),
                        date
                    )
                } catch (exc: Exception) {
                    Log.v("myApp", exc.toString())
                }
            }
            setChartVisibility(false)
            setChartLoadingProgressBarVisibility(true)
        }

        btnPrevRecord.setOnClickListener {

            currentRecordIndex = (currentRecordIndex - 1)
            if (currentRecordIndex < 0)
                currentRecordIndex = maxRecordIndex
            updateIndexInfo()
            setChartVisibility(false)
            setChartLoadingProgressBarVisibility(true)
            displayRecordByIndex(currentRecordIndex)
        }

        btnNextRecord.setOnClickListener {
            currentRecordIndex = (currentRecordIndex + 1)
            if (currentRecordIndex > maxRecordIndex)
                currentRecordIndex = 0
            updateIndexInfo()
            setChartVisibility(false)
            setChartLoadingProgressBarVisibility(true)
            displayRecordByIndex(currentRecordIndex)
        }
    }

    private fun observeLiveData() {
        viewModel.getAllReadings()?.observe(this, Observer {
            listOfRecords = it
            maxRecordIndex = it.size - 1
            updateIndexInfo()
            if (it.isNotEmpty()) {
                navigateToLastInsertedRecord()
                updateIndexInfo()
                switchVisibilityOfRecordViewer(View.VISIBLE)
            } else
                switchVisibilityOfRecordViewer(View.GONE)
        }
        )


    }

    private fun requestPriceHistoryForDateRange() {
        val currencyName = tvCryptocurrencySymbol.text.toString()

        //set date range parameters
        val calendar = Calendar.getInstance()
        calendar.time = defaultDateFormatter.parse(tvCryptocurrencyDate.text.toString())
        val dateEnd = calendar.time

        when (chartRadioGroupTimeRange.checkedRadioButtonId) {
            R.id.chartRadioButtonTimeRangeOneYear -> calendar.add(Calendar.YEAR, -1)
            R.id.chartRadioButtonTimeRangeOneMonth -> calendar.add(Calendar.MONTH, -1)
            R.id.chartRadioButtonTimeRangeOneWeek -> calendar.add(Calendar.DAY_OF_MONTH, -7)
            R.id.chartRadioButtonTimeRange24Hours -> calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        val dateStart = calendar.time

        setChartLoadingProgressBarVisibility(true)
        viewModel.requestPriceHistoryForDateRange(
            currencyName,
            getString(R.string.defaultVsCurrency),
            (dateStart.time / 1000),
            (dateEnd.time / 1000)
        ).observe(this, //TODO: potential memory leak!
            Observer {
                if (!it.isNullOrEmpty()) {
                    //create list
                    var list = arrayListOf<Entry>()
                    it.forEachIndexed { index, currentRow ->
                        Log.v("rawData2", "i=$index, ${currentRow[0]}, ${currentRow[1]}")
                        list.add(Entry(currentRow[0].toFloat(), currentRow[1].toFloat()))
                    }
                    setChartData(list)
                    setChartDescription()
                }
            })
    }

    private fun handleCryptocurrencyChoice() {
        viewModel.requestCryptocurrenciesList().observe(this, Observer { it ->
            listOfCryptocurrenciesNames.clear()

            it.forEach { nextIt ->
                listOfCryptocurrenciesNames.add(nextIt.id)
            }

            isCurrenciesListInitialized = true
            tvSelectedCurrencyId.text = listOfCryptocurrenciesNames.first()
        })

        tvSelectedCurrencyId.setOnClickListener {
            //display dialog
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_searchable_list)
            dialog.show()
            val listView = dialog.findViewById(R.id.dialogListView) as ListView

            //set adapter to list with cryptocurrencies
            val adapter = ArrayAdapter(this, R.layout.my_spinner_item, listOfCryptocurrenciesNames)
            adapter.setDropDownViewResource(R.layout.my_spinner_item)
            listView.adapter = adapter
            listView.setOnItemClickListener { parent, view, position, id ->
                tvSelectedCurrencyId.text = listView.adapter.getItem(position).toString()
                dialog.dismiss()
            }

            //handle search filter
            val editTextFilter = dialog.findViewById(R.id.dialogEtFilter) as EditText
            editTextFilter.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    (listView.adapter as ArrayAdapter<*>).filter.filter(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {

                }
            })
        }

    }

    private fun handleNoNetworkInfo() {
        val networkAccessLiveData = NetworkAccessLiveData(this)
        networkAccessLiveData.observe(this) { hasInternetConnection ->
            this.hasInternetConnection = hasInternetConnection
            changeNoInternetConnectionInfoVisibility(hasInternetConnection)
        }
    }

    private fun initViewModel() {
        val factory = CryptocurrencyPricesViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory).get(CryptocurrencyPricesViewModel::class.java)
    }

    private fun assignViewsVariables() {
        tvSelectedCurrencyId = findViewById(R.id.tvSelectedCurrencyId)
        etDate = findViewById(R.id.etDate)
        btnGet = findViewById(R.id.btnGet)
        btnPrevRecord = findViewById(R.id.btnPrevRecord)
        btnNextRecord = findViewById(R.id.btnNextRecord)

        tvCurrentAndMaxIndex = findViewById(R.id.tvCurrentAndMaxIndex)

        tvCryptocurrencySymbol = findViewById(R.id.tvCryptocurrencySymbol)
        tvCryptocurrencyDate = findViewById(R.id.tvCryptocurrencyDate)
        tvCryptocurrencyPrice = findViewById(R.id.tvCryptocurrencyPrice)
        tvNoInternetConnection = findViewById(R.id.tvNoInternetConnection)
        chart = findViewById(R.id.chart)
        llChartWithOptions = findViewById(R.id.llChartWithOptions)
        progressBarChartLoading = findViewById(R.id.progressBarChartLoading)
        chartRadioGroupTimeRange = findViewById(R.id.chartRadioGroupTimeRange)
    }

    private fun initializeChart() {
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setTouchEnabled(false)
        chart.setDrawBorders(false)

        setChartDescription()
        chart.description.textColor =
            ContextCompat.getColor(applicationContext, R.color.chart_font_color)
        chart.description.textSize += 4 //increase default text size
        chart.description.yOffset -= 15  //offset description to bottom direction

        chart.legend.isEnabled = false

        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawLabels(false)
        chart.xAxis.setDrawAxisLine(false)

        chart.axisLeft.setDrawAxisLine(false)
        Log.v("myApp", chart.axisLeft.textSize.toString())
        chart.axisLeft.textSize = 15f //increase default text size
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.textColor =
            ContextCompat.getColor(applicationContext, R.color.chart_font_color)
        chart.axisLeft.setLabelCount(6, true)

        chart.axisRight.setDrawAxisLine(false)
        chart.axisRight.isEnabled = false

        chart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val digitsNumber = 6
                val valueConverted: String = String.format("%.5f", value)
                var stringToReturn = ""

                if (valueConverted.isNotEmpty() && valueConverted.isNotBlank()) {
                    if (valueConverted.length >= digitsNumber) {
                        stringToReturn = valueConverted.substring(0, digitsNumber)
                        if(value>=10000)
                            stringToReturn = stringToReturn.replace(".", "")
                    } else
                        stringToReturn =
                            valueConverted.substring(0, valueConverted.length - 1)
                }
                return stringToReturn
            }
        }

        setChartVisibility(false)
        setChartLoadingProgressBarVisibility(false)
    }

    private fun setChartData(values: ArrayList<Entry>) {
        val set1 = LineDataSet(values, "")

        set1.color = Color.BLUE
        set1.setDrawCircles(false)
        set1.setDrawHorizontalHighlightIndicator(false)
        set1.setDrawVerticalHighlightIndicator(false)
        set1.lineWidth = 3f
        set1.setDrawValues(false)



        if (chart.data == null) {
            val data = LineData(set1)
            chart.data = data
        } else {
            chart.clearValues()
            chart.data.clearValues()
            chart.data.addDataSet(set1)
        }
        //chart.animateX(1000)
        chart.notifyDataSetChanged()
        chart.invalidate()

        setChartVisibility(true)
        setChartLoadingProgressBarVisibility(false)
    }

    private fun setChartDescription() {
        chart.description.text = when (chartRadioGroupTimeRange.checkedRadioButtonId) {
            R.id.chartRadioButtonTimeRangeOneYear -> "One year"
            R.id.chartRadioButtonTimeRangeOneMonth -> "One month"
            R.id.chartRadioButtonTimeRangeOneWeek -> "One week"
            R.id.chartRadioButtonTimeRange24Hours -> "24 hours"
            else -> "Unknown time period"
        }
    }

    private fun initializeDatePicker() {
        etDate.setText(defaultDateFormatter.format(Date()))

        datePickerDialog =
            DatePickerDialog(
                this, DatePickerDialog.OnDateSetListener { datePicker, year, monthOfYear, day ->
                    val month = monthOfYear + 1
                    etDate.setText("${day}.${month}.${year}")
                }, currentSelectedDate.get(Calendar.YEAR),
                currentSelectedDate.get(Calendar.MONTH),
                currentSelectedDate.get(Calendar.DAY_OF_MONTH)
            )

        etDate.setOnClickListener(View.OnClickListener { openDatePicker() })
    }

    private fun openDatePicker() {
        datePickerDialog?.show()
    }

    private fun switchVisibilityOfRecordViewer(visible: Int) {
        val llRecords: LinearLayout = findViewById(R.id.llRecords)
        llRecords.visibility = visible
    }

    private fun navigateToLastInsertedRecord() {
        try {
            if (viewModel.lastAddedObject != null) {
                var foundIndex = -1
                listOfRecords?.forEachIndexed { index, it -> //skip comparing ID
                    if (it.cryptocurrencyId == viewModel.lastAddedObject!!.cryptocurrencyId
                        && it.date == viewModel.lastAddedObject!!.date
                        && it.priceUsd == viewModel.lastAddedObject!!.priceUsd
                    )
                        foundIndex = index
                }
                if (foundIndex != -1) currentRecordIndex = foundIndex
                displayRecordByIndex(currentRecordIndex)
            } else
                displayRecordByIndex(currentRecordIndex)
        } catch (exc: Exception) {

        }
    }

    private fun updateIndexInfo() {
        tvCurrentAndMaxIndex.text = "${currentRecordIndex + 1}/${maxRecordIndex + 1}"
    }

    private fun displayRecordByIndex(index: Int) {
        try {
            val entity: CryptocurrencyPricesEntityDb? = listOfRecords?.get(currentRecordIndex)

            if (entity != null) {
                tvCryptocurrencySymbol.text = entity.cryptocurrencyId
                tvCryptocurrencyDate.text = defaultDateFormatter.format(entity.date)
                tvCryptocurrencyPrice.text =
                    "%.3f USD".format(entity.priceUsd)
            }

            requestPriceHistoryForDateRange()
        } catch (exc: Exception) {
            Log.e("myApp", exc.toString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.itemClearRecords -> {
                viewModel.clearAllRecords()
                currentRecordIndex = 0
                maxRecordIndex = 0
                updateIndexInfo()
                setChartVisibility(false)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun changeNoInternetConnectionInfoVisibility(hasInternetConnection: Boolean?) {
        if (hasInternetConnection == true)
            tvNoInternetConnection.visibility = View.GONE
        else
            tvNoInternetConnection.visibility = View.VISIBLE
    }

    private fun setChartVisibility(visible: Boolean) {
        if (visible)
            llChartWithOptions.postDelayed(Runnable { //show with delay
                llChartWithOptions.visibility = View.VISIBLE
            }, 200)
        else
            llChartWithOptions.visibility = View.GONE
    }

    private fun setChartLoadingProgressBarVisibility(visible: Boolean) {
        if (visible)
            progressBarChartLoading.visibility = View.VISIBLE
        else {
            progressBarChartLoading.postDelayed(Runnable { //hide with delay
                progressBarChartLoading.visibility = View.GONE
            }, 200)

        }
    }

}

