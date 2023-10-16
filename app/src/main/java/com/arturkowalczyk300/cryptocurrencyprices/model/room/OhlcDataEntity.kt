package com.arturkowalczyk300.cryptocurrencyprices.model.room

data class OhlcDataEntity(
    val timestamp: Long,
    val open: Int,
    val high: Int,
    val low: Int,
    val close: Int,
)
