package com.arturkowalczyk300.cryptocurrencyprices.view

import android.app.Activity
import android.app.AlertDialog
import android.preference.PreferenceManager
import com.arturkowalczyk300.cryptocurrencyprices.R

class EulaDialog(val activity: Activity) {
    val EULA_KEY = "eula"
    fun show() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val hasBeenShown = sharedPreferences.getBoolean(EULA_KEY, false)
        if (!hasBeenShown) {
            val title = activity.getString(R.string.eula_title)
            val message = activity.getString(R.string.eula_content)

            var builder = AlertDialog.Builder(activity)
            builder = builder.setTitle(title).setMessage(message)
                .setPositiveButton(R.string.eula_button_agree) { dialogInterface, _ ->
                    val editor = sharedPreferences.edit()
                    editor.putBoolean(EULA_KEY, true)
                    editor.commit()
                    dialogInterface.dismiss()
                }
            builder = builder.setNegativeButton(R.string.eula_exit) { _, _ ->
                exitActivity()
            }

            val dialog = builder.create()
            dialog.setCanceledOnTouchOutside(true)
            dialog.setOnCancelListener {
                exitActivity()
            }
            dialog.show()
        }
    }

    private fun exitActivity() {
        activity.finish()
    }
}