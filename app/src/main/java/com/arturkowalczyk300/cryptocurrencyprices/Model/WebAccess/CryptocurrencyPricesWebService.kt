package com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess

import android.util.Log
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.StringBuilder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RequestWithResponse(
    var currencySymbol: String = "",
    var date: Date = Date(0),
    var entity: CryptocurrencyPricesEntityApi? = null,
    var flagDataSet: Boolean = false
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
    var cryptocurrenciesListSorted: MutableLiveData<ArrayList<CryptocurrencyPriceFromListApi>> =
        MutableLiveData()

    init {
        when (CryptocurrencyPricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()) {
            null -> Log.v("myApp", "ApiHandleInstance is null")
            else -> {
            }
        }
    }

    fun requestPriceData(
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
                ?.getPrice(currencySymbol, formattedDate)


        response?.enqueue(object : Callback<CryptocurrencyPricesEntityApi> {
            override fun onResponse(
                call: Call<CryptocurrencyPricesEntityApi>,
                response: Response<CryptocurrencyPricesEntityApi>
            ) {
                if (waitingForResponse) {
                    mldRequestWithResponse?.value?.entity = response.body()
                    mldRequestWithResponse?.value?.flagDataSet = true
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

    fun requestCryptocurrenciesList(): MutableLiveData<ArrayList<CryptocurrencyPriceFromListApi>> {
        val listResponse: Call<List<CryptocurrencyPriceFromListApi>>? =
            CryptocurrencyPricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()
                ?.getListOfCryptocurrencies("USD", "market_cap_desc", 100, 1, false)

        listResponse?.enqueue(object : Callback<List<CryptocurrencyPriceFromListApi>> {
            override fun onResponse(
                call: Call<List<CryptocurrencyPriceFromListApi>>,
                response: Response<List<CryptocurrencyPriceFromListApi>>
            ) {
                val responseBody = response.body()
                if (response.body() != null && response.code() != 404) { //valid response
                    cryptocurrenciesListSorted.value = ArrayList(response.body())
                } else {
                    Log.v(
                        "myApp",
                        "invalid response! [size=${responseBody?.size}, code=${response.code()}]"
                    )
                }
            }

            override fun onFailure(call: Call<List<CryptocurrencyPriceFromListApi>>, t: Throwable) {
                Log.v("myApp", "onFailure")
            }
        })
        return cryptocurrenciesListSorted
    }
}
