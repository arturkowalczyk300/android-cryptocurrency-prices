package com.arturkowalczyk300.cryptocurrencyprices.viewModel

import android.app.Application
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
    DEFAULT,
    LOADING,
    DONE
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

    private var _currenciesListLoadingState = MutableLiveData<DataState>()
    val currenciesListLoadingState: LiveData<DataState> = _currenciesListLoadingState

    private var _priceLoadingState = MutableLiveData<DataState>()
    val priceLoadingState: LiveData<DataState> = _priceLoadingState

    private var _chartDataLoadingState = MutableLiveData<DataState>()
    val chartDataLoadingState: LiveData<DataState> = _chartDataLoadingState


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

    fun updateData() {
        updateSelectedCryptocurrencyPriceData()
        updateCryptocurrenciesInfoInDateRange()
    }

    private fun updateSelectedCryptocurrencyPriceData(
    ) {
        if (selectedCryptocurrencyId == null ||
            (showArchivalData && showArchivalDataRange == null)
        ) return

        viewModelScope.launch {
            _allCryptocurrenciesPrices.removeObserver(_allCryptocurrenciesPricesObserver)
            _allCryptocurrenciesPrices.observeForever(_allCryptocurrenciesPricesObserver)

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

    fun updateCryptocurrenciesList() {
        viewModelScope.launch {
            _allCryptocurrencies.removeObserver(_allCryptocurrenciesObserver)
            _allCryptocurrencies.observeForever(_allCryptocurrenciesObserver)

            repository.updateCryptocurrenciesList()
        }
    }

    fun updateCryptocurrenciesInfoInDateRange(
    ) {
        recalculateTimeRange()

        viewModelScope.launch {
            if (selectedCryptocurrencyId != null && vsCurrency != null
                && selectedUnixTimeFrom != null && selectedUnixTimeTo != null
                && selectedDaysToSeeOnChart != null
            ) {
                getCryptocurrenciesInfoWithinTimeRangeLiveData(
                    selectedCryptocurrencyId!!,
                    selectedDaysToSeeOnChart!!
                )

                if (hasInternetConnection)
                    repository.updateCryptocurrenciesInfoInDateRange(
                        selectedCryptocurrencyId!!,
                        vsCurrency!!,
                        selectedUnixTimeFrom!!,
                        selectedUnixTimeTo!!
                    )
            }
        }
    }

    private val _allCryptocurrenciesObserver =
        androidx.lifecycle.Observer<List<CryptocurrencyEntity>> {
            if (it.isNotEmpty())
                _currenciesListLoadingState.value = DataState.DONE
        }

    private val _allCryptocurrenciesPricesObserver =
        androidx.lifecycle.Observer<List<PriceEntity>> {
            if (it.isNotEmpty())
                _priceLoadingState.value = DataState.DONE
        }

    private val _cryptocurrenciesInfoWithinTimeRangeObserver =
        androidx.lifecycle.Observer<List<InfoWithinTimeRangeEntity>> {
            _chartDataLoadingState.value = DataState.DONE

            _cryptocurrenciesInfoWithinTimeRange =
                repository.getCryptocurrenciesInfoWithinTimeRange(
                    selectedCryptocurrencyId!!,
                    selectedDaysToSeeOnChart!!
                )
            cryptocurrenciesInfoWithinTimeRange = _cryptocurrenciesInfoWithinTimeRange
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
        _chartDataLoadingState.value = DataState.LOADING


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