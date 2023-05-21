package com.arturkowalczyk300.cryptocurrencyprices.viewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.arturkowalczyk300.cryptocurrencyprices.model.Repository
import com.arturkowalczyk300.cryptocurrencyprices.model.room.AlertType
import com.arturkowalczyk300.cryptocurrencyprices.model.room.PriceAlertEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PricesAlertsViewModel @Inject constructor(private var repository: Repository) : ViewModel() {
    private var _pricesAlerts = repository.getPricesAlerts()
    val pricesAlerts: LiveData<List<PriceAlertEntity>> = _pricesAlerts

    fun addPriceAlert(cryptocurrency: String, alertType: AlertType, valueThreshold: Float) {
        repository.addPriceAlert(
            PriceAlertEntity(
                0,
                true,
                cryptocurrency,
                alertType,
                valueThreshold
            )
        )
    }

    fun deletePriceAlert(entity: PriceAlertEntity) {
        repository.deletePriceAlert(entity)
    }
}