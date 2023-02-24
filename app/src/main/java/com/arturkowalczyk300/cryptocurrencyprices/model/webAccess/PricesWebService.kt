package com.arturkowalczyk300.cryptocurrencyprices.model.webAccess

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arturkowalczyk300.cryptocurrencyprices.model.*
import com.arturkowalczyk300.cryptocurrencyprices.other.Constants
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
    var entity: PricesResponse? = null,
    var actualPrice: Float? = null,
    var flagDataSet: Boolean = false,
) {

}

class RequestWithResponseArchival(
    currencySymbol: String = "",
    date: Date = Date(0),
    val vs_currency: String,
    val unixtimeFrom: Long,
    val unixTimeTo: Long,
    var archivalPrices: List<List<Double>>? = null,
    var totalVolumes: List<List<Double>>? = null,
    var marketCaps: List<List<Double>>? = null,
) : RequestWithResponse(
    currencySymbol,
    date,
    null,
    null,
    false
) {
}


class CryptocurrencyPricesWebService {
    private var _isDataUpdatedSuccessfully = MutableLiveData<Boolean>(false)
    val isDataUpdatedSuccessfully: LiveData<Boolean> = _isDataUpdatedSuccessfully

    var waitingForResponse: Boolean = false

    var mldErrorCode: MutableLiveData<Pair<Boolean, ErrorMessage>> = MutableLiveData()

    var mldActualRequestWithResponse: MutableLiveData<RequestWithResponse> = MutableLiveData(
        RequestWithResponse()
    )

    var mldArchivalRequestWithResponse: MutableLiveData<RequestWithResponse> = MutableLiveData(
        RequestWithResponse()
    )

    var mldPriceHistory: MutableLiveData<RequestWithResponseArchival?>? = MutableLiveData()

    var cryptocurrenciesListSorted: MutableLiveData<ArrayList<PriceResponseSimplified>> =
        MutableLiveData()

    init {
        when (PricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()) {
            null -> Log.e("myApp", "ApiHandleInstance is null")
            else -> {
            }
        }
    }

    fun resetFlagIsDataUpdatedSuccessfully(){
        _isDataUpdatedSuccessfully.value = false
    }

