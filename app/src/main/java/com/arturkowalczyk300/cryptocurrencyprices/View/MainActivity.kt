package com.arturkowalczyk300.cryptocurrencyprices.View

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arturkowalczyk300.cryptocurrencyprices.Model.REQUEST_CRYPTOCURRENCIES_LIST_FAILURE
import com.arturkowalczyk300.cryptocurrencyprices.Model.REQUEST_PRICE_DATA_FAILURE
import com.arturkowalczyk300.cryptocurrencyprices.Model.REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesEntityDb
import com.arturkowalczyk300.cryptocurrencyprices.NetworkAccessLiveData
import com.arturkowalczyk300.cryptocurrencyprices.Other.DateFormatterUtil
import com.arturkowalczyk300.cryptocurrencyprices.Other.Prefs.SharedPreferencesHelper
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModel
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModelFactory
import java.util.*
import kotlin.collections.ArrayList

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
    private lateinit var flChart: FrameLayout
    private lateinit var chartFragment: ChartFragment
    private lateinit var sharedPrefsInstance: SharedPreferencesHelper
    private var autoFetchDataAlreadyDone = false
    private var autoFetchDataPending = false

    private var isCurrenciesListInitialized: Boolean = false
    private var hasInternetConnection: Boolean = false

    private var datePickerDialog: DatePickerDialog? = null
    private var currentRecordIndex: Int = 0

    private var maxRecordIndex: Int = 0
    private var listOfRecords: List<CryptocurrencyPricesEntityDb>? = null
    private val currentDate: Calendar = Calendar.getInstance()

    private val currentSelectedDate: Calendar = Calendar.getInstance()
    private var listOfCryptocurrenciesNames: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DateFormatterUtil.customDateFormat = getString(R.string.defaultDateFormat)

        assignViewsVariables()
        initViewModel()
        handleNoNetworkInfo()
        handleCryptocurrencyChoice()
        initializeDatePicker()
        addButtonsOnClickListeners()
        observeLiveData()
        updateIndexInfo()

        //chart section
        chartFragment = ChartFragment()
        if (savedInstanceState == null) //prevent recreation of fragment when it already exists
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.flChart, chartFragment)
                commit()
            }

        sharedPrefsInstance = SharedPreferencesHelper(applicationContext)


    }

    private fun addButtonsOnClickListeners() {
        btnGet.setOnClickListener {
            if (isCurrenciesListInitialized && hasInternetConnection) {
                autoFetchDataAlreadyDone = true
                autoFetchDataPending = false
                var date: Date
                try {
                    date = DateFormatterUtil.parse(etDate.text.toString())
                    viewModel.requestPriceData(
                        tvSelectedCurrencyId.text.toString(),
                        date
                    )
                } catch (exc: Exception) {
                    Log.e("myApp", exc.toString())
                }
            } else {
                if (!autoFetchDataAlreadyDone) autoFetchDataPending = true
            }

            chartFragment.setChartVisibility(false)
            chartFragment.setChartLoadingProgressBarVisibility(true)
        }

        btnPrevRecord.setOnClickListener {

            currentRecordIndex = (currentRecordIndex - 1)
            if (currentRecordIndex < 0)
                currentRecordIndex = maxRecordIndex
            updateIndexInfo()
            chartFragment.setChartVisibility(false)
            chartFragment.setChartLoadingProgressBarVisibility(true)
            displayRecordByIndex(currentRecordIndex)
        }

        btnNextRecord.setOnClickListener {
            currentRecordIndex = (currentRecordIndex + 1)
            if (currentRecordIndex > maxRecordIndex)
                currentRecordIndex = 0
            updateIndexInfo()
            chartFragment.setChartVisibility(false)
            chartFragment.setChartLoadingProgressBarVisibility(true)
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

        viewModel.getApiErrorCodeLiveData().observe(this, object : Observer<Pair<Boolean, Int>> {
            override fun onChanged(t: Pair<Boolean, Int>?) {

                if (t!!.first) { //error occured
                    when (t.second) {
                        REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE ->
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE),
                                Toast.LENGTH_LONG
                            ).show()
                        REQUEST_CRYPTOCURRENCIES_LIST_FAILURE ->
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.REQUEST_CRYPTOCURRENCIES_LIST_FAILURE),
                                Toast.LENGTH_LONG
                            ).show()
                        REQUEST_PRICE_DATA_FAILURE ->
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.REQUEST_PRICE_DATA_FAILURE),
                                Toast.LENGTH_LONG
                            ).show()
                        else ->
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.UNKNOWN_FAILURE),
                                Toast.LENGTH_LONG
                            ).show()
                    }
                }
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

            if (sharedPrefsInstance.getLastChosenCryptocurrency() != null)
                tvSelectedCurrencyId.text = sharedPrefsInstance.getLastChosenCryptocurrency()
            else tvSelectedCurrencyId.text = listOfCryptocurrenciesNames.first()

            if (!autoFetchDataAlreadyDone) {
                autoFetchDataAlreadyDone = true
                btnGet.performClick()
            }

        })

        tvSelectedCurrencyId.setOnClickListener {
            val dialog = DialogListWithSearchTool()
            dialog.showDialog(this, listOfCryptocurrenciesNames)

            dialog.setListenerOnClickItem { cryptocurrencyId ->
                tvSelectedCurrencyId.text = cryptocurrencyId
                sharedPrefsInstance.setLastChosenCryptocurrency(cryptocurrencyId)
            }
        }

    }

    private fun handleNoNetworkInfo() {
        val networkAccessLiveData = NetworkAccessLiveData(this)
        networkAccessLiveData.observe(this) { hasInternetConnection ->
            this.hasInternetConnection = hasInternetConnection
            changeNoInternetConnectionInfoVisibility(hasInternetConnection)
            if (!autoFetchDataAlreadyDone && autoFetchDataPending && hasInternetConnection)
                btnGet.performClick()
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
    }


    private fun initializeDatePicker() {
        etDate.setText(DateFormatterUtil.format(Date()))

        val customDatePickerDialog = object : DatePickerDialog(
            this, DatePickerDialog.OnDateSetListener { datePicker, year, monthOfYear, day ->
                val month = monthOfYear + 1
                etDate.setText("${day}.${month}.${year}")
            }, currentSelectedDate.get(Calendar.YEAR),
            currentSelectedDate.get(Calendar.MONTH),
            currentSelectedDate.get(Calendar.DAY_OF_MONTH)
        ) {
            init {
                datePicker.maxDate = System.currentTimeMillis() //forbid choosing future date
            }

            var flagSkipDismiss: Boolean = false

            override fun dismiss() {
                if (flagSkipDismiss)
                    flagSkipDismiss = false
                else
                    super.dismiss()
            }
        }

        //add button Today
        customDatePickerDialog.setButton(
            DialogInterface.BUTTON_NEUTRAL,
            getString(R.string.btnToday),
            DialogInterface.OnClickListener { dialog, which ->
                customDatePickerDialog.updateDate(
                    currentDate.get(Calendar.YEAR),
                    currentDate.get(Calendar.MONTH),
                    currentDate.get(Calendar.DAY_OF_MONTH)
                )
                customDatePickerDialog.flagSkipDismiss =
                    true //clicking "Today" button won't close date picker
            })

        datePickerDialog =
            customDatePickerDialog //assign just to allow app to open date picker later

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
                tvCryptocurrencyDate.text = DateFormatterUtil.format(entity.date)
                tvCryptocurrencyPrice.text =
                    "%.3f USD".format(entity.priceUsd)
            }

            chartFragment.requestPriceHistory()
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
                chartFragment.setChartVisibility(false)
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

}

