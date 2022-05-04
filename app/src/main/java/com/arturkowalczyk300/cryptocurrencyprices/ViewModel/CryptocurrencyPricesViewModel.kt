package com.arturkowalczyk300.cryptocurrencyprices.ViewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arturkowalczyk300.cryptocurrencyprices.Model.CryptocurrencyPricesRepository
import com.arturkowalczyk300.cryptocurrencyprices.Model.WebAccess.RequestWithResponse
import java.util.*

class CryptocurrencyPricesViewModel : ViewModel() {
    var repository: CryptocurrencyPricesRepository = CryptocurrencyPricesRepository()

    public fun requestPriceData(
        currencySymbol: String,
        date: Date
    ): MutableLiveData<RequestWithResponse> {
        return repository.requestPriceData(currencySymbol, date)
    }
}