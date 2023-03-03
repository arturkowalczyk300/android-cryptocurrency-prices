package com.arturkowalczyk300.cryptocurrencyprices.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturkowalczyk300.cryptocurrencyprices.model.Repository
import com.arturkowalczyk300.cryptocurrencyprices.model.room.CryptocurrencyEntity
import com.arturkowalczyk300.cryptocurrencyprices.model.room.InfoWithinTimeRangeEntity
import com.arturkowalczyk300.cryptocurrencyprices.model.room.PriceEntity
import kotlinx.coroutines.launch
import java.util.*

enum class DataState {
    IDLE,
    LOADING_FROM_CACHE,
    SHOW_CACHED_DATA,
    UPDATING,
    UPDATE_DONE,
    ERROR
}

class MainViewModel(application: Application) : ViewModel() {
    private var repository: Repository =
        Repository(application)
    val apiUnwrappingPriceDataErrorLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    //livedata properties
    private var _allCryptocurrencies = repository.getAllCryptocurrencies()
    val allCryptocurrencies = _allCryptocurrencies

    private var _allCryptocurrenciesPrices = repository.getAllCryptocurrenciesPrices()
    val allCryptocurrenciesPrices = _allCryptocurrenciesPrices

    private var _cryptocurrenciesInfoWithinTimeRange: LiveData<List<InfoWithinTimeRangeEntity>>? =
        null
    var cryptocurrenciesInfoWithinTimeRange: LiveData<List<InfoWithinTimeRangeEntity>>? =
        _cryptocurrenciesInfoWithinTimeRange

    //statuses of operations
    private var _apiErrorCode = repository.getApiErrorCodeLiveData()
    val apiErrorCode = _apiErrorCode

    private var _currentlyDisplayedDataUpdatedMinutesAgo: MutableLiveData<Long?> =
        MutableLiveData<Long?>()
    val currentlyDisplayedDataUpdatedMinutesAgo: LiveData<Long?> =
        _currentlyDisplayedDataUpdatedMinutesAgo

    private var _isDataCached: MutableLiveData<Boolean> =
        MutableLiveData()
    val isDataCached: LiveData<Boolean> = _isDataCached

    //data loading state variables
    private var _currenciesListLoadingState = MutableLiveData(DataState.IDLE)
    val currenciesListLoadingState: LiveData<DataState> = _currenciesListLoadingState

    private var _priceLoadingState = MutableLiveData(DataState.IDLE)
    val priceLoadingState: LiveData<DataState> = _priceLoadingState

    private var _infoWithinDataRangeLoadingState = MutableLiveData(DataState.IDLE)
    val infoWithinDataRangeLoadingState: LiveData<DataState> = _infoWithinDataRangeLoadingState


    //parameters for data fetching
    var selectedCryptocurrencyId: String? = null
    var showArchivalData = false
    var showArchivalDataRange: Date? = null
    var vsCurrency: String? = null
    private var selectedUnixTimeFrom: Long? = null
    private var selectedUnixTimeTo: Long? = null
    var selectedDaysToSeeOnChart: Int? = null
    var isChartFragmentInitialized = false

    //info
    var hasInternetConnection: Boolean = false
    var noDataCachedVisibility: Boolean = false

    //observers
    private val _allCryptocurrenciesObserver =
        androidx.lifecycle.Observer<List<CryptocurrencyEntity>> {
            if (it.isNotEmpty()) {
                if (_currenciesListLoadingState.value == DataState.LOADING_FROM_CACHE) {
                    _currenciesListLoadingState.value = DataState.SHOW_CACHED_DATA
                    requestUpdateCryptocurrenciesList()
                } else if (_currenciesListLoadingState.value == DataState.UPDATING)
                    _currenciesListLoadingState.value = DataState.UPDATE_DONE
            }
        }


    private val _allCryptocurrenciesPricesObserver =
        androidx.lifecycle.Observer<List<PriceEntity>> {
            if (it.isNotEmpty())
                if (_priceLoadingState.value == DataState.LOADING_FROM_CACHE) {
                    _priceLoadingState.value = DataState.SHOW_CACHED_DATA
                    requestUpdateSelectedCryptocurrencyPriceData()
                } else if (_priceLoadingState.value == DataState.UPDATING)
                    _priceLoadingState.value = DataState.UPDATE_DONE
        }

