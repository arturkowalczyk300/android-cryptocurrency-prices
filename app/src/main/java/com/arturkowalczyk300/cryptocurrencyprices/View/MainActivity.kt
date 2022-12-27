package com.arturkowalczyk300.cryptocurrencyprices.View

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arturkowalczyk300.cryptocurrencyprices.Model.REQUEST_CRYPTOCURRENCIES_LIST_FAILURE
import com.arturkowalczyk300.cryptocurrencyprices.Model.REQUEST_PRICE_DATA_FAILURE
import com.arturkowalczyk300.cryptocurrencyprices.Model.REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE
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
    private lateinit var tvCryptocurrencySymbol: TextView
    private lateinit var tvCryptocurrencyDate: TextView
    private lateinit var tvCryptocurrencyPrice: TextView
    private lateinit var tvNoInternetConnection: TextView
    private lateinit var rgDateActualArchivalSelection: RadioGroup
    private lateinit var chartFragment: ChartFragment
    private lateinit var sharedPrefsInstance: SharedPreferencesHelper
    private var autoFetchDataAlreadyDone = false
    private var autoFetchDataPending = false

    private var isCurrenciesListInitialized: Boolean = false
    private var hasInternetConnection: Boolean = false

    private var currentRecordIndex: Int = 0

    private var maxRecordIndex: Int = 0
    private var datePicker = CustomDatePickerHandler()

    private var listOfCryptocurrenciesNames: ArrayList<String> = ArrayList()

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && ev.action == ACTION_UP) {
            val chartRectangle = chartFragment.getGlobalVisibleRectOfChart()

            if (!chartRectangle.contains(ev!!.rawX.toInt(), ev!!.rawY.toInt()))
                chartFragment.hideMarker()
        }

        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DateFormatterUtil.customDateOnlyFormat = getString(R.string.defaultDateFormat)

        assignViewsVariables()
        initViewModel()
        handleNoNetworkInfo()
        requestUpdateDataFromNetwork()
        handleCryptocurrencyChoice()
        initializeDatePicker()
        addButtonsOnClickListeners()
        observeLiveData()

        //chart section
        chartFragment = ChartFragment()
        if (savedInstanceState == null) //prevent recreation of fragment when it already exists
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.flChart, chartFragment)
                commit()
            }

        sharedPrefsInstance = SharedPreferencesHelper(applicationContext)
    }

    private fun requestUpdateDataFromNetwork() {
        viewModel.updateCryptocurrenciesList()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun assignViewsVariables() {
        tvSelectedCurrencyId = findViewById(R.id.tvSelectedCurrencyId)
        etDate = findViewById(R.id.etDate)
        btnGet = findViewById(R.id.btnGet)
        tvCryptocurrencySymbol = findViewById(R.id.tvCryptocurrencySymbol)
        tvCryptocurrencyDate = findViewById(R.id.tvCryptocurrencyDate)
        tvCryptocurrencyPrice = findViewById(R.id.tvCryptocurrencyPrice)
        tvNoInternetConnection = findViewById(R.id.tvNoInternetConnection)

        rgDateActualArchivalSelection = findViewById(R.id.radioGroupDate)
    }

    private fun initViewModel() {
        val factory = CryptocurrencyPricesViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory).get(CryptocurrencyPricesViewModel::class.java)

        viewModel.vs_currency = getString(R.string.defaultVsCurrency)
    }

    private fun initializeDatePicker() {
        etDate.setText(DateFormatterUtil.formatDateOnly(Date()))
        etDate.setOnClickListener(View.OnClickListener { openDatePicker() })

        datePicker.initializeDatePicker(this)

        datePicker.setListenerOnDateChanged { dateString ->
            etDate.setText(dateString)
            viewModel.showArchivalDataRange =
                DateFormatterUtil.parseDateOnly(etDate.text.toString())
        }

        viewModel.showArchivalDataRange = DateFormatterUtil.parseDateOnly(etDate.text.toString())
    }

    //listeners

    private fun addButtonsOnClickListeners() {
        btnGet.setOnClickListener {
            if (isCurrenciesListInitialized && hasInternetConnection) {
                autoFetchDataAlreadyDone = true
                autoFetchDataPending = false
                var date: Date
                try {
                    date = DateFormatterUtil.parseDateOnly(etDate.text.toString())
                    viewModel.updatePriceData()
                } catch (exc: Exception) {
                    Log.e("myApp", exc.toString())
                }
            } else {
                if (!autoFetchDataAlreadyDone) autoFetchDataPending = true
            }

            chartFragment.setChartVisibility(false)
            chartFragment.setChartLoadingProgressBarVisibility(true)
        }

        rgDateActualArchivalSelection.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radioButtonDateActual -> {
                    etDate.visibility = View.GONE
                    viewModel.showArchivalData = false
                }
                R.id.radioButtonDateArchival -> {
                    etDate.visibility = View.VISIBLE
                    viewModel.showArchivalData = true
                }
            }

            btnGet.performClick()
        }
    }

    private fun observeLiveData() {
        viewModel.getAllHistoricalPrices().observe(this, Observer {
            maxRecordIndex = it.size - 1
            if (it.isNotEmpty()) {
                switchVisibilityOfRecordViewer(View.VISIBLE)
            } else
                switchVisibilityOfRecordViewer(View.GONE)
        }
        )

        viewModel.apiUnwrappingPriceDataErrorLiveData.observe(this, Observer { errorOccured ->
            if (errorOccured)
                Toast.makeText(
                    this,
                    getString(R.string.UNWRAP_PRICE_DATA_FAILURE),
                    Toast.LENGTH_LONG
                ).show()
        })

        viewModel.getApiErrorCodeLiveData().observe(this, object : Observer<Pair<Boolean, Int>> {
            override fun onChanged(t: Pair<Boolean, Int>?) {

                if (t!!.first) { //error occurred
                    var toastText = when (t.second) {
                        REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE ->
                            getString(R.string.REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE)
                        REQUEST_CRYPTOCURRENCIES_LIST_FAILURE ->
                            getString(R.string.REQUEST_CRYPTOCURRENCIES_LIST_FAILURE)
                        REQUEST_PRICE_DATA_FAILURE ->
                            getString(R.string.REQUEST_PRICE_DATA_FAILURE)
                        else ->
                            getString(R.string.UNKNOWN_FAILURE)
                    }

                    Toast.makeText(
                        applicationContext,
                        toastText,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun handleCryptocurrencyChoice() {
        viewModel.getAllCryptocurrencies().observe(this, Observer { it ->
            listOfCryptocurrenciesNames.clear()

            it.forEach { nextIt ->
                listOfCryptocurrenciesNames.add(nextIt.cryptocurrencyId)
            }

            isCurrenciesListInitialized = true

            if (sharedPrefsInstance.getLastChosenCryptocurrency() != null) {
                val curr = sharedPrefsInstance.getLastChosenCryptocurrency()
                tvSelectedCurrencyId.text = curr
                viewModel.selectedCryptocurrencyId = curr
            } else {
                val curr = listOfCryptocurrenciesNames.first()
                tvSelectedCurrencyId.text = curr
                viewModel.selectedCryptocurrencyId = curr
            }

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
                viewModel.selectedCryptocurrencyId = cryptocurrencyId
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.itemClearRecords -> {
                //TODO(): delete?
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //visibility switching

    private fun changeNoInternetConnectionInfoVisibility(hasInternetConnection: Boolean?) {
        if (hasInternetConnection == true)
            tvNoInternetConnection.visibility = View.GONE
        else
            tvNoInternetConnection.visibility = View.VISIBLE
    }

    private fun switchVisibilityOfRecordViewer(visible: Int) {
        val groupRecords: androidx.constraintlayout.widget.Group = findViewById(R.id.groupRecords)
        groupRecords.visibility = visible
    }

    private fun openDatePicker() {
        datePicker.show()
    }
}

