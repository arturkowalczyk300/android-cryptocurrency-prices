package com.arturkowalczyk300.cryptocurrencyprices.Model

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.*
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.CryptocurrencyPricesWebService
import retrofit2.http.GET
import java.util.*

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
        //limit historical entries to one per currency and time range only
        if (entity.daysCount > 0)
            database!!.userDao()!!.deleteAllHistoricalPricesOfCryptocurrencyInGivenDaysCount(
                entity.cryptocurrencyId,
                entity.daysCount
            )
        database!!.userDao()!!.addHistoricalPrice(entity)
    }

    fun getAllHistoricalPrices(): LiveData<List<EntityCryptocurrenciesHistoricalPrices>> {
        return database!!.userDao()!!.getAllHistoricalPrices()
    }

    private fun deleteAllHistoricalPrices() {
        database!!.userDao()!!.deleteAllHistoricalPrices()
    }

    fun getHistoricalPriceOfCryptocurrenciesWithTimeRange(): LiveData<List<EntityCryptocurrenciesHistoricalPrices>> {
        return database!!.userDao()!!.getHistoricalPriceOfCryptocurrenciesWithTimeRange(
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
        val liveData = webService.requestPriceHistoryForDateRange(
            currencySymbol,
            vs_currency,
            unixtimeFrom,
            unixTimeTo
        )
        if (!liveData.hasActiveObservers())
            liveData.observeForever { response ->
                if (response != null && response?.archivalPrices?.isNotEmpty() != null) {

                    val list: List<CryptocurrencyStatValueWithTime> =
                        response.archivalPrices!!.map {
                            CryptocurrencyStatValueWithTime(
                                it[0].toLong(),
                                it[1]
                            ) //TODO: check type
                        }
                    val prices = ListOfCryptocurrencyStatValuesWithTime(list)

                    val totalVolume =
                        ListOfCryptocurrencyStatValuesWithTime(
                            response.totalVolumes!!.map {
                                CryptocurrencyStatValueWithTime(
                                    it[0].toLong(),
                                    it[1]
                                )
                            })

                    val marketCaps =
                        ListOfCryptocurrencyStatValuesWithTime(
                            response.marketCaps!!.map {
                                CryptocurrencyStatValueWithTime(
                                    it[0].toLong(),
                                    it[1]
                                )
                            })

                    this.addHistoricalPrice(
                        EntityCryptocurrenciesHistoricalPrices(
                            index = 0, //auto-increment, no need to specify manually
                            cryptocurrencyId = response.currencySymbol,
                            timeRangeFrom = response.unixtimeFrom,
                            timeRangeTo = response.unixTimeTo,
                            daysCount = (((response.unixTimeTo - response.unixtimeFrom) / 3600 / 24).toInt()),
                            prices = prices,
                            market_caps = marketCaps,
                            total_volumes = totalVolume
                        )
                    )
                }
            }
    }

    fun deleteAllHistoricalPricesOfCryptocurrencyInGivenDaysCount(
        cryptocurrencyId: String,
        daysCount: Int
    ){
        database!!.userDao()!!
            .deleteAllHistoricalPricesOfCryptocurrencyInGivenDaysCount(cryptocurrencyId, daysCount)
    }

    fun getHistoricalPricesOfCryptocurrencyInTimeRange(
        cryptocurrencyId: String,
        daysCount: Int
    ): LiveData<List<EntityCryptocurrenciesHistoricalPrices>> {
        return database!!.userDao()!!
            .getHistoricalPricesOfCryptocurrencyInTimeRange(cryptocurrencyId, daysCount)
    }

    fun getApiErrorCodeLiveData() = webService.mldErrorCode
}