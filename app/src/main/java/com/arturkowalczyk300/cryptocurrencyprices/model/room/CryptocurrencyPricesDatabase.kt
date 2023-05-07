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
    version = 11
)
abstract class CryptocurrencyPricesDatabase() : RoomDatabase() {
    abstract fun userDao(): Dao

    companion object {
        private var databaseInstance: CryptocurrencyPricesDatabase? = null

        fun getDatabase(context: Context): CryptocurrencyPricesDatabase? {
            databaseInstance = databaseInstance ?: Room.databaseBuilder(
                context.applicationContext,
                CryptocurrencyPricesDatabase::class.java,
                Constants.DB_NAME
            ).fallbackToDestructiveMigration()
                .build()

            return databaseInstance
        }
    }
}
