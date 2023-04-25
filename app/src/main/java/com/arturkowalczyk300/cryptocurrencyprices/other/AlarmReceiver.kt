package com.arturkowalczyk300.cryptocurrencyprices.other

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.arturkowalczyk300.cryptocurrencyprices.other.prefs.SharedPreferencesHelper

const val PRICE_ALERT_INTENT_ACTION =
    "com.arturkowalczyk300.cryptocurrencyprices.PRICE_ALERT_INTENT_ACTION"
const val PRICE_ALERTS_CHECK_INTERVAL_MILLIS = 1 * 60 * 1000L

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val alertsEnabled =
            (context != null) && SharedPreferencesHelper(context!!).getPricesAlertsEnabled() == true

        if (intent?.action == PRICE_ALERT_INTENT_ACTION && alertsEnabled) {
            val intent = Intent(context, PriceAlertsService::class.java)
            context?.startForegroundService(intent)
        }
    }
}