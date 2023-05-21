package com.arturkowalczyk300.cryptocurrencyprices.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.arturkowalczyk300.cryptocurrencyprices.model.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddEditPriceAlertViewModel @Inject constructor(repository: Repository) : ViewModel() {
    //livedata properties
    private var _allCryptocurrencies = repository.getAllCryptocurrencies()
    val allCryptocurrencies = _allCryptocurrencies

    var selectedCryptocurrencyId: String? = null
}