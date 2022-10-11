package com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess

import retrofit2.http.GET
import retrofit2.Call
import retrofit2.http.Path
import retrofit2.http.Query

interface CryptocurrencyPricesApiHandle {
    @GET("api/v3/coins/{currency}/history")
    fun getPrice(
        @Path("currency") currencySymbol: String,
        @Query("date") dateString: String
    ): Call<CryptocurrencyPricesEntityApi>

    @GET("api/v3/coins/markets")
    fun getListOfCryptocurrencies(
        @Query("vs_currency") vsCurrency: String, //e.g. USD
        @Query("order") order: String, //e.g. market_cap_desc - sorting by market cap, descending
        @Query("per_page") recordsPerPage: Int, //e.g. 100
        @Query("page") currentPage: Int, //e.g. 1
        @Query("sparkline") sparkline: Boolean, //e.g. false
    ): Call<List<CryptocurrencyPriceFromListApi>>

    @GET("api/v3/coins/{currency}/market_chart/range")
    fun getHistoryOfPriceForDateRange(
        @Path("currency") currencySymbol: String, //e.g. bitcoin
        @Query("vs_currency") vsCurrency: String, //e.g. USD
        @Query("from") unixTimeFrom: Long, //e.g. 1654370519 - 04.06.2022 192:21:59
        @Query("to") unixTimeTo: Long, //e.g. 1656962519 - 04.07.2022 192:21:59
    ): Call<CryptocurrencyPriceHistoryFromApi>
}