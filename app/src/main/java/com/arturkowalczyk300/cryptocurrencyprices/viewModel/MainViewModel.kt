package com.arturkowalczyk300.cryptocurrencyprices.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturkowalczyk300.cryptocurrencyprices.model.Repository
import com.arturkowalczyk300.cryptocurrencyprices.model.room.InfoWithinTimeRangeEntity
import com.github.mikephil.charting.utils.Utils.init
import kotlinx.coroutines.*
import java.util.*

class MainViewModel(application: Application) : ViewModel() {
    private var repository: Repository =
        Repository(application)
    val apiUnwrappingPriceDataErrorLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    //livedata properties
    private var _allCryptocurrencies = repository.getAllCryptocurrencies()
    val allCryptocurrencies = _allCryptocurrencies

    private var _allCryptocurrenciesPrices = repository.getAllCryptocurrenciesPrices()
    val allCryptocurrenciesPrices = _allCryptocurrenciesPrices

    private var _cryptocurrencyChartData = repository.getCryptocurrencyChartData()
    val cryptocurrencyChartData: LiveData<InfoWithinTimeRangeEntity?>? =
        _cryptocurrencyChartData

    //statuses of operations
    private var _apiErrorCode = repository.getApiErrorCodeLiveData()
    val apiErrorCode = _apiErrorCode

    private var _currentlyDisplayedDataUpdatedMinutesAgo: MutableLiveData<Long?> =
        MutableLiveData<Long?>()
    val currentlyDisplayedDataUpdatedMinutesAgo: LiveData<Long?> =
        _currentlyDisplayedDataUpdatedMinutesAgo

    //TODO: handle filling of flags below

    private var _isCurrenciesListLoadedFromCache = MutableLiveData<Boolean>(false)
    val isCurrenciesListLoadedFromCache: LiveData<Boolean> = _isCurrenciesListLoadedFromCache

    private var _isCurrencyPriceDataLoadedFromCache = MutableLiveData<Boolean>(false)
    val isCurrencyPriceDataLoadedFromCache: LiveData<Boolean> = _isCurrencyPriceDataLoadedFromCache

    private var _isCurrencyChartDataLoadedFromCache = MutableLiveData<Boolean>(false)
    val isCurrencyChartDataLoadedFromCache: LiveData<Boolean> = _isCurrencyChartDataLoadedFromCache

    private var _isCurrenciesListUpdated = MutableLiveData<Boolean>(false)
    val isCurrenciesListUpdated: LiveData<Boolean> = _isCurrenciesListUpdated

    private var _isCurrencyPriceDataUpdated = MutableLiveData<Boolean>(false)
    val isCurrencyPriceDataUpdated: LiveData<Boolean> = _isCurrencyPriceDataUpdated

    private var _isCurrencyChartDataUpdated = MutableLiveData<Boolean>(false)
    val isCurrencyChartDataUpdated: LiveData<Boolean> = _isCurrencyChartDataUpdated

    private var _isUpdateOfCurrenciesListInProgress = MutableLiveData(false)
    val isUpdateOfCurrenciesListInProgress: LiveData<Boolean> = _isUpdateOfCurrenciesListInProgress

    private var _isUpdateOfPriceDataInProgress = MutableLiveData(false)
    val isUpdateOfPriceDataInProgress: LiveData<Boolean> = _isUpdateOfPriceDataInProgress

    private var _isUpdateOfChartDataInProgress = MutableLiveData(false)
    val isUpdateOfChartDataInProgress: LiveData<Boolean> = _isUpdateOfChartDataInProgress

    //parameters for data fetching
    var selectedCryptocurrencyId: String? = null
    var showArchivalData = false
    var showArchivalDataRange: Date? = null
    var vsCurrency: String? = null
    private var selectedUnixTimeFrom: Long? = null
    private var selectedUnixTimeTo: Long? = null
    var selectedDaysToSeeOnChart: Int? = 1
    var isChartFragmentInitialized = false

    //info
    var hasInternetConnection: Boolean = false
    var noDataCachedVisibility: Boolean = false

    //coroutines
    private val loadCryptocurrenciesListCoroutine = viewModelScope.async {
        _allCryptocurrencies.observeForever { currenciesList ->
            if (currenciesList.isNotEmpty())
                _isCurrenciesListLoadedFromCache.value = true

            if (!loadLastPriceCoroutine.isActive) {
                viewModelScope.launch {
                    loadLastPriceCoroutine.start()
                }
            }

            if (!loadChartDataCoroutine.isActive) {
                viewModelScope.launch {
                    loadChartDataCoroutine.start()
                }
            }
        }
        requestUpdateCryptocurrenciesList()
    }

    private val loadLastPriceCoroutine = viewModelScope.async(start = CoroutineStart.LAZY) {
        _allCryptocurrenciesPrices.observeForever {
            //TODO: distinct between cache and network data!
            if (!it.isNullOrEmpty())
                _isCurrencyPriceDataLoadedFromCache.value = true
        }
        withContext(Dispatchers.Default) { //update loop
            while (selectedCryptocurrencyId == null) { //wait until conditions are fulfilled
            }

            requestUpdateSelectedCryptocurrencyPriceData() //TODO: create auto-update
        }
    }

