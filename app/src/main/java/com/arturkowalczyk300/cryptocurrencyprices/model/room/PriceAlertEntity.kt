package com.arturkowalczyk300.cryptocurrencyprices.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlertType {
    ALERT_WHEN_CURRENT_VALUE_IS_BIGGER,
    ALERT_WHEN_CURRENT_VALUE_IS_SMALLER
}

@Entity(tableName = "prices_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val ID: Int=0,
    val active: Boolean,
    val cryptocurrencySymbol: String,
    val alertType: AlertType,
    val valueThreshold: Float,
)
