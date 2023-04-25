package com.arturkowalczyk300.cryptocurrencyprices.other.prefs

import android.content.Context
import android.content.SharedPreferences
import com.arturkowalczyk300.cryptocurrencyprices.R

class SharedPreferencesHelper(val context: Context) {
    private val PREF_FILE_NAME = "com.arturkowalczyk300.cryptocurrencyprices.PREF_FILE"
    private val PREF_LAST_CHOSEN_CRYPTOCURRENCY =
        "com.arturkowalczyk300.cryptocurrencyprices.PREF_FILE.PREF_LAST_CHOSEN_CRYPTOCURRENCY"
    private val PREF_PRICES_ALERTS_ENABLED =
        "com.arturkowalczyk300.cryptocurrencyprices.PREF_PRICES_ALERTS_ENABLED"

    var sharedPrefsInstance: SharedPreferences

    init {
        sharedPrefsInstance = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    }

    fun setLastChosenCryptocurrency(value: String) {
        sharedPrefsInstance
            .edit()
            .putString(PREF_LAST_CHOSEN_CRYPTOCURRENCY, value)
            .apply()
    }

    fun getLastChosenCryptocurrency(): String? {
        return sharedPrefsInstance
            .getString(
                PREF_LAST_CHOSEN_CRYPTOCURRENCY,
                context.getString(R.string.defaultCryptocurrency)
            )
    }

    fun setPricesAlertsEnabled(value: Boolean) {
        sharedPrefsInstance
            .edit()
            .putBoolean(PREF_PRICES_ALERTS_ENABLED, value)
            .apply()
    }

    fun getPricesAlertsEnabled(): Boolean? {
        return sharedPrefsInstance
            .getBoolean(
                PREF_PRICES_ALERTS_ENABLED,
                false
            )
    }
}