    fun requestActualPriceData(
        currencySymbol: String,
        vs_currency: String,
    ): MutableLiveData<RequestWithResponse> {
        mldActualRequestWithResponse?.value?.date = Date()
        mldActualRequestWithResponse?.value?.currencySymbol = currencySymbol
        mldActualRequestWithResponse?.value?.entity = null //test line
        waitingForResponse = true

        val response: Call<ResponseBody>? =
            PricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()
                ?.getActualPrice(currencySymbol, vs_currency)


        response?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>,
            ) {
                if (response.code() == 429)
                    mldErrorCode.value = Pair(true, ErrorMessage(REQUEST_EXCEEDED_API_RATE_LIMIT))
                else if (response.body() != null) {
                    if (waitingForResponse) {

                        val bodyStr: ResponseBody = response.body()!!
                        val src = bodyStr.source().toString()
                        val regex = Regex("(\\d+.\\d+[\\de-]*)")
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
                    mldErrorCode.value = Pair(true, ErrorMessage(REQUEST_PRICE_DATA_FAILURE))
                }
                waitingForResponse = false
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                mldErrorCode.value = Pair(
                    true, ErrorMessage(
                        REQUEST_PRICE_DATA_FAILURE,
                        "${t.stackTraceToString()}"
                    )
                )

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
        date: Date,
    ): MutableLiveData<RequestWithResponse> {
        mldArchivalRequestWithResponse?.value?.date = date
        mldArchivalRequestWithResponse?.value?.currencySymbol = currencySymbol
        mldArchivalRequestWithResponse?.value?.entity = null //test line
        waitingForResponse = true

        val sdf = SimpleDateFormat(Constants.API_DATE_FORMAT)
        val formattedDate = sdf.format(date)

        val response: Call<PricesResponse>? =
            PricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()
                ?.getArchivalPrice(currencySymbol, formattedDate)


        response?.enqueue(object : Callback<PricesResponse> {
            override fun onResponse(
                call: Call<PricesResponse>,
                response: Response<PricesResponse>,
            ) {
                if (response.code() == 429)
                    mldErrorCode.value = Pair(true, ErrorMessage(REQUEST_EXCEEDED_API_RATE_LIMIT))
                else if (response.body() != null) {
                    if (waitingForResponse) {
                        mldArchivalRequestWithResponse?.value?.entity = response.body()
                        mldArchivalRequestWithResponse?.value?.flagDataSet = true
                        mldArchivalRequestWithResponse.value =
                            mldArchivalRequestWithResponse.value //notify data changed
                    }
                } else {
                    mldErrorCode.value = Pair(true, ErrorMessage(REQUEST_PRICE_DATA_FAILURE))
                }
                waitingForResponse = false
            }

            override fun onFailure(call: Call<PricesResponse>, t: Throwable) {
                mldErrorCode.value = Pair(
                    true, ErrorMessage(
                        REQUEST_PRICE_DATA_FAILURE,
                        "${t.stackTraceToString()}"
                    )
                )

                Log.e(
                    "myApp", "onFailure, url:${call.request().url().toString()}" +
                            "\n exc: ${t.stackTraceToString()}"
                )
            }
        })

        return mldArchivalRequestWithResponse
    }

    fun requestCryptocurrenciesList(): MutableLiveData<ArrayList<PriceResponseSimplified>> {
        val listResponse: Call<List<PriceResponseSimplified>>? =
            PricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()
                ?.getCryptocurrenciesList("USD", "market_cap_desc", 100, 1, false)

        listResponse?.enqueue(object : Callback<List<PriceResponseSimplified>> {
            override fun onResponse(
                call: Call<List<PriceResponseSimplified>>,
                response: Response<List<PriceResponseSimplified>>,
            ) {
                val responseBody = response.body()
                if (response.code() == 429)
                    mldErrorCode.value = Pair(true, ErrorMessage(REQUEST_EXCEEDED_API_RATE_LIMIT))
                else if (response.body() != null && response.code() != 404) { //valid response
                    cryptocurrenciesListSorted.value = ArrayList(response.body())
                } else {
                    mldErrorCode.value =
                        Pair(true, ErrorMessage(REQUEST_CRYPTOCURRENCIES_LIST_FAILURE))
                    Log.e(
                        "myApp",
                        "invalid response! [size=${responseBody?.size}, code=${response.code()}]"
                    )
                }
            }

            override fun onFailure(call: Call<List<PriceResponseSimplified>>, t: Throwable) {
                mldErrorCode.value = Pair(
                    true, ErrorMessage(
                        REQUEST_CRYPTOCURRENCIES_LIST_FAILURE,
                        "${t.stackTraceToString()}"
                    )
                )
                Log.e("myApp", "onFailure")
            }
        })
        return cryptocurrenciesListSorted
    }

    fun requestPriceHistoryForDateRange(
        currencySymbol: String,
        vs_currency: String,
        unixtimeFrom: Long,
        unixTimeTo: Long,
    ): MutableLiveData<RequestWithResponseArchival?> {
        mldPriceHistory!!.value = RequestWithResponseArchival(
            currencySymbol,
            Date(),
            vs_currency,
            unixtimeFrom,
            unixTimeTo
        )

        val response: Call<PriceHistoryResponse>? =
            PricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()
                ?.getHistoryOfPriceForDateRange(
                    currencySymbol,
                    vs_currency,
                    unixtimeFrom,
                    unixTimeTo
                )


        response?.enqueue(object : Callback<PriceHistoryResponse> {
            override fun onResponse(
                call: Call<PriceHistoryResponse>,
                response: Response<PriceHistoryResponse>,
            ) {
                Log.d("myApp", "code=$response.code()")
                if (response.code() == 429)
                    mldErrorCode.value = Pair(true, ErrorMessage(REQUEST_EXCEEDED_API_RATE_LIMIT))
                else if (response.body() != null && response.body()?.prices?.isNotEmpty() != null) {
                    _isDataUpdatedSuccessfully.value = true

                    mldPriceHistory!!.value!!.archivalPrices = response.body()?.prices
                    mldPriceHistory!!.value!!.totalVolumes = response.body()?.total_volumes
                    mldPriceHistory!!.value!!.marketCaps = response.body()?.market_caps
                    mldPriceHistory!!.postValue(mldPriceHistory!!.value) //notify data changed
                } else {
                    mldPriceHistory!!.value = null
                    mldErrorCode.value =
                        Pair(true, ErrorMessage(REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE))
                    Log.e(
                        "myApp",
                        "onResponse, invalid response, respCode=${response.code()} errorCode=${
                            response.errorBody()?.string()
                        } }"
                    )
                }
            }

            override fun onFailure(call: Call<PriceHistoryResponse>, t: Throwable) {
                mldPriceHistory!!.value = null
                mldErrorCode.value =
                    Pair(
                        true, ErrorMessage(
                            REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE,
                            "${t.stackTraceToString()}"
                        )
                    )

                Log.e(
                    "myApp", "onFailure, url:${call.request().url().toString()}" +
                            "\n exc: ${t.stackTraceToString()}"
                )
            }
        })

        return mldPriceHistory!!
    }

}
