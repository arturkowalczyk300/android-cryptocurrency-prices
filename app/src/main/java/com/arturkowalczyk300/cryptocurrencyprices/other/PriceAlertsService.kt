package com.arturkowalczyk300.cryptocurrencyprices.other

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.model.Repository
import com.arturkowalczyk300.cryptocurrencyprices.model.room.AlertType
import com.arturkowalczyk300.cryptocurrencyprices.model.room.PriceAlertEntity
import java.lang.Thread.sleep

private const val NOTIFICATION_CHANNEL_ID = "my_notification_channel"

private const val INTERVAL = 30000L

class PriceAlertsService : Service() {
    private var priceAlerts: List<PriceAlertEntity>? = null
    private var thread: Thread? = null
    private var running = false
    private lateinit var repository: Repository
    private var startNotificationId=10

    override fun onCreate() {
        Log.d("myApp", "onCreate")
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("myApp", "onStartCommand")
        running = true
        if(!(this::repository.isInitialized))
            repository= Repository(application)
        observeLiveData()

        try {
            thread = Thread {
                while (running) {
                    handleAlerts()
                    running = false
                }

            }.also { it.start() }
        } catch (e: Exception) {
            Log.e("myApp", "exc=$e")
        }

        Log.d("myApp", "onStartCommand - END")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("myApp", "onDestroy")
        running = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "my_notification_channel",
                "My notification channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun observeLiveData() {
        repository.getPricesAlerts().observeForever {
            priceAlerts = it
        }
    }

    private fun handleAlerts() {
        priceAlerts?.forEachIndexed { index, it ->
            val price = repository.getActualPriceOfCryptocurrencySynchronously(
                it.cryptocurrencySymbol,
                "USD"
            )

            var alertOccured = false
            var alertText = ""
            if (it.alertType == AlertType.ALERT_WHEN_CURRENT_VALUE_IS_SMALLER) {
                if (price < it.valueThreshold) {
                    alertOccured = true
                    alertText =
                        "Current price of ${it.cryptocurrencySymbol} is smaller than ${it.valueThreshold}!"
                }
            } else {
                if (price > it.valueThreshold) {
                    alertOccured = true
                    alertText =
                        "Current price of ${it.cryptocurrencySymbol} is bigger than ${it.valueThreshold}!"
                }
            }
            if (alertOccured)
                displayNotification(alertText, startNotificationId++)
        }
    }

    private fun buildNotification(description: String) = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Price alert!")
        .setContentText(description).build()

    private fun displayNotification(description: String, id: Int) {
        NotificationManagerCompat.from(this).notify(id, buildNotification(description))
    }
}