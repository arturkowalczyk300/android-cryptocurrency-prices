package com.arturkowalczyk300.cryptocurrencyprices.ViewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturkowalczyk300.cryptocurrencyprices.Model.CryptocurrencyPricesRepository
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.EntityCryptocurrencyInfoInTimeRange
import kotlinx.coroutines.launch
import java.util.*


class CryptocurrencyPricesViewModel(application: Application) : ViewModel() {
    private var repository: CryptocurrencyPricesRepository =
        CryptocurrencyPricesRepository(application)
    val apiUnwrappingPriceDataErrorLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    //livedata properties
    private var _allCryptocurrencies = repository.getAllCryptocurrencies()
    val allCryptocurrencies = _allCryptocurrencies

    private var _allCryptocurrenciesPrices = repository.getAllCryptocurrenciesPrices()
    val allCryptocurrenciesPrices = _allCryptocurrenciesPrices

    private var _cryptocurrenciesInfoInTimeRange: LiveData<List<EntityCryptocurrencyInfoInTimeRange>>? =
        null
    var cryptocurrenciesInfoInTimeRange: LiveData<List<EntityCryptocurrencyInfoInTimeRange>>? =
        _cryptocurrenciesInfoInTimeRange

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

    //parameters for data fetching
    var selectedCryptocurrencyId: String? = null
    var showArchivalData = false
    var showArchivalDataRange: Date? = null
    var vsCurrency: String? = null
    private var selectedUnixTimeFrom: Long? = null
    private var selectedUnixTimeTo: Long? = null
    var selectedDaysToSeeOnChart: Int? = null
        set(value) {
            if (value != field) {
                resetFlagIsDataUpdatedSuccessfully()
            }
            field = value
        }

    //info
    var hasInternetConnection: Boolean = false
    var noDataCachedVisibility: Boolean = false
    var isDataUpdatedSuccessfully = repository.isDataUpdatedSuccessfully

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

    private fun resetFlagIsDataUpdatedSuccessfully() {
        repository.resetFlagIsDataUpdatedSuccessfully()
    }

    fun updateSelectedCryptocurrencyPriceData(
    ) {
        if (selectedCryptocurrencyId == null ||
            (showArchivalData && showArchivalDataRange == null)
        ) return

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


    fun updateCryptocurrenciesList() {
        viewModelScope.launch {
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
                getCryptocurrenciesInfoInTimeRange(
                    selectedCryptocurrencyId!!,
                    selectedDaysToSeeOnChart!!
                )

                repository.updateCryptocurrenciesInfoInDateRange(
                    selectedCryptocurrencyId!!,
                    vsCurrency!!,
                    selectedUnixTimeFrom!!,
                    selectedUnixTimeTo!!
                )
            }
        }
    }

    private fun getCryptocurrenciesInfoInTimeRange(
        cryptocurrencyId: String,
        daysCount: Int,
    ) {
        _cryptocurrenciesInfoInTimeRange = repository.getCryptocurrenciesInfoInTimeRange(
            cryptocurrencyId,
            daysCount
        )
        cryptocurrenciesInfoInTimeRange = _cryptocurrenciesInfoInTimeRange //TODO: refactor
    }

    fun setCurrentlyDisplayedDataUpdatedMinutesAgo(value: Long?) {
        _currentlyDisplayedDataUpdatedMinutesAgo.postValue(value)
    }

    fun setDataCached(value: Boolean) {
        _isDataCached.postValue(value)
    }

}