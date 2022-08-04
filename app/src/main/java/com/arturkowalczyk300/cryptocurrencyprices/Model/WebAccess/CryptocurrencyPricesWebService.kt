package com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.arturkowalczyk300.cryptocurrencyprices.Model.REQUEST_CRYPTOCURRENCIES_LIST_FAILURE
import com.arturkowalczyk300.cryptocurrencyprices.Model.REQUEST_PRICE_DATA_FAILURE
import com.arturkowalczyk300.cryptocurrencyprices.Model.REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE
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
    private val sdf = SimpleDateFormat("dd.MM.yyyy")

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

    var mldErrorCode: MutableLiveData<Pair<Boolean, Int>> = MutableLiveData()

    var mldRequestWithResponse: MutableLiveData<RequestWithResponse> = MutableLiveData(
        RequestWithResponse()
    )

    var mldPriceHistory: MutableLiveData<List<List<Double>>?> = MutableLiveData(listOf())

    var cryptocurrenciesListSorted: MutableLiveData<ArrayList<CryptocurrencyPriceFromListApi>> =
        MutableLiveData()

    init {
        when (CryptocurrencyPricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()) {
            null -> Log.e("myApp", "ApiHandleInstance is null")
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
                if (response.body() != null) {
                    if (waitingForResponse) {
                        mldRequestWithResponse?.value?.entity = response.body()
                        mldRequestWithResponse?.value?.flagDataSet = true
                        mldRequestWithResponse.value =
                            mldRequestWithResponse.value //notify data changed
                    }
                } else {
                    mldErrorCode.value = Pair(true, REQUEST_PRICE_DATA_FAILURE)
                }
                waitingForResponse = false
            }

            override fun onFailure(call: Call<CryptocurrencyPricesEntityApi>, t: Throwable) {
                mldErrorCode.value = Pair(true, REQUEST_PRICE_DATA_FAILURE)

                Log.e(
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
                    mldErrorCode.value = Pair(true, REQUEST_CRYPTOCURRENCIES_LIST_FAILURE)
                    Log.e(
                        "myApp",
                        "invalid response! [size=${responseBody?.size}, code=${response.code()}]"
                    )
                }
            }

            override fun onFailure(call: Call<List<CryptocurrencyPriceFromListApi>>, t: Throwable) {
                mldErrorCode.value = Pair(true, REQUEST_CRYPTOCURRENCIES_LIST_FAILURE)
                Log.e("myApp", "onFailure")
            }
        })
        return cryptocurrenciesListSorted
    }

    fun requestPriceHistoryForDateRange(
        currencySymbol: String,
        vs_currency: String,
        unixtimeFrom: Long,
        unixTimeTo: Long
    ): MutableLiveData<List<List<Double>>?> {

        val response: Call<CryptocurrencyPriceHistoryFromApi>? =
            CryptocurrencyPricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()
                ?.getHistoryOfPriceForDateRange(
                    currencySymbol,
                    vs_currency,
                    unixtimeFrom,
                    unixTimeTo
                )


        response?.enqueue(object : Callback<CryptocurrencyPriceHistoryFromApi> {
            override fun onResponse(
                call: Call<CryptocurrencyPriceHistoryFromApi>,
                response: Response<CryptocurrencyPriceHistoryFromApi>
            ) {
                if (response.body() != null)
                    mldPriceHistory.value = response.body()?.prices
                else {
                    mldPriceHistory.value = null
                    mldErrorCode.value = Pair(true, REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE)
                }
            }

            override fun onFailure(call: Call<CryptocurrencyPriceHistoryFromApi>, t: Throwable) {
                mldPriceHistory.value = null

                mldErrorCode.value = Pair(true, REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE)

                Log.e(
                    "myApp", "onFailure, url:${call.request().url().toString()}" +
                            "\n exc: ${t.stackTraceToString()}"
                )
            }
        })

        return mldPriceHistory
    }

}
