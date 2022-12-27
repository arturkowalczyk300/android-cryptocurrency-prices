package com.arturkowalczyk300.cryptocurrencyprices.Model.Room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.*

@Entity(tableName = "top100_cryptocurrencies")
@TypeConverters(DateConverter::class)
data class EntityCryptocurrenciesTop100ByMarketCap(
    @PrimaryKey val market_cap_rank: Int,
    val cryptocurrencyId: String,
    val name: String,
    val marketCap: Long,
    val currentPrice: Double,
    val updateDate: Date,
)