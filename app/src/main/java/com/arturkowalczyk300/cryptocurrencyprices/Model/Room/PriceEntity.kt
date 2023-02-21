package com.arturkowalczyk300.cryptocurrencyprices.Model.Room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.*

@Entity(tableName = "cryptocurrencies_prices")
@TypeConverters(ParametersAtTimeConverterDb::class, DateConverterDb::class)
data class PriceEntity(
    @PrimaryKey(autoGenerate = true) val index: Int,
    val cryptocurrencyId: String,
    val price: Double,
    val date: Date
)