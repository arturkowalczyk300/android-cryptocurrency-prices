package com.arturkowalczyk300.cryptocurrencyprices.model.webAccess

data class OhlcDataResponseEntity(
    val time: Long,
    val open: Int,
    val high: Int,
    val low: Int,
    val close: Int
)
