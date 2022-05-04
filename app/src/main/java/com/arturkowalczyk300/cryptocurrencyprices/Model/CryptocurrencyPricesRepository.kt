package com.arturkowalczyk300.cryptocurrencyprices.Model

import android.util.Log
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesDatabase
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.CryptocurrencyPricesWebService

class CryptocurrencyPricesRepository {
    val database: CryptocurrencyPricesDatabase = CryptocurrencyPricesDatabase()
    val webService: CryptocurrencyPricesWebService = CryptocurrencyPricesWebService()
}