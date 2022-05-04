package com.arturkowalczyk300.cryptocurrencyprices.View

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.ViewModel.CryptocurrencyPricesViewModel
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
    private lateinit var tvResponse: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etCurrencySymbol = findViewById(R.id.etCurrencySymbol)
        etDate = findViewById(R.id.etDate)
        btnGet = findViewById(R.id.btnGet)
        tvResponse = findViewById(R.id.tvResponse)

        viewModel = ViewModelProvider(this).get(CryptocurrencyPricesViewModel::class.java)

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
                        tvResponse.text = it.toString()
                    }
                })
            } catch (exc: Exception) {
                Log.v("myApp", exc.toString())
            }
        }


    }
}