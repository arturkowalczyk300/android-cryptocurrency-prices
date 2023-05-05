package com.arturkowalczyk300.cryptocurrencyprices.view

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.other.prefs.SharedPreferencesHelper
import com.arturkowalczyk300.cryptocurrencyprices.viewModel.AddEditPriceAlertViewModel
import com.arturkowalczyk300.cryptocurrencyprices.viewModel.AddEditPriceAlertViewModelFactory

class AddPriceAlertActivity : AppCompatActivity() {
    private lateinit var tvCryptocurrencyId: TextView
    private lateinit var etAlertThreshold: EditText
    private lateinit var spinnerAlertTypes: Spinner

    private var isCurrenciesListInitialized: Boolean = false
    private lateinit var viewModel: AddEditPriceAlertViewModel
    private lateinit var sharedPrefsInstance: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_price_alert)

        setTitle(R.string.addPriceAlert)

        tvCryptocurrencyId = findViewById(R.id.tvCryptocurrencyId)
        etAlertThreshold = findViewById(R.id.etAlertThreshold)
        spinnerAlertTypes = findViewById(R.id.spinnerAlertsTypes)

        val factory = AddEditPriceAlertViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory).get(AddEditPriceAlertViewModel::class.java)

        sharedPrefsInstance = SharedPreferencesHelper(applicationContext)

        handleCryptocurrencyChoice()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.add_edit_price_alert_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveData() {
        val intent = Intent().apply {
            putExtra("currency", tvCryptocurrencyId.text.toString())
            putExtra("alert_type", spinnerAlertTypes.selectedItemId.toInt())
            putExtra("threshold", etAlertThreshold.text.toString())
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun handleCryptocurrencyChoice() {
        viewModel.allCryptocurrencies.observe(this, Observer { it ->
            isCurrenciesListInitialized = true

            if (sharedPrefsInstance.getLastChosenCryptocurrency() != null) {
                val curr = sharedPrefsInstance.getLastChosenCryptocurrency()
                tvCryptocurrencyId.text = curr
                viewModel.selectedCryptocurrencyId = curr
            } else {
                val curr = it.first().cryptocurrencyId
                tvCryptocurrencyId.text = curr
                viewModel.selectedCryptocurrencyId = curr
            }

        })

        tvCryptocurrencyId.setOnClickListener {
            viewModel.allCryptocurrencies.value?.let { list ->
                val dialog = DialogListWithSearchTool()

                if (!dialog.isListenerSet)
                    dialog.setOnItemClickListener { cryptocurrencyId ->
                        tvCryptocurrencyId.text = cryptocurrencyId
                        viewModel.selectedCryptocurrencyId = cryptocurrencyId
                    }

                dialog.open(this, list.map { it.cryptocurrencyId })
            }
        }
    }
}