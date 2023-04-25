package com.arturkowalczyk300.cryptocurrencyprices.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.arturkowalczyk300.cryptocurrencyprices.model.Repository
import com.arturkowalczyk300.cryptocurrencyprices.model.room.CryptocurrencyEntity
import com.arturkowalczyk300.cryptocurrencyprices.model.room.InfoWithinTimeRangeEntity
import com.arturkowalczyk300.cryptocurrencyprices.model.room.PriceEntity
import com.github.mikephil.charting.utils.Utils.init
import kotlinx.coroutines.*
import java.util.*

private const val REFRESH_PRICE_INTERVAL_MILLIS = 30000L
private const val REFRESH_CHART_INTERVAL_MILLIS = 60000L

class MainViewModel(application: Application) : ViewModel() {

    private var repository: Repository =
        Repository(application)
    val apiUnwrappingPriceDataErrorLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    //livedata properties
    private var _allCryptocurrencies = repository.getAllCryptocurrencies()
    val allCryptocurrencies = _allCryptocurrencies

    private var _allCryptocurrenciesPrices = MutableLiveData<List<PriceEntity>>()
    val allCryptocurrenciesPrices =
        Transformations.switchMap(repository.getAllCryptocurrenciesPrices()) {
            _allCryptocurrenciesPrices.value = it
            _allCryptocurrenciesPrices
        }

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

    private var _isCurrenciesListLoadedFromCache = MutableLiveData<Boolean>(false)
    val isCurrenciesListLoadedFromCache: LiveData<Boolean> = _isCurrenciesListLoadedFromCache

    private var _isCurrencyPriceDataLoadedFromCache = MutableLiveData<Boolean>(false)
    val isCurrencyPriceDataLoadedFromCache: LiveData<Boolean> = _isCurrencyPriceDataLoadedFromCache

    private var _isCurrencyChartDataLoadedFromCache = MutableLiveData<Boolean>(false)
    val isCurrencyChartDataLoadedFromCache: LiveData<Boolean> = _isCurrencyChartDataLoadedFromCache

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


            if (!it.isNullOrEmpty())
                _isCurrencyPriceDataLoadedFromCache.value = true
        }

        withContext(Dispatchers.Default) { //update loop
            while (true) {
                while (selectedCryptocurrencyId == null) { //wait until conditions are fulfilled
                }
                withContext(Dispatchers.Main) {
                    requestUpdateSelectedCryptocurrencyPriceData()
                }
                delay(REFRESH_PRICE_INTERVAL_MILLIS)
            }
        }
    }

    private val loadChartDataCoroutine = viewModelScope.async(start = CoroutineStart.LAZY) {
        withContext(Dispatchers.Main) {
            _cryptocurrencyChartData.observeForever {
                _isCurrencyChartDataLoadedFromCache.value = true
            }
        }
        withContext(Dispatchers.Default) { //update loop
            while (true) {
                while (selectedCryptocurrencyId == null || selectedDaysToSeeOnChart == null) { //wait until conditions are fulfilled
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
                requestUpdateCryptocurrencyChartData()
                delay(REFRESH_CHART_INTERVAL_MILLIS)
            }
        }
    }


    init {
        loadCryptocurrenciesListCoroutine.start()
    }

    fun recalculateTimeRange() {
        val calendar = Calendar.getInstance()
        if (showArchivalData) {
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
        refreshPriceData()

        if (_isUpdateOfPriceDataInProgress.value == true) {
            Log.d("myApp", "Update in progress or already done")
            return
        }

        if (selectedCryptocurrencyId == null ||
            (showArchivalData && showArchivalDataRange == null)
        ) {
            Log.d(
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
            }

        }
    }

    fun requestUpdateCryptocurrenciesList() {
        if (_isUpdateOfCurrenciesListInProgress.value == true) {
            Log.d("myApp", "Update in progress or already done")
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
            Log.d("myApp", "update in progress or already done")
            return
        }

        if (selectedCryptocurrencyId == null || vsCurrency == null
            || selectedUnixTimeFrom == null || selectedUnixTimeTo == null
            || selectedDaysToSeeOnChart == null
        ) {
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

            if (hasInternetConnection) {
                repository.updateCryptocurrenciesInfoWithinDateRange(
                    selectedCryptocurrencyId!!,
                    vsCurrency!!,
                    selectedUnixTimeFrom!!,
                    selectedUnixTimeTo!!
                )
            } else {
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

    private fun refreshPriceData() {
        _allCryptocurrenciesPrices.value = _allCryptocurrenciesPrices.value
    }

    override fun onCleared() {
        super.onCleared()
    }
}