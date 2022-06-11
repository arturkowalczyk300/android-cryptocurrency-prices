package com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess

data class CryptocurrencyPriceFromListApi(
    val id: String,
    val symbol: String,
    val current_price: Double, //in vs currency
    val market_cap: Double //in vs currency
)