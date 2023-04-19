package com.arturkowalczyk300.cryptocurrencyprices.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturkowalczyk300.cryptocurrencyprices.model.Repository
import com.arturkowalczyk300.cryptocurrencyprices.model.room.InfoWithinTimeRangeEntity
import com.arturkowalczyk300.cryptocurrencyprices.model.room.PriceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val cryptocurrencyChartData: LiveData<InfoWithinTimeRangeEntity>? =
        _cryptocurrencyChartData

    //statuses of operations
    private var _apiErrorCode = repository.getApiErrorCodeLiveData()
    val apiErrorCode = _apiErrorCode

    private var _currentlyDisplayedDataUpdatedMinutesAgo: MutableLiveData<Long?> =
        MutableLiveData<Long?>()
    val currentlyDisplayedDataUpdatedMinutesAgo: LiveData<Long?> =
        _currentlyDisplayedDataUpdatedMinutesAgo

    //TODO: handle filling of flags below

    private var _isDataCached: MutableLiveData<Boolean> =
        MutableLiveData()
    val isDataCached: LiveData<Boolean> = _isDataCached //TODO: delete?

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
        Log.d("myApp/viewModel/coroutines/coroutineCryptocurrenciesList", "STARTED")
        _allCryptocurrencies.observeForever { currenciesList ->
            if (currenciesList.isNotEmpty())
                _isCurrenciesListLoadedFromCache.value = true

            if (!loadLastPriceCoroutine.isActive)
                loadLastPriceCoroutine.start()

            if (!loadChartDataCoroutine.isActive)
                loadLastPriceCoroutine.start()
        }
        requestUpdateCryptocurrenciesList()
            .also {
                Log.d(
                    "myApp/viewModel/coroutines/coroutineCryptocurrenciesList",
                    "REQ UPDATE"
                )
            }
    }

    private val loadLastPriceCoroutine = viewModelScope.async {
        Log.d("myApp/viewModel/coroutines/coroutineLastPrice ", "STARTED")
        _allCryptocurrenciesPrices.observeForever {
            //TODO: distinct between cache and network data!
            if (!it.isNullOrEmpty())
                _isCurrencyPriceDataLoadedFromCache.value = true
        }

        withContext(Dispatchers.Default) { //update loop

            while (selectedCryptocurrencyId == null) { //wait until conditions are fulfilled

            }

            requestUpdateSelectedCryptocurrencyPriceData()
                .also { Log.d("myApp/viewModel/coroutines/coroutineLastPrice", "REQ UPDATE") }


        }
    }

    private val loadChartDataCoroutine = viewModelScope.async {
        Log.d("myApp/viewModel/coroutines/coroutineChartData ", "STARTED")


        _cryptocurrencyChartData.observeForever {
            //TODO: distinct between cache and network data
            _isCurrencyChartDataLoadedFromCache.value = true
        }

        withContext(Dispatchers.Default) { //update loop
            recalculateTimeRange()
            while (selectedCryptocurrencyId == null || vsCurrency == null
                || selectedUnixTimeFrom == null || selectedUnixTimeTo == null
                || selectedDaysToSeeOnChart == null || !hasInternetConnection
            ) { //wait until conditions are fulfilled

            }
            requestUpdateCryptocurrencyChartData()
                .also { Log.d("myApp/viewModel/coroutines/coroutineChartData", "REQ UPDATE") }
        }

    }


//observers

private val _allCryptocurrenciesPricesObserver =
    androidx.lifecycle.Observer<List<PriceEntity>> {
        if (it.isNotEmpty()) {
            requestUpdateSelectedCryptocurrencyPriceData()
        }
    }

private val _cryptocurrenciesInfoWithinTimeRangeObserver =
    androidx.lifecycle.Observer<List<InfoWithinTimeRangeEntity>> { //TODO: it should run now!!!
        if (it.isNotEmpty()) {
            requestUpdateCryptocurrencyChartData()
        }

        _cryptocurrencyChartData =
            repository.getCryptocurrencyChartData(
                selectedCryptocurrencyId!!,
                selectedDaysToSeeOnChart!!
            )
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
        1 -> calendar.add(Calendar.DAY_OF_MONTH, -1)
        7 -> calendar.add(Calendar.DAY_OF_MONTH, -7)
        31 -> calendar.add(Calendar.MONTH, -1)
        365 -> calendar.add(Calendar.YEAR, -1)
    }
    val dateStart = calendar.time

    selectedUnixTimeFrom = (dateStart.time / 1000)
    selectedUnixTimeTo = (dateEnd.time / 1000)
}

private fun loadCachedData() {
    requestUpdateSelectedCryptocurrencyPriceData()
    requestUpdateCryptocurrencyChartData()
    loadCryptocurrenciesInfoInDateRangeFromCache() //TODO: it is experimental
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
    //TODO(): add api errors handling
}

fun requestUpdateCryptocurrenciesList() {
    if (_isUpdateOfCurrenciesListInProgress.value == true) {
        Log.e("myApp", "Update in progress or already done")
        return
    }

    viewModelScope.launch {
        repository.updateCryptocurrenciesList()
    }
}

private fun loadCryptocurrenciesInfoInDateRangeFromCache() {
    recalculateTimeRange()

    if (selectedCryptocurrencyId == null || vsCurrency == null
        || selectedUnixTimeFrom == null || selectedUnixTimeTo == null
        || selectedDaysToSeeOnChart == null
    )
    //check if parameters are valid
    {
        Log.e("myApp", "INVALID PARAMETERS!")
        return
    }

    viewModelScope.launch {
        getCryptocurrenciesInfoWithinTimeRangeLiveData(
            selectedCryptocurrencyId!!,
            selectedDaysToSeeOnChart!!
        )
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
        || selectedDaysToSeeOnChart == null || !hasInternetConnection
    ) //check if parameters are valid
    {
        Log.e(
            "myApp/requestUpdateCryptocurrencyChartData",
            "One or more parameters are not set"
        )
        return
    }

    recalculateTimeRange()
    viewModelScope.launch {
        getCryptocurrenciesInfoWithinTimeRangeLiveData(
            selectedCryptocurrencyId!!,
            selectedDaysToSeeOnChart!!
        )

        repository.updateCryptocurrenciesInfoWithinDateRange( //TODO: it should be called!
            selectedCryptocurrencyId!!,
            vsCurrency!!,
            selectedUnixTimeFrom!!,
            selectedUnixTimeTo!!
        )
    }
}


private fun getCryptocurrenciesInfoWithinTimeRangeLiveData(
    cryptocurrencyId: String,
    daysCount: Int,
) {
    repository.getCryptocurrencyChartData(
        cryptocurrencyId,
        daysCount
    )
}

fun setCurrentlyDisplayedDataUpdatedMinutesAgo(value: Long?) {
    _currentlyDisplayedDataUpdatedMinutesAgo.postValue(value)
}

fun setDataCached(value: Boolean) {
    _isDataCached.postValue(value)
}

}