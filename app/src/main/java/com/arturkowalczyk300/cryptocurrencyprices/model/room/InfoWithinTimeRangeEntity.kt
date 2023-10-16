package com.arturkowalczyk300.cryptocurrencyprices.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.*

@Entity(tableName = "cryptocurrencies_info_in_time_range")
@TypeConverters(ParametersAtTimeConverterDb::class, DateConverterDb::class, OhlcDataEntityConverterDb::class)
data class InfoWithinTimeRangeEntity(
    @PrimaryKey(autoGenerate = true) val index: Int,
    val cryptocurrencyId: String,
    val timeRangeFrom: Long, //unit time format
    val timeRangeTo: Long,
    val daysCount: Int = 0,
    val market_caps: ParametersAtTime? = null,
    val prices: ParametersAtTime? = null,
    val total_volumes: ParametersAtTime? = null,
    val updateDate: Date,
    var ohlcData: List<OhlcDataEntity>? = null
)