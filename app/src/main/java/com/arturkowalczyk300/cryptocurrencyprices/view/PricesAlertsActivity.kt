package com.arturkowalczyk300.cryptocurrencyprices.view

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.model.room.AlertType
import com.arturkowalczyk300.cryptocurrencyprices.other.prefs.SharedPreferencesHelper
import com.arturkowalczyk300.cryptocurrencyprices.viewModel.PricesAlertsViewModel
import com.arturkowalczyk300.cryptocurrencyprices.viewModel.PricesAlertsViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PricesAlertsActivity : AppCompatActivity() {
    private lateinit var rvPricesAlerts: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var viewModel: PricesAlertsViewModel
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var swEnableAlerts: Switch
    private lateinit var sharedPrefsInstance: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prices_alerts)
        setTitle(R.string.activityPricesAlerts)
        rvPricesAlerts = findViewById(R.id.rvPricesAlerts)
        fab = findViewById(R.id.fab)
        swEnableAlerts = findViewById(R.id.swEnableAlerts)

        sharedPrefsInstance = SharedPreferencesHelper(this)

        swEnableAlerts.setOnCheckedChangeListener { buttonView, isChecked ->
            sharedPrefsInstance.setPricesAlertsEnabled(isChecked)
        }
        swEnableAlerts.isChecked = sharedPrefsInstance.getPricesAlertsEnabled() ?: false

        rvPricesAlerts.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        val factory = PricesAlertsViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory).get(PricesAlertsViewModel::class.java)

        observeData()

        resultLauncher = registerForActivityResult(StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val data = it.data!!
                viewModel.addPriceAlert(
                    data.getStringExtra("currency")!!,
                    AlertType.values()[data.getIntExtra("alert_type", -1)],
                    data.getStringExtra("threshold")!!.toFloat()
                )
            }
        }
        fab.setOnClickListener {
            launchAddActivity()
        }
        rvPricesAlerts.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun observeData() {
        viewModel.pricesAlerts.observe(this) {
            rvPricesAlerts.adapter = PriceAlertRecyclerAdapter(this, it)
            { viewModel.deletePriceAlert(it) }
        }
    }

    fun launchAddActivity() {
        val intent = Intent(this, AddPriceAlertActivity::class.java)
        resultLauncher.launch(intent)
    }
}