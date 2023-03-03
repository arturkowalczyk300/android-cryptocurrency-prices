package com.arturkowalczyk300.cryptocurrencyprices.model

import android.app.Application
import androidx.lifecycle.LiveData
import com.arturkowalczyk300.cryptocurrencyprices.model.room.*
import com.arturkowalczyk300.cryptocurrencyprices.model.webAccess.CryptocurrencyPricesWebService
import kotlinx.coroutines.runBlocking
import java.util.*

class Repository(application: Application) {

    private val database: CryptocurrencyPricesDatabase? =
        CryptocurrencyPricesDatabase.getDatabase(application)
    private val webService: CryptocurrencyPricesWebService = CryptocurrencyPricesWebService()

    private fun addCryptocurrency(entity: CryptocurrencyEntity) {
        runBlocking {
            database!!.userDao()!!.addCryptocurrency(entity)
        }
    }

    fun getAllCryptocurrencies(): LiveData<List<CryptocurrencyEntity>> {
        return database!!.userDao()!!.getAllCryptocurrencies()
    }

    private suspend fun deleteAllCryptocurrencies() {
        database!!.userDao()!!.deleteAllCryptocurrencies()
    }

    fun getAllCryptocurrenciesPrices(): LiveData<List<PriceEntity>> {
        return database!!.userDao().getAllCryptocurrenciesPrices()
    }

    private suspend fun deleteAllCryptocurrenciesPrices() {
        database!!.userDao().deleteAllCryptocurrenciesPrices()
    }

    private suspend fun deletePricesOfGivenCryptocurrency(cryptocurrencyId: String) {
        database!!.userDao().deletePricesOfGivenCryptocurrency(cryptocurrencyId)
    }

    private suspend fun addCryptocurrencyPrice(entity: PriceEntity) {
        database!!.userDao()!!.deletePricesOfGivenCryptocurrency(entity.cryptocurrencyId)
        database!!.userDao()!!.addCryptocurrencyPrice(entity)
    }

    private suspend fun addCryptocurrencyInfoWithinTimeRange(entity: InfoWithinTimeRangeEntity) {
        if (entity.daysCount > 0)
            database!!.userDao()!!.deleteAllCryptocurrenciesInfoInGivenDaysCount(
                entity.cryptocurrencyId,
                entity.daysCount
            )

        database!!.userDao()!!.addCryptocurrencyInfoWithinTimeRange(entity)
    }

    fun getAllCryptocurrenciesInfo(): LiveData<List<InfoWithinTimeRangeEntity>> {
        return database!!.userDao()!!.getAllCryptocurrenciesInfoWithinTimeRange()
    }

    private suspend fun deleteAllCryptocurrenciesInfo() {
        database!!.userDao()!!.deleteAllCryptocurrenciesInfo()
    }

    fun getCryptocurrencyInfoWithinTimeRange(): LiveData<List<InfoWithinTimeRangeEntity>> {
        return database!!.userDao()!!.getAllCryptocurrenciesInfoWithinTimeRange()
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
                                PriceEntity(
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

    suspend fun updateCryptocurrenciesList() {
        val liveData = webService.requestCryptocurrenciesList()
        if (!liveData.hasActiveObservers())
            liveData.observeForever() { response ->
                runBlocking {
                    deleteAllCryptocurrencies()
                }
                response.forEach { row ->
                    this.addCryptocurrency(
                        CryptocurrencyEntity(
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

    suspend fun updateCryptocurrenciesInfoWithinDateRange(
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

                    val list: List<ParameterAtTime> =
                        response.archivalPrices!!.map {
                            ParameterAtTime(
                                it[0].toLong(),
                                it[1]
                            )
                        }
                    val prices = ParametersAtTime(list)

                    val totalVolume =
                        ParametersAtTime(
                            response.totalVolumes!!.map {
                                ParameterAtTime(
                                    it[0].toLong(),
                                    it[1]
                                )
                            })

                    val marketCaps =
                        ParametersAtTime(
                            response.marketCaps!!.map {
                                ParameterAtTime(
                                    it[0].toLong(),
                                    it[1]
                                )
                            })

                    runBlocking {
                        addCryptocurrencyInfoWithinTimeRange(
                            InfoWithinTimeRangeEntity(
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

    fun getCryptocurrenciesInfoWithinTimeRange(
        cryptocurrencyId: String,
        daysCount: Int,
    ): LiveData<List<InfoWithinTimeRangeEntity>> {
        return database!!.userDao()!!
            .getInfoOfCryptocurrencyWithinTimeRange(cryptocurrencyId, daysCount)
    }

    fun getApiErrorCodeLiveData() = webService.mldErrorCode
}