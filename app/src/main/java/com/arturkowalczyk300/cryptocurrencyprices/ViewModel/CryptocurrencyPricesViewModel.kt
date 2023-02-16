package com.arturkowalczyk300.cryptocurrencyprices.ViewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arturkowalczyk300.cryptocurrencyprices.Model.CryptocurrencyPricesRepository
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.EntityCryptocurrencyInfoInTimeRange
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.EntityCryptocurrencyPrice
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.EntityCryptocurrencyTop100ByMarketCap
import java.util.*


class CryptocurrencyPricesViewModel(application: Application) : ViewModel() {
    private var repository: CryptocurrencyPricesRepository = CryptocurrencyPricesRepository(application)
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

    fun updateSelectedCryptocurrencyPriceData(
    ) {
        selectedCryptocurrencyId ?: return
        var dateRequest: Date

        if (showArchivalData) {
            showArchivalDataRange ?: return
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
        //TODO(): add api errors handling
    }


    fun updateCryptocurrenciesList() {
        repository.updateCryptocurrenciesList()
    }

    fun updateCryptocurrenciesInfoInDateRange(
    ) {
        if (this.selectedCryptocurrencyId != null && this.vsCurrency != null && this.selectedUnixTimeFrom != null && this.selectedUnixTimeTo != null) {
            repository.updateCryptocurrenciesInfoInDateRange(
                this.selectedCryptocurrencyId!!,
                this.vsCurrency!!,
                this.selectedUnixTimeFrom!!,
                this.selectedUnixTimeTo!!
            )
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