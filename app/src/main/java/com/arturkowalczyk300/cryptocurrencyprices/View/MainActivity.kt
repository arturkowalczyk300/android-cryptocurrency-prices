package com.arturkowalczyk300.cryptocurrencyprices.View

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesEntityDb
import com.arturkowalczyk300.cryptocurrencyprices.NetworkAccessLiveData
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModel
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: CryptocurrencyPricesViewModel
    private lateinit var spinCurrencyId: Spinner
    private lateinit var etDate: EditText
    private lateinit var btnGet: Button
    private lateinit var btnPrevRecord: Button
    private lateinit var btnNextRecord: Button
    private lateinit var tvCurrentAndMaxIndex: TextView
    private lateinit var tvCryptocurrencySymbol: TextView
    private lateinit var tvCryptocurrencyDate: TextView
    private lateinit var tvCryptocurrencyPrice: TextView
    private lateinit var tvNoInternetConnection: TextView

    private var isSpinnerInitialized: Boolean = false
    private var hasInternetConnection: Boolean = false
    private var datePickerDialog: DatePickerDialog? = null

    private var currentRecordIndex: Int = 0
    private var maxRecordIndex: Int = 0

    private var listOfRecords: List<CryptocurrencyPricesEntityDb>? = null
    private val currentSelectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        assignViewsVariables()
        initViewModel()
        handleNoNetworkInfo()
        handleCryptocurrencyChoice()
        initializeDatePicker()
        addButtonsOnClickListeners()
        observeLiveData()
        updateIndexInfo()
    }

    private fun addButtonsOnClickListeners() {
        btnGet.setOnClickListener {
            if (isSpinnerInitialized && hasInternetConnection) {
                var date: Date
                var sdf = SimpleDateFormat(getString(R.string.defaultDateFormat))
                try {
                    date = sdf.parse(etDate.text.toString())
                    viewModel.requestPriceData(
                        spinCurrencyId.selectedItem.toString(),
                        date
                    )
                } catch (exc: Exception) {
                    Log.v("myApp", exc.toString())
                }
            }
        }

        btnPrevRecord.setOnClickListener {

            currentRecordIndex = (currentRecordIndex - 1)
            if (currentRecordIndex < 0)
                currentRecordIndex = maxRecordIndex
            updateIndexInfo()
            displayRecordByIndex(currentRecordIndex)
        }

        btnNextRecord.setOnClickListener {
            currentRecordIndex = (currentRecordIndex + 1)
            if (currentRecordIndex > maxRecordIndex)
                currentRecordIndex = 0
            updateIndexInfo()
            displayRecordByIndex(currentRecordIndex)
        }
    }

    private fun observeLiveData() {
        viewModel.getAllReadings()?.observe(this, Observer {
            listOfRecords = it
            maxRecordIndex = it.size - 1
            updateIndexInfo()
            if (it.size > 0) {
                navigateToLastInsertedRecord()
                updateIndexInfo()
                switchVisibilityOfRecordViewer(View.VISIBLE)
            } else
                switchVisibilityOfRecordViewer(View.GONE)
        }
        )
    }

    private fun handleCryptocurrencyChoice() {
        viewModel.requestCryptocurrenciesList().observe(this, Observer { it ->
            var listOfCryptocurrenciesNames: ArrayList<String> = ArrayList()

            it.forEach { nextIt ->
                listOfCryptocurrenciesNames.add(nextIt.id)
            }

            ArrayAdapter(this, R.layout.my_spinner_item, listOfCryptocurrenciesNames)
                .also { adapter ->
                    adapter.setDropDownViewResource(R.layout.my_spinner_item)
                    spinCurrencyId.adapter = adapter
                }

            isSpinnerInitialized = true
        })
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
        spinCurrencyId = findViewById(R.id.spinCurrencyId)
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
        val year: Int = 0
        val month: Int = 0
        val day: Int = 0

        val sdf = SimpleDateFormat(getString(R.string.defaultDateFormat))
        etDate.setText(sdf.format(Date()))

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
                val sdf = SimpleDateFormat(getString(R.string.defaultDateFormat))

                tvCryptocurrencySymbol.text = entity.cryptocurrencyId
                tvCryptocurrencyDate.text = sdf.format(entity.date)
                tvCryptocurrencyPrice.text =
                    "%.3f USD".format(entity.priceUsd)
            }
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

