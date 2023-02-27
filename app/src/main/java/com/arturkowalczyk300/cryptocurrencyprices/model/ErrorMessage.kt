package com.arturkowalczyk300.cryptocurrencyprices.model

const val REQUEST_PRICE_DATA_FAILURE = 101
const val REQUEST_CRYPTOCURRENCIES_LIST_FAILURE = 201
const val REQUEST_PRICE_HISTORY_FOR_DATE_RANGE_FAILURE = 301
const val REQUEST_EXCEEDED_API_RATE_LIMIT = 302
const val SHOW_DEBUG_TOASTS = false

data class ErrorMessage(
    val errorCode: Int,
    val additionalInfo: String? = null
)