package com.arturkowalczyk300.cryptocurrencyprices.model.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.arturkowalczyk300.cryptocurrencyprices.other.Constants

@Database(
    entities = [CryptocurrencyEntity::class,
        InfoWithinTimeRangeEntity::class,
        PriceEntity::class,
        PriceAlertEntity::class],
    version = 12
)
abstract class CryptocurrencyPricesDatabase() : RoomDatabase() {
    abstract fun userDao(): Dao
}
