package com.arturkowalczyk300.cryptocurrencyprices.Model

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesDao
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesDatabase
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesEntityDb
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.CryptocurrencyPriceFromListApi
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.CryptocurrencyPricesWebService
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.RequestWithResponse
import java.util.*
import kotlin.jvm.internal.MutablePropertyReference

class CryptocurrencyPricesRepository(application: Application) {

    val database: CryptocurrencyPricesDatabase? =
        CryptocurrencyPricesDatabase.getDatabase(application)
    val webService: CryptocurrencyPricesWebService = CryptocurrencyPricesWebService()

    fun addReading(reading: CryptocurrencyPricesEntityDb) {
        database?.userDao()?.addReading(reading)
    }

    fun clearAllRecords() {
        database?.userDao()?.deleteAllReadings()
    }

    fun getAllReadings(): LiveData<List<CryptocurrencyPricesEntityDb>>? {
        return database?.userDao()?.getAllReadings()
    }

    fun requestPriceData(
        currencySymbol: String,
        date: Date
    ): MutableLiveData<RequestWithResponse> {
        return webService.requestPriceData(currencySymbol, date)
    }

    fun requestCryptocurrenciesList(): MutableLiveData<ArrayList<CryptocurrencyPriceFromListApi>>{
        return webService.requestCryptocurrenciesList()
    }
}