package com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess

import retrofit2.http.GET
import retrofit2.Call
import retrofit2.http.Path
import retrofit2.http.Query

interface CryptocurrencyPricesApiHandle {
    @GET("api/v3/coins/{currency}/history") //date=01-05-2022
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
}