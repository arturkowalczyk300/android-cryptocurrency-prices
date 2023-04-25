package com.arturkowalczyk300.cryptocurrencyprices.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AddEditPriceAlertViewModelFactory(private val application: Application): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(AddEditPriceAlertViewModel::class.java))
            return AddEditPriceAlertViewModel(application) as T

        throw IllegalArgumentException("Appropriate ViewModel class not found!")
    }
}