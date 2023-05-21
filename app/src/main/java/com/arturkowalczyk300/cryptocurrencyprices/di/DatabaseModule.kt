package com.arturkowalczyk300.cryptocurrencyprices.di

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import com.arturkowalczyk300.cryptocurrencyprices.model.room.CryptocurrencyPricesDatabase
import com.arturkowalczyk300.cryptocurrencyprices.model.room.Dao
import com.arturkowalczyk300.cryptocurrencyprices.other.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        appContext: Application
    ): CryptocurrencyPricesDatabase{
        return  Room.databaseBuilder(
            appContext.applicationContext,
            CryptocurrencyPricesDatabase::class.java,
            Constants.DB_NAME
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao(
        database: CryptocurrencyPricesDatabase
    ): Dao{
        return database.userDao()
    }
}