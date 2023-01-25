package com.arturkowalczyk300.cryptocurrencyprices.ViewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arturkowalczyk300.cryptocurrencyprices.Model.CryptocurrencyPricesRepository
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.EntityCryptocurrenciesHistoricalPrices
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.EntityCryptocurrenciesTop100ByMarketCap
import java.util.*


class CryptocurrencyPricesViewModel(application: Application) : ViewModel() {
    var repository: CryptocurrencyPricesRepository = CryptocurrencyPricesRepository(application)
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

    private fun resetFlagIsDataUpdatedSuccessfully() {
        repository.resetFlagIsDataUpdatedSuccessfully()
    }

    fun updatePriceData(
    ) {
        selectedCryptocurrencyId ?: return
        var dateRequest: Date

        if (showArchivalData) {
            showArchivalDataRange ?: return
            dateRequest = showArchivalDataRange!!

            repository.updateArchivalPriceData(selectedCryptocurrencyId!!, dateRequest)
        } else {
            repository.updateActualPriceData(selectedCryptocurrencyId!!, vsCurrency!!)
        }
        //TODO(): add api errors handling
    }


    fun updateCryptocurrenciesList() {
        repository.updateCryptocurrenciesList()
    }

    fun updatePriceHistoryForSelectedDateRange(
    ) {
        if (this.selectedCryptocurrencyId != null && this.vsCurrency != null && this.selectedUnixTimeFrom != null && this.selectedUnixTimeTo != null) {
            repository.updatePriceHistoryForDateRange(
                this.selectedCryptocurrencyId!!,
                this.vsCurrency!!,
                this.selectedUnixTimeFrom!!,
                this.selectedUnixTimeTo!!
            )
        }
    }

    fun getApiErrorCodeLiveData() = repository.getApiErrorCodeLiveData()

    fun getAllCryptocurrencies(): LiveData<List<EntityCryptocurrenciesTop100ByMarketCap>> {
        return repository.getAllCryptocurrencies()
    }

    fun getAllHistoricalPrices(): LiveData<List<EntityCryptocurrenciesHistoricalPrices>> {
        return repository.getAllHistoricalPrices()
    }

    fun getHistoricalPriceOfCryptocurrenciesWithTimeRange(): LiveData<List<EntityCryptocurrenciesHistoricalPrices>> {
        return repository.getHistoricalPriceOfCryptocurrenciesWithTimeRange()
    }


    fun getHistoricalPricesOfCryptocurrencyInTimeRange(
        cryptocurrencyId: String,
        daysCount: Int,
    ): LiveData<List<EntityCryptocurrenciesHistoricalPrices>> {
        return repository.getHistoricalPricesOfCryptocurrencyInTimeRange(
            cryptocurrencyId,
            daysCount
        )
    }

}