package com.arturkowalczyk300.cryptocurrencyprices.other.prefs

import java.util.*

class DateUtils {
    companion object {
        fun areDatesEqual(date1: Date?, date2: Date?): Boolean {
            if (date1 == null || date2 == null)
                return false

            val cal1 = Calendar.getInstance().apply {
                time=date1
            }
            val cal2 = Calendar.getInstance().apply {
                time = date2
            }

            return (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                    && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
                    && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH))
        }
    }
}