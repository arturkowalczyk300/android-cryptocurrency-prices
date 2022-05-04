package com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.StringBuilder
import java.util.*

class CryptocurrencyPricesWebService {
//    val retrofitClient: CryptocurrencyPricesRetrofitClient.

    init {
        Log.v("myApp", "webService init")
        when (CryptocurrencyPricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()) {
            null -> Log.v("myApp", "ApiHandleInstance is null")
            else -> {
                Log.v("myApp", "ApiHandleInstance is OK!")
                requestPriceData("", Date(2342))
            }
        }
    }

    public fun requestPriceData(currencyName: String, date: Date) {
        val response: Call<CryptocurrencyPricesEntityApi>? =
            CryptocurrencyPricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()
                ?.getPrice(
                    "bitcoin",
                    "01-05-2022"
                )

        response?.enqueue(object : Callback<CryptocurrencyPricesEntityApi> {
            override fun onResponse(
                call: Call<CryptocurrencyPricesEntityApi>,
                response: Response<CryptocurrencyPricesEntityApi>
            ) {
                Log.v("myApp", "onResponse, url:${call.request().url().toString()}")
                val sb: StringBuilder = StringBuilder()
                sb.append("Price data:\n")
                sb.append("id: ${response.body()?.id}\n")
                sb.append("symbol: ${response.body()?.symbol}\n")
                sb.append("name: ${response.body()?.name}\n")
                sb.append("price: ${response.body()?.market_data?.current_price?.usd}\n")
                Log.v("myApp", sb.toString())
            }

            override fun onFailure(call: Call<CryptocurrencyPricesEntityApi>, t: Throwable) {
                Log.v(
                    "myApp", "onFailure, url:${call.request().url().toString()}" +
                            "\n exc: ${t.stackTraceToString()}"
                )
            }
        })
    }
}
