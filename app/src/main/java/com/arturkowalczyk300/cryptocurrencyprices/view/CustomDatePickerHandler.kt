package com.arturkowalczyk300.cryptocurrencyprices.view

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import com.arturkowalczyk300.cryptocurrencyprices.R
import java.util.*


class CustomDatePicker(context: Context) : DatePickerDialog(context) {
    init {
        datePicker.maxDate = System.currentTimeMillis() //forbid choosing future date
    }

    var flagSkipDismiss: Boolean = false

    override fun dismiss() {
        if (flagSkipDismiss)
            flagSkipDismiss = false
        else
            super.dismiss()
    }
}


class CustomDatePickerHandler {
    private lateinit var datePickerDialog: CustomDatePicker //TODO: DI

    private val currentDate: Calendar = Calendar.getInstance() //TODO: DI
    private val currentSelectedDate: Calendar = Calendar.getInstance()
    private lateinit var listenerOnDateChanged: (dateString: String) -> Unit

    fun initializeDatePicker(context: Context) {
        datePickerDialog = CustomDatePicker(context)

        //set initial values
        datePickerDialog.updateDate(
            currentSelectedDate.get(Calendar.YEAR),
            currentSelectedDate.get(Calendar.MONTH),
            currentSelectedDate.get(Calendar.DAY_OF_MONTH)
        )

        //set listener
        datePickerDialog.setOnDateSetListener { view, year, monthOfYear, day ->
            val month = monthOfYear + 1
            listenerOnDateChanged.invoke("${day}.${month}.${year}")
        }

        //add button Today
        datePickerDialog.setButton(
            DialogInterface.BUTTON_NEUTRAL,
            context.getString(R.string.btnToday),
            DialogInterface.OnClickListener { dialog, which ->
                datePickerDialog.updateDate(
                    currentDate.get(Calendar.YEAR),
                    currentDate.get(Calendar.MONTH),
                    currentDate.get(Calendar.DAY_OF_MONTH)
                )
                datePickerDialog.flagSkipDismiss =
                    true //clicking "Today" button won't close date picker
            })
    }

    fun show() {
        datePickerDialog!!.show()
    }

    fun setListenerOnDateChanged(listener: (dateString: String) -> Unit) {
        listenerOnDateChanged = listener
    }
}