package com.arturkowalczyk300.cryptocurrencyprices.ViewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arturkowalczyk300.cryptocurrencyprices.Model.CryptocurrencyPricesRepository
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesEntityDb
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.CryptocurrencyPriceFromListApi
import java.util.*


class CryptocurrencyPricesViewModel(application: Application) : ViewModel() {
    var repository: CryptocurrencyPricesRepository = CryptocurrencyPricesRepository(application)
    var lastAddedObject: CryptocurrencyPricesEntityDb? = null
    val apiUnwrappingPriceDataErrorLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    var selectedCryptocurrencyId: String? = null
    var showArchivalData = false
    var showArchivalDataRange: Date? = null
    var vs_currency: String? = null

    fun requestPriceData(
    ) {
        selectedCryptocurrencyId ?: return
        var dateRequest: Date

        if (showArchivalData) {
            showArchivalDataRange ?: return
            dateRequest = showArchivalDataRange!!

            repository.requestArchivalPriceData(selectedCryptocurrencyId!!, dateRequest)
                .observeForever(androidx.lifecycle.Observer {
                    if (it.flagDataSet) {
                        if (it?.entity?.market_data?.current_price != null) {
                            apiUnwrappingPriceDataErrorLiveData.value = false
                            it.flagDataSet = false

                            addReading(
                                it.currencySymbol,
                                it.date,
                                it.entity!!.market_data.current_price.usd
                            )
                        } else apiUnwrappingPriceDataErrorLiveData.value = true
                    }
                })
        } else {
            dateRequest = Date()

            repository.requestActualPriceData(selectedCryptocurrencyId!!, vs_currency!!)
                .observeForever(androidx.lifecycle.Observer {
                    if (it.flagDataSet) {
                        if (it?.actualPrice != null) {
                            apiUnwrappingPriceDataErrorLiveData.value = false
                            it.flagDataSet = false

                            addReading(
                                it.currencySymbol,
                                it.date,
                                it.actualPrice!!.toDouble()
                            )
                        } else apiUnwrappingPriceDataErrorLiveData.value = true
                    }
                })
        }


    }

    fun requestCryptocurrenciesList(): LiveData<ArrayList<CryptocurrencyPriceFromListApi>> {
        return repository.requestCryptocurrenciesList()
    }

    fun requestPriceHistoryForSelectedDateRange(
        currencySymbol: String, vs_currency: String, unixtimeFrom: Long, unixTimeTo: Long
    ): MutableLiveData<List<List<Double>>?> {
        return repository.requestPriceHistoryForDateRange(
            currencySymbol, vs_currency, unixtimeFrom, unixTimeTo
        )
    }

    fun clearAllRecords() {
        repository.clearAllRecords()
    }

    private fun addReading(cryptocurrencyIdArg: String, dateArg: Date, priceUsdArg: Double) {
        lastAddedObject = CryptocurrencyPricesEntityDb(
            cryptocurrencyId = cryptocurrencyIdArg, date = dateArg, priceUsd = priceUsdArg
        )

        repository.addReading(lastAddedObject!!)
    }

    fun getAllReadings(): LiveData<List<CryptocurrencyPricesEntityDb>>? {
        return repository.getAllReadings()
    }

    fun getApiErrorCodeLiveData() = repository.getApiErrorCodeLiveData()
}