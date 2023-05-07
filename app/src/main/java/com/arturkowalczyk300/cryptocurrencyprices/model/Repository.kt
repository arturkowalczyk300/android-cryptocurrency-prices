package com.arturkowalczyk300.cryptocurrencyprices.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.arturkowalczyk300.cryptocurrencyprices.model.room.*
import com.arturkowalczyk300.cryptocurrencyprices.model.webAccess.CryptocurrencyPricesWebService
import kotlinx.coroutines.runBlocking
import java.util.*

class Repository(application: Application) {

    private var _chartDataObserved = false
    private val database: CryptocurrencyPricesDatabase? =
        CryptocurrencyPricesDatabase.getDatabase(application)
    private val webService: CryptocurrencyPricesWebService = CryptocurrencyPricesWebService()

    private var _cryptocurrencyChartData = MutableLiveData<InfoWithinTimeRangeEntity?>()
    private val cryptocurrencyChartData: LiveData<InfoWithinTimeRangeEntity?> =
        _cryptocurrencyChartData

    private var _lastChartDataFromDaoRef: LiveData<List<InfoWithinTimeRangeEntity>>? = null
    private var _lastChartDataFromDaoObserver: Observer<List<InfoWithinTimeRangeEntity>?>? = null

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

    fun getCryptocurrencyChartData(
        cryptocurrencyId: String? = null,
        daysCount: Int? = null,
    ): LiveData<InfoWithinTimeRangeEntity?> {
        if (cryptocurrencyId != null
            && daysCount != null
        ) { //update data, if parameters are given
            val observer: androidx.lifecycle.Observer<List<InfoWithinTimeRangeEntity>?> =
                object : androidx.lifecycle.Observer<List<InfoWithinTimeRangeEntity>?> {
                    override fun onChanged(currenciesChartData: List<InfoWithinTimeRangeEntity>?) {
                        if (currenciesChartData != null) {

                            _cryptocurrencyChartData.value = currenciesChartData
                                .filter { it.cryptocurrencyId == cryptocurrencyId && it.daysCount == daysCount }
                                .maxByOrNull { it.updateDate.time }
                        }
                    }
                }

            _lastChartDataFromDaoObserver?.let {
                removeChartDataObserver(_lastChartDataFromDaoObserver!!)
            }

            _lastChartDataFromDaoRef = database!!.userDao()!!
                .getInfoOfCryptocurrencyWithinTimeRange(
                    cryptocurrencyId,
                    daysCount
                ).also {
                    it.observeForever(observer)
                    _lastChartDataFromDaoObserver = observer
                }
        }

        return cryptocurrencyChartData
    }

    private fun removeChartDataObserver(observer: Observer<List<InfoWithinTimeRangeEntity>?>) {
        if (_lastChartDataFromDaoRef != null && _lastChartDataFromDaoRef!!.hasActiveObservers())
            _lastChartDataFromDaoRef!!.removeObserver(observer)
    }

    private suspend fun deleteAllCryptocurrenciesInfo() {
        database!!.userDao()!!.deleteAllCryptocurrenciesInfo()
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
                            name = row.id,
                            symbol = row.symbol,
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

    fun getApiErrorCodeLiveData() = webService.mldErrorCode

    fun addPriceAlert(entity: PriceAlertEntity) {
        runBlocking {
            database!!.userDao().addPriceAlert(entity).also {
            }
        }
    }

    fun deletePriceAlert(entity:PriceAlertEntity){
        runBlocking {
            database!!.userDao().deletePriceAlert(entity)
        }
    }

    fun deleteAllPriceAlerts(){
        runBlocking {
            database!!.userDao().deleteAllPricesAlerts()
        }
    }

    fun getPricesAlerts(): LiveData<List<PriceAlertEntity>> {
        return database!!.userDao().getPricesAlerts()
    }

    fun getActualPriceOfCryptocurrencySynchronously(cryptocurrencySymbol: String, vs_currency: String): Float{
        return webService.getActualPriceOfCryptocurrencySynchronously(cryptocurrencySymbol, vs_currency)
    }
}