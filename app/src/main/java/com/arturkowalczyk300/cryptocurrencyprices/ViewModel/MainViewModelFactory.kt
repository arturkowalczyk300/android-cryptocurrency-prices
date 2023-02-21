package com.arturkowalczyk300.cryptocurrencyprices.ViewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModelFactory(var application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(CryptocurrencyPricesViewModel::class.java)) TODO()
        return MainViewModel(application) as T
    }
}