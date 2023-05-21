package com.arturkowalczyk300.cryptocurrencyprices.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.arturkowalczyk300.cryptocurrencyprices.model.Repository

class AddEditPriceAlertViewModel(application: Application): ViewModel() {
    private var repository: Repository = Repository(application) //TODO: DI

    //livedata properties
    private var _allCryptocurrencies = repository.getAllCryptocurrencies()
    val allCryptocurrencies = _allCryptocurrencies

    var selectedCryptocurrencyId: String? = null
}