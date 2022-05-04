package com.arturkowalczyk300.cryptocurrencyprices.View

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesDatabase
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModel
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModelFactory
import java.lang.Exception
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: CryptocurrencyPricesViewModel
    private lateinit var etCurrencySymbol: EditText
    private lateinit var etDate: EditText
    private lateinit var btnGet: Button

    private lateinit var tvCryptocurrencySymbol: TextView
    private lateinit var tvCryptocurrencyDate: TextView
    private lateinit var tvCryptocurrencyPrice: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etCurrencySymbol = findViewById(R.id.etCurrencySymbol)
        etDate = findViewById(R.id.etDate)
        btnGet = findViewById(R.id.btnGet)

        tvCryptocurrencySymbol = findViewById(R.id.tvCryptocurrencySymbol)
        tvCryptocurrencyDate = findViewById(R.id.tvCryptocurrencyDate)
        tvCryptocurrencyPrice = findViewById(R.id.tvCryptocurrencyPrice)

        //viewModel = ViewModelProvider(this).get(CryptocurrencyPricesViewModel::class.java)
        val factory = CryptocurrencyPricesViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory).get(CryptocurrencyPricesViewModel::class.java)

        btnGet.setOnClickListener {
            var date: Date

            var sdf: SimpleDateFormat = SimpleDateFormat("dd.MM.yyyy")
            try {
                date = sdf.parse(etDate.text.toString())

                viewModel.requestPriceData(
                    etCurrencySymbol.text.toString(),
                    date
                ).observe(this, Observer {
                    if (it.entity != null) {
                        Log.v("myApp", "response in MainActivity")
                        //tvResponse.text = it.toString()

                        tvCryptocurrencySymbol.text = it.currencySymbol
                        tvCryptocurrencyDate.text = it.getFormattedDate()
                        tvCryptocurrencyPrice.text =
                            "%.3fUSD".format(it.entity?.market_data?.current_price?.usd)
                    }
                })
            } catch (exc: Exception) {
                Log.v("myApp", exc.toString())
            }
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.itemAddRecord -> {
                viewModel.addReading()
            }
            R.id.itemGetAllRecords -> {
                viewModel.getAllReadings()
            }
            R.id.itemClearRecords -> {
                viewModel.clearAllRecords()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}