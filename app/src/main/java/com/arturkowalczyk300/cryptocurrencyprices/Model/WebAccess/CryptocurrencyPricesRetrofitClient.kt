package com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object CryptocurrencyPricesRetrofitClient {
    private var okHttpClientInstance: OkHttpClient? = null
    private var retrofitInstance: Retrofit? = null
    private var cryptocurrencyPricesApiHandle: CryptocurrencyPricesApiHandle? = null
    val baseUrlAddress: String = "https://api.coingecko.com/"

    fun getCryptocurrencyPricesApiHandleInstance(): CryptocurrencyPricesApiHandle? {

        try {
            okHttpClientInstance =
                okHttpClientInstance ?: OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(2, TimeUnit.SECONDS)
                    .writeTimeout(2, TimeUnit.SECONDS)
                    .build()

            retrofitInstance = retrofitInstance ?: Retrofit.Builder()
                .baseUrl(baseUrlAddress)
                .client(okHttpClientInstance)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            cryptocurrencyPricesApiHandle =
                cryptocurrencyPricesApiHandle ?: retrofitInstance?.create(
                    CryptocurrencyPricesApiHandle::class.java
                )


            okHttpClientInstance!!
            retrofitInstance!!
            cryptocurrencyPricesApiHandle!!
        } catch (exc: Exception) {
            Log.e("myApp", exc.toString())
            return null
        }

        return cryptocurrencyPricesApiHandle
    }

    fun getRetrofitInstance(): Retrofit?{
        if(retrofitInstance==null)
            getCryptocurrencyPricesApiHandleInstance()
        return retrofitInstance
    }
}