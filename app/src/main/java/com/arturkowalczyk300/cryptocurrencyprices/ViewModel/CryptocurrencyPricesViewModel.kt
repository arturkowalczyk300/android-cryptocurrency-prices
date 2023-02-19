package com.arturkowalczyk300.cryptocurrencyprices.ViewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturkowalczyk300.cryptocurrencyprices.Model.CryptocurrencyPricesRepository
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.EntityCryptocurrencyInfoInTimeRange
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.EntityCryptocurrencyPrice
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.EntityCryptocurrencyTop100ByMarketCap
import com.arturkowalczyk300.cryptocurrencyprices.R
import kotlinx.coroutines.launch
import java.util.*


class CryptocurrencyPricesViewModel(application: Application) : ViewModel() {
    private var repository: CryptocurrencyPricesRepository =
        CryptocurrencyPricesRepository(application)
    val apiUnwrappingPriceDataErrorLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    //parameters for data fetching
    var selectedCryptocurrencyId: String? = null
    var showArchivalData = false
    var showArchivalDataRange: Date? = null
    var vsCurrency: String? = null
    var selectedUnixTimeFrom: Long? = null
    var selectedUnixTimeTo: Long? = null

    var selectedDaysToSeeOnChart: Int? = null
        set(value) {
            if (value != field) {
                resetFlagIsDataUpdatedSuccessfully()
            }
            field = value
        }

    var hasInternetConnection: Boolean = false
    var currentlyDisplayedDataUpdatedMinutesAgo: MutableLiveData<Long?> = MutableLiveData<Long?>()
    var noCachedData: MutableLiveData<Boolean> = MutableLiveData()
    var noCachedDataVisibility: Boolean = false
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
            if (selectedCryptocurrencyId != null && vsCurrency != null && selectedUnixTimeFrom != null && selectedUnixTimeTo != null) {
                repository.updateCryptocurrenciesInfoInDateRange(
                    selectedCryptocurrencyId!!,
                    vsCurrency!!,
                    selectedUnixTimeFrom!!,
                    selectedUnixTimeTo!!
                )
            }
        }
    }

    fun getApiErrorCodeLiveData() = repository.getApiErrorCodeLiveData()

    fun getAllCryptocurrencies(): LiveData<List<EntityCryptocurrencyTop100ByMarketCap>> {
        return repository.getAllCryptocurrencies()
    }

    fun getAllCryptocurrenciesPrices(): LiveData<List<EntityCryptocurrencyPrice>> {
        return repository.getAllCryptocurrenciesPrices()
    }

    fun getAllCryptocurrenciesInfo(): LiveData<List<EntityCryptocurrencyInfoInTimeRange>> {
        return repository.getAllCryptocurrenciesInfo()
    }

    fun getCryptocurrencyInfoInTimeRange(): LiveData<List<EntityCryptocurrencyInfoInTimeRange>> {
        return repository.getCryptocurrencyInfoInTimeRange()
    }


    fun getCryptocurrenciesInfoInTimeRange(
        cryptocurrencyId: String,
        daysCount: Int,
    ): LiveData<List<EntityCryptocurrencyInfoInTimeRange>> {
        return repository.getCryptocurrenciesInfoInTimeRange(
            cryptocurrencyId,
            daysCount
        )
    }

}