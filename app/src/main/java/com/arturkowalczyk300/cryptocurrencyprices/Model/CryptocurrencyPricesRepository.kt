package com.arturkowalczyk300.cryptocurrencyprices.Model

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.*
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.CryptocurrencyPricesWebService
import kotlinx.coroutines.runBlocking
import java.util.*

class CryptocurrencyPricesRepository(application: Application) {

    private val database: CryptocurrencyPricesDatabase? =
        CryptocurrencyPricesDatabase.getDatabase(application)
    private val webService: CryptocurrencyPricesWebService = CryptocurrencyPricesWebService()

    var isDataUpdatedSuccessfully = webService.isDataUpdatedSuccessfully

    /////////////////////////////////////////////////////////////////////////////////////
    //database CRUD methods
    /////////////////////////////////////////////////////////////////////////////////////

    private fun addCryptocurrencyToTop100ByMarketCapTable(entity: EntityCryptocurrencyTop100ByMarketCap) {
        runBlocking {
            database!!.userDao()!!.addCryptocurrencyToTop100ByMarketCapTable(entity)
        }
    }

    fun resetFlagIsDataUpdatedSuccessfully() {
        webService.resetFlagIsDataUpdatedSuccessfully()
    }

    fun getAllCryptocurrencies(): LiveData<List<EntityCryptocurrencyTop100ByMarketCap>> {
        return database!!.userDao()!!.getAllCryptocurrencies()
    }

    private suspend fun deleteAllCryptocurrencies() {
        database!!.userDao()!!.deleteAllCryptocurrencies()
    }

    fun getAllCryptocurrenciesPrices(): LiveData<List<EntityCryptocurrencyPrice>> {
        return database!!.userDao().getAllCryptocurrenciesPrices()
    }

    private suspend fun deleteAllCryptocurrenciesPrices() {
        database!!.userDao().deleteAllCryptocurrenciesPrices()
    }

    private suspend fun deletePricesOfGivenCryptocurrency(cryptocurrencyId: String) {
        database!!.userDao().deletePricesOfGivenCryptocurrency(cryptocurrencyId)
    }

    private suspend fun addCryptocurrencyPrice(entity: EntityCryptocurrencyPrice) {
        database!!.userDao()!!.deletePricesOfGivenCryptocurrency(entity.cryptocurrencyId)
        database!!.userDao()!!.addCryptocurrencyPrice(entity)
    }

    private suspend fun addCryptocurrencyInfoInTimeRange(entity: EntityCryptocurrencyInfoInTimeRange) {
        if (entity.daysCount > 0)
            database!!.userDao()!!.deleteAllCryptocurrenciesInfoInGivenDaysCount(
                entity.cryptocurrencyId,
                entity.daysCount
            )
        database!!.userDao()!!.addCryptocurrencyInfoInTimeRange(entity)
    }

    fun getAllCryptocurrenciesInfo(): LiveData<List<EntityCryptocurrencyInfoInTimeRange>> {
        return database!!.userDao()!!.getAllCryptocurrenciesInfoInTimeRange()
    }

    private suspend fun deleteAllCryptocurrenciesInfo() {
        database!!.userDao()!!.deleteAllCryptocurrenciesInfo()
    }

    fun getCryptocurrencyInfoInTimeRange(): LiveData<List<EntityCryptocurrencyInfoInTimeRange>> {
        return database!!.userDao()!!.getAllCryptocurrenciesInfoInTimeRange()
    }

    private suspend fun deleteCryptocurrencyInfoContainsGivenDay(
        cryptocurrencyId: String,
        unixTimeDay: Long,
    ) {
        database!!.userDao()!!.deleteCryptocurrencyInfoContainsGivenDay(
            cryptocurrencyId,
            unixTimeDay
        )
    }
    /////////////////////////////////////////////////////////////////////////////////////
    //methods for request update data in database through webservice
    /////////////////////////////////////////////////////////////////////////////////////

    suspend fun updatePriceData(
        currencySymbol: String,
        vs_currency: String,
        archival: Boolean = false,
        archivalDate: Date? = null,
    ) {
        if (!archival || archivalDate != null) {
            val liveData = when (archival) {
                true -> webService.requestArchivalPriceData(currencySymbol, archivalDate!!)
                else -> webService.requestActualPriceData(currencySymbol, vs_currency)
            }
            if (!liveData.hasActiveObservers())
                liveData.observeForever { response ->
                    if (response?.actualPrice != null) //value check
                    {
                        runBlocking {
                            addCryptocurrencyPrice(
                                EntityCryptocurrencyPrice(
                                    index = 0, //auto-increment, no need to specify manually
                                    cryptocurrencyId = response.currencySymbol,
                                    price = response.actualPrice!!.toDouble(),
                                    date = Date(response.date.time * 1000),
                                )
                            )
                        }
                    }
                }
        }
    }

    suspend fun updateCryptocurrenciesList() { //TODO: modify
        val liveData = webService.requestCryptocurrenciesList()
        if (!liveData.hasActiveObservers())
            liveData.observeForever() { response ->
                runBlocking {
                    deleteAllCryptocurrencies()
                }
                response.forEach { row ->
                    this.addCryptocurrencyToTop100ByMarketCapTable(
                        EntityCryptocurrencyTop100ByMarketCap(
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

    suspend fun updateCryptocurrenciesInfoInDateRange(
        currencySymbol: String, vs_currency: String, unixtimeFrom: Long,
        unixTimeTo: Long,
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
                            )
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

                    runBlocking {
                        addCryptocurrencyInfoInTimeRange(
                            EntityCryptocurrencyInfoInTimeRange(
                                index = 0, //auto-increment, no need to specify manually
                                cryptocurrencyId = response.currencySymbol,
                                timeRangeFrom = response.unixtimeFrom,
                                timeRangeTo = response.unixTimeTo,
                                daysCount = (((response.unixTimeTo - response.unixtimeFrom) / 3600 / 24).toInt()),
                                prices = prices,
                                market_caps = marketCaps,
                                total_volumes = totalVolume,
                                updateDate = Date()
                            )
                        )
                    }
                }
            }
    }

    suspend fun deleteAllCryptocurrenciesInfoInGivenDaysCount(
        cryptocurrencyId: String,
        daysCount: Int,
    ) {
        database!!.userDao()!!
            .deleteAllCryptocurrenciesInfoInGivenDaysCount(cryptocurrencyId, daysCount)
    }

    fun getCryptocurrenciesInfoInTimeRange(
        cryptocurrencyId: String,
        daysCount: Int,
    ): LiveData<List<EntityCryptocurrencyInfoInTimeRange>> {
        return database!!.userDao()!!
            .getInfoOfCryptocurrencyInTimeRange(cryptocurrencyId, daysCount)
    }

    fun getApiErrorCodeLiveData() = webService.mldErrorCode
}