    private val _cryptocurrenciesInfoWithinTimeRangeObserver =
        androidx.lifecycle.Observer<List<InfoWithinTimeRangeEntity>> {

            if (it.isNotEmpty()) {
                if (_infoWithinDataRangeLoadingState.value == DataState.LOADING_FROM_CACHE) {
                    _infoWithinDataRangeLoadingState.value = DataState.SHOW_CACHED_DATA
                    requestUpdateCryptocurrenciesInfoInDateRange()
                } else if (_infoWithinDataRangeLoadingState.value == DataState.UPDATING)
                    _infoWithinDataRangeLoadingState.value = DataState.UPDATE_DONE
            }

            _cryptocurrenciesInfoWithinTimeRange =
                repository.getCryptocurrenciesInfoWithinTimeRange(
                    selectedCryptocurrencyId!!,
                    selectedDaysToSeeOnChart!!
                )
            cryptocurrenciesInfoWithinTimeRange = _cryptocurrenciesInfoWithinTimeRange
        }

    init {
        _allCryptocurrencies.observeForever(_allCryptocurrenciesObserver)
        _allCryptocurrenciesPrices.observeForever(_allCryptocurrenciesPricesObserver)

        _currenciesListLoadingState.value = DataState.LOADING_FROM_CACHE

        //TODO: delete after debug
        currenciesListLoadingState.observeForever() {
            Log.e("myApp", "_currenciesListLoadingState, new value=${it}")
        }

        priceLoadingState.observeForever() {
            Log.e("myApp", "_priceLoadingState, new value=${it}")
        }

        infoWithinDataRangeLoadingState.observeForever() {
            Log.e("myApp", "_infoWithinDataRangeLoadingState, new value=${it}")
        }
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

    fun updateData() { //TODO: its probably best to make it private, viewmodel should update data by itself
        requestUpdateSelectedCryptocurrencyPriceData()
        requestUpdateCryptocurrenciesInfoInDateRange()
    }

    private fun requestUpdateSelectedCryptocurrencyPriceData(
    ) {
        if (_priceLoadingState.value == DataState.UPDATING
            || _priceLoadingState.value == DataState.UPDATE_DONE
        )
            return //update in progress or already done

        if (selectedCryptocurrencyId == null ||
            (showArchivalData && showArchivalDataRange == null)
        ) {
            return
        }

        _priceLoadingState.value = DataState.UPDATING

        viewModelScope.launch {
            _allCryptocurrenciesPrices.removeObserver(_allCryptocurrenciesPricesObserver)
            _allCryptocurrenciesPrices.observeForever(_allCryptocurrenciesPricesObserver) //TODO: init block

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
        if (_currenciesListLoadingState.value == DataState.UPDATING ||
            _currenciesListLoadingState.value == DataState.UPDATE_DONE
        )
            return //update in progress or already done

        _currenciesListLoadingState.value = DataState.UPDATING

        viewModelScope.launch {
            _allCryptocurrencies.removeObserver(_allCryptocurrenciesObserver)
            _allCryptocurrencies.observeForever(_allCryptocurrenciesObserver)

            repository.updateCryptocurrenciesList()
        }
    }

    private fun requestUpdateCryptocurrenciesInfoInDateRange(
    ) {
        if (_infoWithinDataRangeLoadingState.value == DataState.UPDATING ||
            _infoWithinDataRangeLoadingState.value == DataState.UPDATE_DONE
        )
            return //update in progress or already done

        if (infoWithinDataRangeLoadingState.value != DataState.SHOW_CACHED_DATA &&
            infoWithinDataRangeLoadingState.value != DataState.UPDATE_DONE
        )
            return //not valid state on beginning

        if (selectedCryptocurrencyId == null || vsCurrency == null
            || selectedUnixTimeFrom == null || selectedUnixTimeTo == null
            || selectedDaysToSeeOnChart == null || !hasInternetConnection
        ) //check if parameters are valid
            return

        _infoWithinDataRangeLoadingState.value = DataState.UPDATING

        recalculateTimeRange()

        viewModelScope.launch {
            getCryptocurrenciesInfoWithinTimeRangeLiveData(
                selectedCryptocurrencyId!!,
                selectedDaysToSeeOnChart!!
            )

            repository.updateCryptocurrenciesInfoWithinDateRange(
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
        _cryptocurrenciesInfoWithinTimeRange?.removeObserver(
            _cryptocurrenciesInfoWithinTimeRangeObserver
        )

        _cryptocurrenciesInfoWithinTimeRange = repository.getCryptocurrenciesInfoWithinTimeRange(
            cryptocurrencyId,
            daysCount
        )
        cryptocurrenciesInfoWithinTimeRange = _cryptocurrenciesInfoWithinTimeRange
        _infoWithinDataRangeLoadingState.value = DataState.UPDATING


        _cryptocurrenciesInfoWithinTimeRange?.observeForever(
            _cryptocurrenciesInfoWithinTimeRangeObserver
        )
    }

    fun setCurrentlyDisplayedDataUpdatedMinutesAgo(value: Long?) {
        _currentlyDisplayedDataUpdatedMinutesAgo.postValue(value)
    }

    fun setDataCached(value: Boolean) {
        _isDataCached.postValue(value)
    }

}