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
    var selectedCryptocurrencyId: String? = null
    var showArchivalData = false
    var showArchivalDataRange: Date? = null
    var vs_currency: String? = null

    fun updatePriceData(
    ) {
        selectedCryptocurrencyId ?: return
        var dateRequest: Date

        if (showArchivalData) {
            showArchivalDataRange ?: return
            dateRequest = showArchivalDataRange!!

            repository.updateArchivalPriceData(selectedCryptocurrencyId!!, dateRequest)
        } else {
            dateRequest = Date()

            repository.updateActualPriceData(selectedCryptocurrencyId!!, vs_currency!!)
        }
        //TODO(): add api errors handling
    }


    fun updateCryptocurrenciesList() {
        repository.updateCryptocurrenciesList()
    }

    fun updatePriceHistoryForSelectedDateRange(
        currencySymbol: String, vs_currency: String, unixtimeFrom: Long, unixTimeTo: Long
    ) {
        repository.updatePriceHistoryForDateRange(
            currencySymbol, vs_currency, unixtimeFrom, unixTimeTo
        )
    }

    fun getApiErrorCodeLiveData() = repository.getApiErrorCodeLiveData()


/*

NEW

 */

    fun getAllCryptocurrencies(): LiveData<List<EntityCryptocurrenciesTop100ByMarketCap>> {
        return repository.getAllCryptocurrencies()
    }

    fun getAllHistoricalPrices(): LiveData<List<EntityCryptocurrenciesHistoricalPrices>> {
        return repository.getAllHistoricalPrices()
    }

    fun getHistoricalPriceOfCryptocurrencyContainsGivenDay(
        cryptocurrencyId: String,
        unixTimeDay: Long
    ): LiveData<EntityCryptocurrenciesHistoricalPrices> {
        return repository.getHistoricalPriceOfCryptocurrencyContainsGivenDay(
            cryptocurrencyId,
            unixTimeDay
        )
    }

}