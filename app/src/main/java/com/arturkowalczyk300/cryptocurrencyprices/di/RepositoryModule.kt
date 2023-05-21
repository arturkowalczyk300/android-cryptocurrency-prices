package com.arturkowalczyk300.cryptocurrencyprices.di

import android.app.Application
import com.arturkowalczyk300.cryptocurrencyprices.model.Repository
import com.arturkowalczyk300.cryptocurrencyprices.model.room.CryptocurrencyPricesDatabase
import com.arturkowalczyk300.cryptocurrencyprices.model.webAccess.CryptocurrencyPricesWebService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    fun provideRepository(
        app: Application,
        webService: CryptocurrencyPricesWebService,
        database: CryptocurrencyPricesDatabase,
    ): Repository {
        return Repository(app, webService, database)
    }
}