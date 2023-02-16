package com.arturkowalczyk300.cryptocurrencyprices.Model.Room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

@Dao
interface CryptocurrencyPricesDao {
    //********************************************************************
    // table top100_cryptocurrencies
    //********************************************************************

    @Insert(onConflict = REPLACE)
    fun addCryptocurrencyToTop100ByMarketCapTable(entity: EntityCryptocurrencyTop100ByMarketCap)

    @Query("SELECT * FROM top100_cryptocurrencies ORDER BY market_cap_rank ASC")
    fun getAllCryptocurrencies(): LiveData<List<EntityCryptocurrencyTop100ByMarketCap>>

    @Query("DELETE FROM top100_cryptocurrencies")
    fun deleteAllCryptocurrencies()

    //********************************************************************
    // table cryptocurrencies prices (just simple price)
    //********************************************************************
    @Insert(onConflict = REPLACE)
    fun addCryptocurrencyPrice(entity: EntityCryptocurrencyPrice)

    @Query(
        "SELECT * FROM cryptocurrencies_prices"
    )
    fun getAllCryptocurrenciesPrices(): LiveData<List<EntityCryptocurrencyPrice>>

    @Query(
        "DELETE FROM cryptocurrencies_prices"
    )
    fun deleteAllCryptocurrenciesPrices()

    @Query(
        "DELETE FROM cryptocurrencies_prices WHERE cryptocurrencyId=:cryptocurrencyId"
    )
    fun deletePricesOfGivenCryptocurrency(cryptocurrencyId: String)

    //********************************************************************
    // table cryptocurrencies info in time range (price also)
    //********************************************************************

    @Insert(onConflict = REPLACE)
    fun addCryptocurrencyInfoInTimeRange(entity: EntityCryptocurrencyInfoInTimeRange)

    @Query(
        "SELECT * FROM cryptocurrencies_info_in_time_range "
    )
    fun getAllCryptocurrenciesInfoInTimeRange(): LiveData<List<EntityCryptocurrencyInfoInTimeRange>>

    @Query(
        "SELECT * FROM cryptocurrencies_info_in_time_range " +
                "WHERE cryptocurrencyId=:cryptocurrencyId " +
                "AND daysCount=:daysCount"
    )
    fun getInfoOfCryptocurrencyInTimeRange(
        cryptocurrencyId: String,
        daysCount: Int,
    ): LiveData<List<EntityCryptocurrencyInfoInTimeRange>>

    @Query("DELETE FROM cryptocurrencies_info_in_time_range")
    fun deleteAllCryptocurrenciesInfo()

    //get by a) cryptocurrency id, b) time range
    @Query(
        "DELETE FROM cryptocurrencies_info_in_time_range " +
                "WHERE cryptocurrencyId=:cryptocurrencyId " +
                "AND timeRangeFrom <= :unixTimeDay " +
                "AND timeRangeTo >= :unixTimeDay"
    )
    fun deleteCryptocurrencyInfoContainsGivenDay(
        cryptocurrencyId: String,
        unixTimeDay: Long,
    )

    //new requests
    @Query(
        "DELETE FROM cryptocurrencies_info_in_time_range " +
                "WHERE cryptocurrencyId=:cryptocurrencyId " +
                "AND daysCount=:daysCount"
    )
    fun deleteAllCryptocurrenciesInfoInGivenDaysCount(
        cryptocurrencyId: String,
        daysCount: Int,
    )
}