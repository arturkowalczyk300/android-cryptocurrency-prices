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
    fun addCryptocurrencyToTop100ByMarketCapTable(entity: EntityCryptocurrenciesTop100ByMarketCap)

    @Query("SELECT * FROM top100_cryptocurrencies ORDER BY market_cap_rank ASC")
    fun getAllCryptocurrencies(): LiveData<List<EntityCryptocurrenciesTop100ByMarketCap>>

    @Query("DELETE FROM top100_cryptocurrencies")
    fun deleteAllCryptocurrencies()

    //********************************************************************
    // table cryptocurrencies_price_history
    //********************************************************************

    @Insert(onConflict = REPLACE)
    fun addHistoricalPrice(entity: EntityCryptocurrenciesHistoricalPrices)

    @Query(
        "SELECT * FROM cryptocurrencies_price_history " +
                "WHERE total_volumes is NULL " + //single reads has those fields empty
                "AND market_caps is NULL"
    )
    fun getAllHistoricalPrices(): LiveData<List<EntityCryptocurrenciesHistoricalPrices>>

    @Query("DELETE FROM cryptocurrencies_price_history")
    fun deleteAllHistoricalPrices()

    //get by a) cryptocurrency id, b) time range
    @Query(
        "SELECT * FROM cryptocurrencies_price_history " +
                "WHERE market_caps is not NULL" +
                " AND total_volumes is not NULL"//single reads has those fields empty
    )
    fun getHistoricalPriceOfCryptocurrenciesWithTimeRange(
    ): LiveData<List<EntityCryptocurrenciesHistoricalPrices>>

    @Query(
        "DELETE FROM cryptocurrencies_price_history " +
                "WHERE cryptocurrencyId=:cryptocurrencyId " +
                "AND timeRangeFrom <= :unixTimeDay " +
                "AND timeRangeTo >= :unixTimeDay"
    )
    fun deleteHistoricalPriceOfCryptocurrencyContainsGivenDay(
        cryptocurrencyId: String,
        unixTimeDay: Long
    )

    //todo: limit max records for every cryptocurrency id, eg 3
}