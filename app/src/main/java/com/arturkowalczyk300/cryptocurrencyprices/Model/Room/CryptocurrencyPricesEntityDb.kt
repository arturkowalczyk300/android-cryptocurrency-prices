package com.arturkowalczyk300.cryptocurrencyprices.Model.Room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.*

@Entity(tableName = "main_table")
@TypeConverters(DateConverter::class)
data class CryptocurrencyPricesEntityDb(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cryptocurrencyId: String,
    val date: Date,
    val priceUsd: Double
)
