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
}