package com.arturkowalczyk300.cryptocurrencyprices.Model.Room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.*

@Entity(tableName = "cryptocurrencies_prices")
@TypeConverters(ListOfCryptocurrencyStatValuesWithTimeConverter::class, DateConverter::class)
data class EntityCryptocurrencyPrice(
    @PrimaryKey(autoGenerate = true) val index: Int,
    val cryptocurrencyId: String,
    val price: Double,
    val date: Date
)