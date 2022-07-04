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

    @GET("api/v3/coins/{currency}/market_chart/range?from=1654368441&to=1656960441")
    fun getHistoryOfPriceForLastMonth(
        @Path("currency") currencySymbol: String, //e.g. bitcoin
        @Query("vs_currency") vsCurrency: String, //e.g. USD
    ): Call<CryptocurrencyPriceHistoryFromApi>
}