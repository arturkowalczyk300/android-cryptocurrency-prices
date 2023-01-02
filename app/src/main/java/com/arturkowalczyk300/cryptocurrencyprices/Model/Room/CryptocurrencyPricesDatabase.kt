package com.arturkowalczyk300.cryptocurrencyprices.Model.Room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EntityCryptocurrenciesTop100ByMarketCap::class,
        EntityCryptocurrenciesHistoricalPrices::class],
    version = 5
)
abstract class CryptocurrencyPricesDatabase() : RoomDatabase() {
    abstract fun userDao(): CryptocurrencyPricesDao

    companion object {
        private var databaseInstance: CryptocurrencyPricesDatabase? = null

        fun getDatabase(context: Context): CryptocurrencyPricesDatabase? {
            databaseInstance = databaseInstance ?: Room.databaseBuilder(
                context.applicationContext,
                CryptocurrencyPricesDatabase::class.java,
                "main_database"
            ).fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()

            return databaseInstance
        }
    }
}
