package com.arturkowalczyk300.cryptocurrencyprices

import com.arturkowalczyk300.cryptocurrencyprices.model.webAccess.PricesApiHandle
import com.arturkowalczyk300.cryptocurrencyprices.model.webAccess.PricesRetrofitClient
import com.arturkowalczyk300.cryptocurrencyprices.other.Constants
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class RetrofitTest {
    private var retrofitInstance: Retrofit? = null
    private var apiHandle: PricesApiHandle? = null

    @Before
    fun setup(){
         retrofitInstance = PricesRetrofitClient.getRetrofitInstance() //TODO: use hilt??
         apiHandle =
            PricesRetrofitClient.getCryptocurrencyPricesApiHandleInstance()
    }

    @Test
    fun testRetrofitInstance() {
        val currentUrl = retrofitInstance!!.baseUrl().url().toString()
        val baseUrl = Constants.BASE_URL
        assertThat(currentUrl).isEqualTo(baseUrl)
    }

    @Test
    fun testApiCallGetPrices() {
        val response = apiHandle!!.getArchivalPrice(
            "bitcoin",
            "02-06-2022"
        ).execute()

        val errorBody = response.errorBody()
        assertThat(errorBody).isEqualTo(null)

        val responseBody = response.body()
        assertThat(responseBody).isNotEqualTo(null)
        assertThat(response.code()).isEqualTo(200)

        //verification of data
        assertThat(responseBody!!.market_data.current_price.usd).isEqualTo(29833.450330205254)
    }

    @Test
    fun testApiCallGetListOfCryptocurrencies() {
        val response = apiHandle!!.getCryptocurrenciesList(
            "USD",
            "market_cap_desc",
            100,
            1,
            false
        ).execute()

        val errorBody = response.errorBody()
        assertThat(errorBody).isEqualTo(null)

        val responseBody = response.body()
        assertThat(responseBody).isNotEqualTo(null)
        assertThat(response.code()).isEqualTo(200)

        //verification of data
        val namesList = responseBody!!.map {
            it.id
        }
        assertThat(namesList).contains("bitcoin")
        assertThat(namesList).contains("ethereum")
    }

    @Test
    fun testApiCallGetHistoryOfPriceForDateRange(){
        val response = apiHandle!!.getHistoryOfPriceForDateRange(
            "bitcoin",
            "USD",
            1654370519, //04.06.2022 192:21:59
            1656962519 //04.07.2022 192:21:59
        ).execute()

        val errorBody = response.errorBody()
        assertThat(errorBody).isEqualTo(null)

        val responseBody = response.body()
        assertThat(responseBody).isNotEqualTo(null)
        assertThat(response.code()).isEqualTo(200)

        //verification of data
        assertThat(responseBody!!.prices).isNotNull()
        assertThat(responseBody!!.prices).isNotEmpty()
    }

}