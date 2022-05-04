package com.arturkowalczyk300.cryptocurrencyprices.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arturkowalczyk300.cryptocurrencyprices.Model.CryptocurrencyPricesRepository
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesEntityDb
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.RequestWithResponse
import java.util.*

class CryptocurrencyPricesViewModel(application: Application) : ViewModel() {
    var repository: CryptocurrencyPricesRepository = CryptocurrencyPricesRepository(application)

    fun requestPriceData(
        currencySymbol: String,
        date: Date
    ): MutableLiveData<RequestWithResponse> {
        return repository.requestPriceData(currencySymbol, date)
    }

    fun clearAllRecords() {
        repository.clearAllRecords()
    }

    fun addReading() {
        repository.addReading(
            CryptocurrencyPricesEntityDb(
                cryptocurrencyId = "dsczxa",
                date = Date(2342),
                priceUsd = 0.1223
            )
        )
    }

    fun getAllReadings() {
        repository.getAllReadings()
            ?.observeForever(androidx.lifecycle.Observer { t ->
                t.forEach {
                    //Log.v("myApp", "element")
                }
                Log.v("myApp", "count: ${t.size}")
            })
    }
}