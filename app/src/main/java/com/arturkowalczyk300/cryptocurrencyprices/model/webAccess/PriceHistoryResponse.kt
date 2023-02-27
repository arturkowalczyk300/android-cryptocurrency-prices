package com.arturkowalczyk300.cryptocurrencyprices.model.webAccess

data class PriceHistoryResponse(
    val market_caps: List<List<Double>>,
    val prices: List<List<Double>>,
    val total_volumes: List<List<Double>>
)