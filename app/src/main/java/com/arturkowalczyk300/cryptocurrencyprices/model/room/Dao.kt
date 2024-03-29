package com.arturkowalczyk300.cryptocurrencyprices.model.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import retrofit2.http.GET

@Dao
interface Dao {
    @Insert(onConflict = REPLACE)
    suspend fun addCryptocurrency(entity: CryptocurrencyEntity)

    @Query("SELECT * FROM cryptocurrencies ORDER BY market_cap_rank ASC")
    fun getAllCryptocurrencies(): LiveData<List<CryptocurrencyEntity>>

    @Query("DELETE FROM cryptocurrencies")
    suspend fun deleteAllCryptocurrencies()

    @Insert(onConflict = REPLACE)
    suspend fun addCryptocurrencyPrice(entity: PriceEntity)

    @Query(
        "SELECT * FROM cryptocurrencies_prices"
    )
    fun getAllCryptocurrenciesPrices(): LiveData<List<PriceEntity>>

    @Query(
        "DELETE FROM cryptocurrencies_prices"
    )
    suspend fun deleteAllCryptocurrenciesPrices()

    @Query(
        "DELETE FROM cryptocurrencies_prices WHERE cryptocurrencyId=:cryptocurrencyId"
    )
    suspend fun deletePricesOfGivenCryptocurrency(cryptocurrencyId: String)

    @Insert(onConflict = REPLACE)
    suspend fun addCryptocurrencyInfoWithinTimeRange(entity: InfoWithinTimeRangeEntity)

    @Query(
        "SELECT * FROM cryptocurrencies_info_in_time_range "
    )
    fun getAllCryptocurrenciesInfoWithinTimeRange(): LiveData<List<InfoWithinTimeRangeEntity>>

    @Query(
        "SELECT * FROM cryptocurrencies_info_in_time_range " +
                "WHERE cryptocurrencyId=:cryptocurrencyId " +
                "AND daysCount=:daysCount"
    )
    fun getInfoOfCryptocurrencyWithinTimeRange(
        cryptocurrencyId: String,
        daysCount: Int,
    ): LiveData<List<InfoWithinTimeRangeEntity>>

    @Query("DELETE FROM cryptocurrencies_info_in_time_range")
    suspend fun deleteAllCryptocurrenciesInfo()

    @Query(
        "DELETE FROM cryptocurrencies_info_in_time_range " +
                "WHERE cryptocurrencyId=:cryptocurrencyId " +
                "AND timeRangeFrom <= :unixTimeDay " +
                "AND timeRangeTo >= :unixTimeDay"
    )
    suspend fun deleteCryptocurrencyInfoContainsGivenDay(
        cryptocurrencyId: String,
        unixTimeDay: Long,
    )

    @Query(
        "DELETE FROM cryptocurrencies_info_in_time_range " +
                "WHERE cryptocurrencyId=:cryptocurrencyId " +
                "AND daysCount=:daysCount"
    )
    suspend fun deleteAllCryptocurrenciesInfoInGivenDaysCount(
        cryptocurrencyId: String,
        daysCount: Int,
    )
    
    @Query("SELECT * FROM prices_alerts")
    fun getPricesAlerts(): LiveData<List<PriceAlertEntity>>

    @Insert(onConflict = REPLACE)
    suspend fun addPriceAlert(alertEntity: PriceAlertEntity)

    @Delete
    suspend fun deletePriceAlert(entity: PriceAlertEntity)

    @Query("DELETE FROM prices_alerts")
    suspend fun deleteAllPricesAlerts()
}