package com.arturkowalczyk300.cryptocurrencyprices.Model.Room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.*

@Entity(tableName = "cryptocurrencies_price_history")
@TypeConverters(ListOfCryptocurrencyStatValuesWithTimeConverter::class, DateConverter::class)
data class EntityCryptocurrenciesHistoricalPrices(
    @PrimaryKey(autoGenerate = true) val index: Int,
    val cryptocurrencyId: String,
    val timeRangeFrom: Long, //unit time format
    val timeRangeTo: Long,
    val daysCount: Int = 0,
    val market_caps: ListOfCryptocurrencyStatValuesWithTime? = null,
    val prices: ListOfCryptocurrencyStatValuesWithTime,
    val total_volumes: ListOfCryptocurrencyStatValuesWithTime? = null,
    val updateDate: Date
)