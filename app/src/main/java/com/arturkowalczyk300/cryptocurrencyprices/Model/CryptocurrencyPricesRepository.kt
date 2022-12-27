package com.arturkowalczyk300.cryptocurrencyprices.Model

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.*
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.CryptocurrencyPriceFromListApi
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.CryptocurrencyPricesWebService
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.RequestWithResponse
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.RequestWithResponseArchival
import java.util.*
import kotlin.collections.ArrayList

class CryptocurrencyPricesRepository(application: Application) {

    val database: CryptocurrencyPricesDatabase? =
        CryptocurrencyPricesDatabase.getDatabase(application)
    val webService: CryptocurrencyPricesWebService = CryptocurrencyPricesWebService()

    /////////////////////////////////////////////////////////////////////////////////////
    //database CRUD methods
    /////////////////////////////////////////////////////////////////////////////////////

    private fun addCryptocurrencyToTop100ByMarketCapTable(entity: EntityCryptocurrenciesTop100ByMarketCap) {
        database!!.userDao()!!.addCryptocurrencyToTop100ByMarketCapTable(entity)
    }

    fun getAllCryptocurrencies(): LiveData<List<EntityCryptocurrenciesTop100ByMarketCap>> {
        return database!!.userDao()!!.getAllCryptocurrencies()
    }

    private fun deleteAllCryptocurrencies() {
        database!!.userDao()!!.deleteAllCryptocurrencies()
    }

    private fun addHistoricalPrice(entity: EntityCryptocurrenciesHistoricalPrices) {
        database!!.userDao()!!.addHistoricalPrice(entity)
    }

    fun getAllHistoricalPrices(): LiveData<List<EntityCryptocurrenciesHistoricalPrices>> {
        return database!!.userDao()!!.getAllHistoricalPrices()
    }

    private fun deleteAllHistoricalPrices() {
        database!!.userDao()!!.deleteAllHistoricalPrices()
    }

    fun getHistoricalPriceOfCryptocurrencyContainsGivenDay(
        cryptocurrencyId: String,
        unixTimeDay: Long
    ): LiveData<EntityCryptocurrenciesHistoricalPrices> {
        return database!!.userDao()!!.getHistoricalPriceOfCryptocurrencyContainsGivenDay(
            cryptocurrencyId,
            unixTimeDay
        )
    }

    private fun deleteHistoricalPriceOfCryptocurrencyContainsGivenDay(
        cryptocurrencyId: String,
        unixTimeDay: Long
    ) {
        database!!.userDao()!!.deleteHistoricalPriceOfCryptocurrencyContainsGivenDay(
            cryptocurrencyId,
            unixTimeDay
        )
    }
    /////////////////////////////////////////////////////////////////////////////////////
    //methods for request update data in database through webservice
    /////////////////////////////////////////////////////////////////////////////////////

    fun updateActualPriceData(
        currencySymbol: String,
        vs_currency: String
    ) {
        Log.d("myApp", "updateActualPriceData")

        val liveData = webService.requestActualPriceData(currencySymbol, vs_currency)
        if (!liveData.hasActiveObservers())
            liveData.observeForever { response ->
                if (response?.actualPrice != null) //value check
                {
                    this.addHistoricalPrice(
                        EntityCryptocurrenciesHistoricalPrices(
                            index = 0, //auto-increment, no need to specify manually
                            cryptocurrencyId = response.currencySymbol,
                            timeRangeFrom = response.date.time,
                            timeRangeTo = response.date.time,
                            market_caps = null,
                            prices =
                            ListOfCryptocurrencyStatValuesWithTime(
                                listOf(
                                    CryptocurrencyStatValueWithTime(
                                        response.date.time,
                                        response.actualPrice!!.toDouble()
                                    )
                                )
                            ),
                            total_volumes = null
                        )
                    )
                }
            }
    }

    fun updateArchivalPriceData(
        currencySymbol: String,
        date: Date
    ) {
        Log.d("myApp", "updateArchivalPriceData")

        val liveData = webService.requestArchivalPriceData(currencySymbol, date)
        if (!liveData.hasActiveObservers())
            liveData.observeForever { response ->
                if (response?.entity != null) //value check
                {
                    this.addHistoricalPrice(
                        EntityCryptocurrenciesHistoricalPrices(
                            index = 0, //auto-increment, no need to specify manually
                            cryptocurrencyId = response.currencySymbol,
                            timeRangeFrom = response.date.time,
                            timeRangeTo = response.date.time,
                            market_caps = ListOfCryptocurrencyStatValuesWithTime(
                                listOf(
                                    CryptocurrencyStatValueWithTime(
                                        response.date.time,
                                        response.entity!!.market_data.market_cap.usd
                                    )
                                )
                            ),
                            prices =
                            ListOfCryptocurrencyStatValuesWithTime(
                                listOf(
                                    CryptocurrencyStatValueWithTime(
                                        response.date.time,
                                        response.entity!!.market_data.current_price.usd
                                    )
                                )
                            ),
                            total_volumes = ListOfCryptocurrencyStatValuesWithTime(
                                listOf(
                                    CryptocurrencyStatValueWithTime(
                                        response.date.time,
                                        response.entity!!.market_data.total_volume.usd
                                    )
                                )
                            )
                        )
                    )
                }
            }
    }

    fun updateCryptocurrenciesList() {
        Log.d("myApp", "updateCryptocurrenciesList")

        val liveData = webService.requestCryptocurrenciesList()
        if (!liveData.hasActiveObservers())
            liveData.observeForever() { response ->
                this.deleteAllCryptocurrencies()
                response.forEach { row ->
                    this.addCryptocurrencyToTop100ByMarketCapTable(
                        EntityCryptocurrenciesTop100ByMarketCap(
                            market_cap_rank = row.market_cap_rank,
                            cryptocurrencyId = row.id,
                            name = row.name,
                            marketCap = row.market_cap,
                            currentPrice = row.current_price,
                            updateDate = Date()
                        )
                    )
                }

            }
    }

    fun updatePriceHistoryForDateRange(
        currencySymbol: String, vs_currency: String, unixtimeFrom: Long,
        unixTimeTo: Long
    ) {
        Log.d("myApp", "updatePriceHistoryForDateRange")

        val liveData = webService.requestPriceHistoryForDateRange(
            currencySymbol,
            vs_currency,
            unixtimeFrom,
            unixTimeTo
        )
        if (!liveData.hasActiveObservers())
            liveData.observeForever { response ->
                if (response != null) {

                    val list: List<CryptocurrencyStatValueWithTime> =
                        response.archivalPrices!!.map {
                            CryptocurrencyStatValueWithTime(
                                it[0].toLong(),
                                it[1]
                            ) //TODO: check type
                        }
                    val prices = ListOfCryptocurrencyStatValuesWithTime(list)

                    this.addHistoricalPrice(
                        EntityCryptocurrenciesHistoricalPrices(
                            index = 0, //auto-increment, no need to specify manually
                            cryptocurrencyId = response.currencySymbol,
                            timeRangeFrom = response.unixtimeFrom,
                            timeRangeTo = response.unixTimeTo,
                            prices = prices
                        )
                    )
                }
            }
    }

    fun getApiErrorCodeLiveData() = webService.mldErrorCode
}