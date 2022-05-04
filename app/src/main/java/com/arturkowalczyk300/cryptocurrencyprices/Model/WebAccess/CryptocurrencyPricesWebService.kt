package com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess

import android.util.Log
import androidx.lifecycle.MutableLiveData
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.StringBuilder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class RequestWithResponse(
    var currencySymbol: String = "",
    var date: Date = Date(0),
    var entity: CryptocurrencyPricesEntityApi? = null
) {
    val sdf = SimpleDateFormat("dd.MM.yyyy")

    fun formatPrice(price: Double): String {
        val df = DecimalFormat("#.###")
        return df.format(price)
    }

    fun getFormattedDate(): String {
        return sdf.format(date)
    }

    override fun toString(): String {
        val sb: StringBuilder = StringBuilder()
        sb.append("== Request ==\n")
        sb.append("CurrencySymbol: ${currencySymbol}\n")
        sb.append("Date: ${getFormattedDate()}\n")
        sb.append("== Response ==\n")
        sb.append("Price data:\n")
        sb.append("id: ${entity?.id}\n")
        sb.append("symbol: ${entity?.symbol}\n")
        sb.append("name: ${entity?.name}\n")
        sb.append("price: ${entity?.market_data?.current_price?.usd}\n")

        return sb.toString()

    }
}


class CryptocurrencyPricesWebService {
    var waitingForResponse: Boolean = false

    var mldRequestWithResponse: MutableLiveData<RequestWithResponse> = MutableLiveData(
        RequestWithResponse()
    )

    init {
        when (CryptocurrencyPricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()) {
            null -> Log.v("myApp", "ApiHandleInstance is null")
            else -> {
                Log.v("myApp", "ApiHandleInstance is OK!")
            }
        }
    }

    public fun requestPriceData(
        currencySymbol: String,
        date: Date
    ): MutableLiveData<RequestWithResponse> {
        mldRequestWithResponse?.value?.date = date
        mldRequestWithResponse?.value?.currencySymbol = currencySymbol
        mldRequestWithResponse?.value?.entity = null //test line
        waitingForResponse = true

        val sdf = SimpleDateFormat("dd-MM-yyyy")
        val formattedDate = sdf.format(date)

        val response: Call<CryptocurrencyPricesEntityApi>? =
            CryptocurrencyPricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()
                ?.getPrice(currencySymbol, formattedDate).also { Log.v("myApp", "getPrice") }


        response?.enqueue(object : Callback<CryptocurrencyPricesEntityApi> {
            override fun onResponse(
                call: Call<CryptocurrencyPricesEntityApi>,
                response: Response<CryptocurrencyPricesEntityApi>
            ) {
                Log.v("myApp", "response")
                if (waitingForResponse) {
                    mldRequestWithResponse?.value?.entity = response.body()
                    mldRequestWithResponse.value =
                        mldRequestWithResponse.value //notify data changed
                }
                waitingForResponse = false
            }

            override fun onFailure(call: Call<CryptocurrencyPricesEntityApi>, t: Throwable) {
                Log.v(
                    "myApp", "onFailure, url:${call.request().url().toString()}" +
                            "\n exc: ${t.stackTraceToString()}"
                )
            }
        })
        return mldRequestWithResponse
    }
}
