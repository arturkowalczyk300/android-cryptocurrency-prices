package com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.arturkowalczyk300.cryptocurrencyprices.Model.REQUEST_CRYPTOCURRENCIES_LIST_FAILURE
import com.arturkowalczyk300.cryptocurrencyprices.Model.REQUEST_PRICE_DATA_FAILURE
import com.arturkowalczyk300.cryptocurrencyprices.Model.REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.ListOfCryptocurrencyStatValuesWithTime
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.StringBuilder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

open class RequestWithResponse(
    var currencySymbol: String = "",
    var date: Date = Date(0),
    var entity: CryptocurrencyPricesEntityApi? = null,
    var actualPrice: Float? = null,
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

class RequestWithResponseArchival(
    currencySymbol: String = "",
    date: Date = Date(0),
    val vs_currency: String,
    val unixtimeFrom: Long,
    val unixTimeTo: Long,
    var archivalPrices: List<List<Double>>? = null
) : RequestWithResponse(
    currencySymbol,
    date,
    null,
    null,
    false
) {
}


class CryptocurrencyPricesWebService {
    var waitingForResponse: Boolean = false

    var mldErrorCode: MutableLiveData<Pair<Boolean, Int>> = MutableLiveData()

    var mldActualRequestWithResponse: MutableLiveData<RequestWithResponse> = MutableLiveData(
        RequestWithResponse()
    )

    var mldArchivalRequestWithResponse: MutableLiveData<RequestWithResponse> = MutableLiveData(
        RequestWithResponse()
    )

    var mldPriceHistory: MutableLiveData<RequestWithResponseArchival?>? = MutableLiveData()

    var cryptocurrenciesListSorted: MutableLiveData<ArrayList<CryptocurrencyPriceFromListApi>> =
        MutableLiveData()

    init {
        when (CryptocurrencyPricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()) {
            null -> Log.e("myApp", "ApiHandleInstance is null")
            else -> {
            }
        }
    }

    fun requestActualPriceData(
        currencySymbol: String,
        vs_currency: String
    ): MutableLiveData<RequestWithResponse> {
        mldActualRequestWithResponse?.value?.date = Date()
        mldActualRequestWithResponse?.value?.currencySymbol = currencySymbol
        mldActualRequestWithResponse?.value?.entity = null //test line
        waitingForResponse = true

        val response: Call<ResponseBody>? =
            CryptocurrencyPricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()
                ?.getActualPrice(currencySymbol, vs_currency)


        response?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.body() != null) {
                    if (waitingForResponse) {
                        val bodyStr: ResponseBody = response.body()!!
                        val src = bodyStr.source().toString()
                        val regex = Regex("(\\d+.\\d+)")
                        val priceStr: String? = regex.find(src)?.groupValues?.get(0)
                        var price: Float? = null
                        if (priceStr != null)
                            price = priceStr.toFloat()

                        mldActualRequestWithResponse?.value?.actualPrice = price
                        mldActualRequestWithResponse?.value?.flagDataSet = true
                        mldActualRequestWithResponse.value =
                            mldActualRequestWithResponse.value //notify data changed
                    }
                } else {
                    mldErrorCode.value = Pair(true, REQUEST_PRICE_DATA_FAILURE)
                }
                waitingForResponse = false
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                mldErrorCode.value = Pair(true, REQUEST_PRICE_DATA_FAILURE)

                Log.e(
                    "myApp", "onFailure, url:${call.request().url().toString()}" +
                            "\n exc: ${t.stackTraceToString()}"
                )
            }
        })

        return mldActualRequestWithResponse
    }

    fun requestArchivalPriceData(
        currencySymbol: String,
        date: Date
    ): MutableLiveData<RequestWithResponse> {
        mldArchivalRequestWithResponse?.value?.date = date
        mldArchivalRequestWithResponse?.value?.currencySymbol = currencySymbol
        mldArchivalRequestWithResponse?.value?.entity = null //test line
        waitingForResponse = true

        val sdf = SimpleDateFormat("dd-MM-yyyy")
        val formattedDate = sdf.format(date)

        val response: Call<CryptocurrencyPricesEntityApi>? =
            CryptocurrencyPricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()
                ?.getArchivalPrice(currencySymbol, formattedDate)


        response?.enqueue(object : Callback<CryptocurrencyPricesEntityApi> {
            override fun onResponse(
                call: Call<CryptocurrencyPricesEntityApi>,
                response: Response<CryptocurrencyPricesEntityApi>
            ) {
                if (response.body() != null) {
                    if (waitingForResponse) {
                        mldArchivalRequestWithResponse?.value?.entity = response.body()
                        mldArchivalRequestWithResponse?.value?.flagDataSet = true
                        mldArchivalRequestWithResponse.value =
                            mldArchivalRequestWithResponse.value //notify data changed
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

        return mldArchivalRequestWithResponse
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
    ): MutableLiveData<RequestWithResponseArchival?> {

        mldPriceHistory!!.value = RequestWithResponseArchival(
            currencySymbol,
            Date(),
            vs_currency,
            unixtimeFrom,
            unixTimeTo
        )


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
                    mldPriceHistory!!.value!!.archivalPrices = response.body()?.prices
                else {
                    mldPriceHistory!!.value = null
                    mldErrorCode.value = Pair(true, REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE)
                }
            }

            override fun onFailure(call: Call<CryptocurrencyPriceHistoryFromApi>, t: Throwable) {
                mldPriceHistory!!.value = null

                mldErrorCode.value = Pair(true, REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE)

                Log.e(
                    "myApp", "onFailure, url:${call.request().url().toString()}" +
                            "\n exc: ${t.stackTraceToString()}"
                )
            }
        })

        return mldPriceHistory!!
    }

}
