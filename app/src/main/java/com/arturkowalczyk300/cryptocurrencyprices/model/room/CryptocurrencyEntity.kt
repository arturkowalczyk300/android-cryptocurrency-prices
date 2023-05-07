package com.arturkowalczyk300.cryptocurrencyprices.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.*

@Entity(tableName = "cryptocurrencies")
@TypeConverters(DateConverterDb::class)
data class CryptocurrencyEntity(
    @PrimaryKey val market_cap_rank: Int,
    val cryptocurrencyId: String,
    val name: String,
    val symbol: String,
    val marketCap: Long,
    val currentPrice: Double,
    val updateDate: Date,
)