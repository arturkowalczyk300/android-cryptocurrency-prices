package com.arturkowalczyk300.cryptocurrencyprices.View

import android.os.Bundle
import android.util.Log
import android.view.Menu
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
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.EntityCryptocurrenciesHistoricalPrices
import com.arturkowalczyk300.cryptocurrencyprices.NetworkAccessLiveData
import com.arturkowalczyk300.cryptocurrencyprices.Other.DateFormatterUtil
import com.arturkowalczyk300.cryptocurrencyprices.Other.Prefs.SharedPreferencesHelper
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModel
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModelFactory
import java.sql.Timestamp
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: CryptocurrencyPricesViewModel
    private lateinit var tvSelectedCurrencyId: TextView
    private lateinit var etDate: EditText
    private lateinit var tvCryptocurrencySymbol: TextView
    private lateinit var tvCryptocurrencyDate: TextView
    private lateinit var tvCryptocurrencyPrice: TextView
    private lateinit var tvNoInternetConnection: TextView
    private lateinit var rgDateActualArchivalSelection: RadioGroup
    private var chartFragment: ChartFragment? = null
    private lateinit var sharedPrefsInstance: SharedPreferencesHelper
    private var savedInstanceStateBundle: Bundle? = null
    private var autoFetchDataAlreadyDone = false
    private var autoFetchDataPending = false

    private var isCurrenciesListInitialized: Boolean = false


    private var datePicker = CustomDatePickerHandler()

    private var listOfCryptocurrenciesNames: ArrayList<String> = ArrayList()

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (chartFragment != null && ev != null && ev.action == ACTION_UP) {
            val chartRectangle = chartFragment!!.getGlobalVisibleRectOfChart()

            if (!chartRectangle.contains(ev!!.rawX.toInt(), ev!!.rawY.toInt()))
                chartFragment!!.hideMarker()
        }

        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DateFormatterUtil.customDateOnlyFormat = getString(R.string.defaultDateFormat)

        savedInstanceStateBundle = savedInstanceState
        assignViewsVariables()
        initViewModel()
        handleNoNetworkInfo()

        handleCryptocurrencyChoice()
        initializeDatePicker()
        addButtonsOnClickListeners()
        observeLiveData()

        sharedPrefsInstance = SharedPreferencesHelper(applicationContext)
    }

    private fun initChartFragment() {
        chartFragment = ChartFragment()
        if (savedInstanceStateBundle == null) //prevent recreation of fragment when it already exists
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.flChart, chartFragment!!)
                commit()
            }
    }

    private fun requestUpdateDataFromNetwork() {
        if (viewModel.hasInternetConnection)
            viewModel.updateCryptocurrenciesList()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun assignViewsVariables() {
        tvSelectedCurrencyId = findViewById(R.id.tvSelectedCurrencyId)
        etDate = findViewById(R.id.etDate)
        tvCryptocurrencySymbol = findViewById(R.id.tvCryptocurrencySymbol)
        tvCryptocurrencyDate = findViewById(R.id.tvCryptocurrencyDate)
        tvCryptocurrencyPrice = findViewById(R.id.tvCryptocurrencyPrice)
        tvNoInternetConnection = findViewById(R.id.tvNoInternetConnection)

        rgDateActualArchivalSelection = findViewById(R.id.radioGroupDate)
    }

    private fun initViewModel() {
        val factory = CryptocurrencyPricesViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory).get(CryptocurrencyPricesViewModel::class.java)

        viewModel.vsCurrency = getString(R.string.defaultVsCurrency)
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

            updateDataIfConnectedToInternet()
        }
    }

    private fun updateDataIfConnectedToInternet() {
        if (isCurrenciesListInitialized && viewModel.hasInternetConnection) {
            autoFetchDataAlreadyDone = true
            autoFetchDataPending = false
            try {
                viewModel.updatePriceData()
            } catch (exc: Exception) {
                Log.e("myApp", "addButtonsOnClickListeners, $exc")
            }
        } else {
            if (!autoFetchDataAlreadyDone) autoFetchDataPending = true
        }
    }

    private fun observeLiveData() {
        //show actual price

        updateCurrentPriceSection()


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

    private fun updateCurrentPriceSection() {
        viewModel.getAllHistoricalPrices().observe(this, Observer { allPricesList ->
            if (allPricesList == null)
                switchVisibilityOfCurrentPriceSection(View.INVISIBLE) //to still take layout space


            allPricesList?.let {
                if (allPricesList.isNotEmpty()) {

                    val currentElement = allPricesList.sortedByDescending { it.timeRangeTo }
                        .find { it.cryptocurrencyId == viewModel.selectedCryptocurrencyId }
                    currentElement?.let {
                        val actualPrice =
                            currentElement!!.prices.list.first().value

                        if (currentElement!!.cryptocurrencyId == viewModel.selectedCryptocurrencyId) {
                            switchVisibilityOfCurrentPriceSection(View.VISIBLE)
                            updateTextViews(
                                currentElement!!.cryptocurrencyId,
                                currentElement!!.timeRangeTo,
                                actualPrice.toFloat()
                            )
                        } else //no valid data
                            switchVisibilityOfCurrentPriceSection(View.INVISIBLE)
                    }
                    if (currentElement == null)
                        switchVisibilityOfCurrentPriceSection(View.INVISIBLE)
                } else
                    switchVisibilityOfCurrentPriceSection(View.INVISIBLE)

            }
        }
        )
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
                initChartFragment()
                updateDataIfConnectedToInternet()
            }

        })

        tvSelectedCurrencyId.setOnClickListener {
            val dialog = DialogListWithSearchTool()
            dialog.showDialog(this, listOfCryptocurrenciesNames)

            dialog.setListenerOnClickItem { cryptocurrencyId ->
                tvSelectedCurrencyId.text = cryptocurrencyId
                viewModel.selectedCryptocurrencyId = cryptocurrencyId
                sharedPrefsInstance.setLastChosenCryptocurrency(cryptocurrencyId)
                updateDataIfConnectedToInternet()

                updateCurrentPriceSection()

                chartFragment?.updateData()
            }
        }
    }

    private fun handleNoNetworkInfo() {
        val networkAccessLiveData = NetworkAccessLiveData(this)
        networkAccessLiveData.observe(this) { hasInternetConnection ->
            viewModel.hasInternetConnection = hasInternetConnection
            changeNoInternetConnectionInfoVisibility(hasInternetConnection)
            if (viewModel.hasInternetConnection) {
                requestUpdateDataFromNetwork()
                updateDataIfConnectedToInternet()
                chartFragment?.updateData()
            }
        }

        viewModel.currentlyDisplayedDataUpdatedMinutesAgo.observe(this) { minutes ->
            val tv: TextView = findViewById(R.id.textViewLastUpdate)

            if (!viewModel.hasInternetConnection && minutes != null) {
                tv.text = getString(R.string.lastUpdate, minutes)
                tv.visibility = View.VISIBLE
            } else
                tv.visibility = View.GONE
        }
    }

    private fun changeNoInternetConnectionInfoVisibility(hasInternetConnection: Boolean?) {
        if (hasInternetConnection == true)
            tvNoInternetConnection.visibility = View.GONE
        else
            tvNoInternetConnection.visibility = View.VISIBLE
    }

    private fun switchVisibilityOfCurrentPriceSection(visible: Int) {
        val groupRecords: androidx.constraintlayout.widget.Group = findViewById(R.id.groupRecords)
        groupRecords.visibility = visible
    }

    private fun openDatePicker() {
        datePicker.show()
    }

    private fun updateTextViews(currencySymbol: String, dateUnixTime: Long, price: Float) {
        tvCryptocurrencySymbol.text = currencySymbol
        tvCryptocurrencyDate.text = DateFormatterUtil.formatDateOnly(Timestamp(dateUnixTime))
        tvCryptocurrencyPrice.text =
            "%.3f %s".format(
                price,
                getString(R.string.defaultVsCurrency)
            )
    }
}

