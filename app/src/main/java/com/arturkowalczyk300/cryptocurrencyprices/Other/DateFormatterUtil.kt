package com.arturkowalczyk300.cryptocurrencyprices.Other

import java.text.SimpleDateFormat
import java.util.Date

class DateFormatterUtil {

    companion object {
        const val DEFAULT_DATE_FORMAT = "dd.MM.yyyy" //use in case when custom is not provided
        private var dateFormatter: SimpleDateFormat? = null

        var customDateFormat: String? = null

        private fun initDateFormatter() {
            dateFormatter = if (customDateFormat == null)
                SimpleDateFormat(DEFAULT_DATE_FORMAT)
            else
                SimpleDateFormat(customDateFormat)
        }

        fun parse(dateString: String): Date {
            if (dateFormatter == null) initDateFormatter()

            return dateFormatter!!.parse(dateString)
        }

        fun format(date: Date): String {
            if (dateFormatter == null) initDateFormatter()

            return dateFormatter!!.format(date)
        }
    }
}