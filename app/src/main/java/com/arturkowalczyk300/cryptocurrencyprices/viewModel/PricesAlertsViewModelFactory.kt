package com.arturkowalczyk300.cryptocurrencyprices.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PricesAlertsViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PricesAlertsViewModel::class.java))
            return PricesAlertsViewModel(application) as T

        throw IllegalArgumentException("Appropriate ViewModel class not found!")
    }
}