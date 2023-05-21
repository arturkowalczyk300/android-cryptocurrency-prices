package com.arturkowalczyk300.cryptocurrencyprices.other

import java.text.SimpleDateFormat
import java.util.Date

class DateFormatterUtil { //TODO: DI

    companion object {
        private const val DEFAULT_DATE_ONLY_FORMAT = "dd.MM.yyyy" //use in case when custom is not provided
        private const val DEFAULT_DATE_WITH_TIME_FORMAT = "dd.MM.yyyy HH:mm"
        private var dateOnlyFormatter: SimpleDateFormat? = null
        private var dateWithTimeFormatter: SimpleDateFormat? = null

        var customDateOnlyFormat: String? = null
        var customDateWithTimeFormat: String? = null

        private fun initDateOnlyFormatter() {
            dateOnlyFormatter = if (customDateOnlyFormat == null)
                SimpleDateFormat(DEFAULT_DATE_ONLY_FORMAT)
            else
                SimpleDateFormat(customDateOnlyFormat)
        }

        fun parseDateOnly(dateString: String): Date {
            if (dateOnlyFormatter == null) initDateOnlyFormatter()

            return dateOnlyFormatter!!.parse(dateString)
        }

        fun formatDateOnly(date: Date): String {
            if (dateOnlyFormatter == null) initDateOnlyFormatter()

            return dateOnlyFormatter!!.format(date)
        }

        private fun initDateWithTimeFormatter() {
            dateWithTimeFormatter = if (customDateWithTimeFormat == null)
                SimpleDateFormat(DEFAULT_DATE_WITH_TIME_FORMAT)
            else
                SimpleDateFormat(customDateWithTimeFormat)
        }

        fun parseDateWithTime(dateString: String): Date {
            if (dateWithTimeFormatter == null) initDateWithTimeFormatter()

            return dateWithTimeFormatter!!.parse(dateString)
        }

        fun formatDateWithTime(date: Date): String {
            if (dateWithTimeFormatter == null) initDateWithTimeFormatter()

            return dateWithTimeFormatter!!.format(date)
        }
    }
}