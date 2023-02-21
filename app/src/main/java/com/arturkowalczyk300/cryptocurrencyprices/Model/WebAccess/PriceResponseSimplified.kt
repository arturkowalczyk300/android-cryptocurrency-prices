package com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess

data class PriceResponseSimplified(
    val id: String,
    val symbol: String,
    val name: String,
    val current_price: Double, //in vs currency
    val market_cap: Long, //in vs currency
    val market_cap_rank: Int
)