package com.arturkowalczyk300.cryptocurrencyprices.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
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
    ) {
        repository.requestPriceData(currencySymbol, date)
            .observeForever(androidx.lifecycle.Observer {
                //it.entity?.market_data?.current_price?.usd.run {
                if (it.entity != null)
                    addReading(
                        it.currencySymbol,
                        it.date,
                        it.entity!!.market_data!!.current_price!!.usd
                    )

            })
    }

    fun clearAllRecords() {
        repository.clearAllRecords()
    }

    private fun addReading(cryptocurrencyIdArg: String, dateArg: Date, priceUsdArg: Double) {
        repository.addReading(
            CryptocurrencyPricesEntityDb(
                cryptocurrencyId = cryptocurrencyIdArg,
                date = dateArg,
                priceUsd = priceUsdArg
            )
        )
    }

    fun getAllReadings(): LiveData<List<CryptocurrencyPricesEntityDb>>? {
        return repository.getAllReadings()
    }
}