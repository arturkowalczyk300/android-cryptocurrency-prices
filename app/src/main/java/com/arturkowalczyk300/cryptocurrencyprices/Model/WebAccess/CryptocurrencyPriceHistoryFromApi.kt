package com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess

data class CryptocurrencyPriceHistoryFromApi(
    val market_caps: List<List<Double>>,
    val prices: List<List<Double>>, //it interests me , TODO: write converter to pair?
    val total_volumes: List<List<Double>>
)