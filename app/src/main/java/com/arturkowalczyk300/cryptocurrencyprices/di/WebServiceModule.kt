package com.arturkowalczyk300.cryptocurrencyprices.di

import com.arturkowalczyk300.cryptocurrencyprices.model.webAccess.CryptocurrencyPricesWebService
import com.arturkowalczyk300.cryptocurrencyprices.model.webAccess.PricesApiHandle
import com.arturkowalczyk300.cryptocurrencyprices.other.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebServiceModule {
    @Provides
    @Singleton
    fun provideRetrofitInstance():Retrofit{
        val okHttpClientInstance = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClientInstance)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providePricesApiHandle(
        retrofitInstance: Retrofit
    ): PricesApiHandle {


        return retrofitInstance.create(
            PricesApiHandle::class.java
        )
    }

    @Provides
    @Singleton
    fun provideWebService(
        api: PricesApiHandle,
    ): CryptocurrencyPricesWebService {
        return CryptocurrencyPricesWebService(api)
    }
}