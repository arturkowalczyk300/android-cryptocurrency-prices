package com.arturkowalczyk300.cryptocurrencyprices.model.webAccess

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object PricesRetrofitClient {
    private var okHttpClientInstance: OkHttpClient? = null
    private var retrofitInstance: Retrofit? = null
    private var pricesApiHandle: PricesApiHandle? = null
    val baseUrlAddress: String = "https://api.coingecko.com/"

    fun getCryptocurrencyPricesApiHandleInstance(): PricesApiHandle? {

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

            pricesApiHandle =
                pricesApiHandle ?: retrofitInstance?.create(
                    PricesApiHandle::class.java
                )


            okHttpClientInstance!!
            retrofitInstance!!
            pricesApiHandle!!
        } catch (exc: Exception) {
            Log.e("myApp", "getCryptocurrencyPricesApiHandleInstance, $exc")
            return null
        }

        return pricesApiHandle
    }

    fun getRetrofitInstance(): Retrofit?{
        if(retrofitInstance==null)
            getCryptocurrencyPricesApiHandleInstance()
        return retrofitInstance
    }
}