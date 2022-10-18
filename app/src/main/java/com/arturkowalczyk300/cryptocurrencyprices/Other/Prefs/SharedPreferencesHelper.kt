package com.arturkowalczyk300.cryptocurrencyprices.Other.Prefs

import android.content.Context
import android.content.SharedPreferences
import com.arturkowalczyk300.cryptocurrencyprices.R

class SharedPreferencesHelper(val context: Context) {
    private val PREF_FILE_NAME = "com.arturkowalczyk300.cryptocurrencyprices.PREF_FILE"
    private val PREF_LAST_CHOSEN_CRYPTOCURRENCY =
        "com.arturkowalczyk300.cryptocurrencyprices.PREF_FILE.PREF_LAST_CHOSEN_CRYPTOCURRENCY"

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
}