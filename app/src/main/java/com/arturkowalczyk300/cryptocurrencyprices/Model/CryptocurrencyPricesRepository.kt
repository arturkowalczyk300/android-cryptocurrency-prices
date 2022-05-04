package com.arturkowalczyk300.cryptocurrencyprices.Model

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesDatabase
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.CryptocurrencyPricesWebService
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.RequestWithResponse
import java.util.*

class CryptocurrencyPricesRepository {
    val database: CryptocurrencyPricesDatabase = CryptocurrencyPricesDatabase()
    val webService: CryptocurrencyPricesWebService = CryptocurrencyPricesWebService()

    fun requestPriceData(
        currencySymbol: String,
        date: Date
    ): MutableLiveData<RequestWithResponse> {
        return webService.requestPriceData(currencySymbol, date)
    }
}