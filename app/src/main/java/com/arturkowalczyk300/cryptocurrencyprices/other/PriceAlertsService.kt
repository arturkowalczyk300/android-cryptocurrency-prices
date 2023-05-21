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

private const val FOREGROUND_SERVICE_CHANNEL_ID = "foreground_service_channel"
private const val NOTIFICATION_CHANNEL_ID = "notification_channel"
private const val FOREGROUND_SERVICE_ID = 105

private const val INTERVAL = 20*60*1000L //check alerts every 20 minutes

class PriceAlertsService : Service() {
    private var priceAlerts: List<PriceAlertEntity>? = null
    private var thread: Thread? = null
    private var running = false
    private lateinit var repository: Repository //TODO: DI
    private var startNotificationId=10

    override fun onCreate() {
        super.onCreate()
        createForegroundServiceNotificationChannel()
        startForeground(FOREGROUND_SERVICE_ID, buildForegroundServiceNotification())
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    private fun createForegroundServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_SERVICE_CHANNEL_ID,
                "Foreground service channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "My notification channel",
                NotificationManager.IMPORTANCE_HIGH
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

    private fun buildForegroundServiceNotification() = NotificationCompat.Builder(this, FOREGROUND_SERVICE_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Price alerts service").build()

    private fun buildAlertNotification(description: String) = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Price alert!")
        .setContentText(description).build()

    private fun displayNotification(description: String, id: Int) {
        NotificationManagerCompat.from(this).notify(id, buildAlertNotification(description))
    }
}