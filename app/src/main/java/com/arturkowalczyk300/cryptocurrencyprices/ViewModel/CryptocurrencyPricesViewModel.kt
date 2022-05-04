package com.arturkowalczyk300.cryptocurrencyprices.ViewModel

import androidx.lifecycle.ViewModel
import com.arturkowalczyk300.cryptocurrencyprices.Model.CryptocurrencyPricesRepository

class CryptocurrencyPricesViewModel :ViewModel(){
   var repository: CryptocurrencyPricesRepository = CryptocurrencyPricesRepository()
}