    private val loadChartDataCoroutine = viewModelScope.async(start = CoroutineStart.LAZY) {
        withContext(Dispatchers.Main) {
            _cryptocurrencyChartData.observeForever {
                //TODO: distinct between cache and network data
                _isCurrencyChartDataLoadedFromCache.value = true
            }
        }
        withContext(Dispatchers.Default) { //update loop
            while (selectedCryptocurrencyId == null || selectedDaysToSeeOnChart == null) //wait until conditions are fulfilled
            {

            }
            recalculateTimeRange()
            withContext(Dispatchers.Main) {
                getChartLiveData(
                    selectedCryptocurrencyId!!,
                    selectedDaysToSeeOnChart!!
                )
            }

            while (selectedCryptocurrencyId == null || selectedDaysToSeeOnChart == null
                || selectedUnixTimeFrom == null || selectedUnixTimeTo == null
                || !hasInternetConnection || vsCurrency == null
            ) { //wait until conditions are fulfilled
            }
            requestUpdateCryptocurrencyChartData() //TODO: create auto-update
        }
    }

    init {
        loadCryptocurrenciesListCoroutine.start()
    }

    fun recalculateTimeRange() {
        val calendar = Calendar.getInstance()
        if (showArchivalData) { //TODO: check this
            calendar.time = showArchivalDataRange
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        val dateEnd = calendar.time

        when (selectedDaysToSeeOnChart) {
            1 -> calendar.add(Calendar.SECOND, -(60 * 60 * 24))
            7 -> calendar.add(Calendar.SECOND, -(60 * 60 * 24 * 7))
            31 -> calendar.add(Calendar.SECOND, -(60 * 60 * 24 * 31))
            365 -> calendar.add(Calendar.SECOND, -(60 * 60 * 24 * 365))
        }
        val dateStart = calendar.time

        selectedUnixTimeFrom = (dateStart.time / 1000)
        selectedUnixTimeTo = (dateEnd.time / 1000)
    }

    private fun requestUpdateSelectedCryptocurrencyPriceData(
    ) {
        if (_isUpdateOfPriceDataInProgress.value == true) {
            Log.e("myApp", "Update in progress or already done")
            return
        }

        if (selectedCryptocurrencyId == null ||
            (showArchivalData && showArchivalDataRange == null)
        ) {
            Log.e(
                "myApp/requestUpdateSelectedCryptocurrencyPriceData",
                "No specified cryptocurrency, or not specified data range for archival reading"
            )
            return
        }

        if (!hasInternetConnection) {
            Log.d("myApp", "no internet connection!")
            return
        }

        viewModelScope.launch {
            var dateRequest: Date

            if (showArchivalData) {
                dateRequest = showArchivalDataRange!!

                repository.updatePriceData(
                    currencySymbol = selectedCryptocurrencyId!!,
                    vs_currency = vsCurrency!!,
                    archival = true,
                    archivalDate = dateRequest
                )
            } else {
                repository.updatePriceData(
                    currencySymbol = selectedCryptocurrencyId!!,
                    vs_currency = vsCurrency!!
                )

                Log.e(
                    "myApp/viewmodel",
                    "price update req, currency=${selectedCryptocurrencyId}"
                )
            }

        }
        //TODO(): add api errors handling
    }

    fun requestUpdateCryptocurrenciesList() {
        if (_isUpdateOfCurrenciesListInProgress.value == true) {
            Log.e("myApp", "Update in progress or already done")
            return
        }

        if (!hasInternetConnection) {
            Log.d("myApp", "no internet connection!")
            return
        }

        viewModelScope.launch {
            repository.updateCryptocurrenciesList()
        }
    }

    private fun requestUpdateCryptocurrencyChartData(
    ) {
        recalculateTimeRange()

        if (_isUpdateOfChartDataInProgress.value == true) {
            Log.e("myApp", "update in progress or already done")
            return
        }

        if (selectedCryptocurrencyId == null || vsCurrency == null
            || selectedUnixTimeFrom == null || selectedUnixTimeTo == null
            || selectedDaysToSeeOnChart == null
        ) //check if parameters are valid //TODO: add only getting data when there is no internet availabel
        {
            Log.e(
                "myApp/requestUpdateCryptocurrencyChartData",
                "One or more parameters are not set"
            )
            return
        }



        viewModelScope.launch {
            getChartLiveData(
                selectedCryptocurrencyId!!,
                selectedDaysToSeeOnChart!!
            )

            if(hasInternetConnection) {
                repository.updateCryptocurrenciesInfoWithinDateRange(
                    selectedCryptocurrencyId!!,
                    vsCurrency!!,
                    selectedUnixTimeFrom!!,
                    selectedUnixTimeTo!!
                )


                Log.e("myApp/viewmodel", "chart update req, currency=${selectedCryptocurrencyId}")
            }
            else{
                Log.d("myApp", "no internet connection, skip data update!")
            }
        }
    }


    private fun getChartLiveData(
        cryptocurrencyId: String,
        daysCount: Int,
    ) {
        repository.getCryptocurrencyChartData(
            cryptocurrencyId,
            daysCount
        )
    }

    fun requestUpdateAllData() {
        Log.d("myApp", "requestUpdateAllData")

        requestUpdateCryptocurrencyChartData()
        requestUpdateSelectedCryptocurrencyPriceData()

        invalidateChartDataData()
    }

    private fun invalidateChartDataData() {
        _isCurrencyChartDataLoadedFromCache.value = false
    }

    fun setCurrentlyDisplayedDataUpdatedMinutesAgo(value: Long?) {
        _currentlyDisplayedDataUpdatedMinutesAgo.postValue(value)
    }
}