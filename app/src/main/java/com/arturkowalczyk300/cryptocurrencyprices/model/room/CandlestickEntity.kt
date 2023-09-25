package com.arturkowalczyk300.cryptocurrencyprices.model.room

data class CandlestickEntity(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
)
