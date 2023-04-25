package com.arturkowalczyk300.cryptocurrencyprices.other

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arturkowalczyk300.cryptocurrencyprices.R
import com.arturkowalczyk300.cryptocurrencyprices.model.Repository
import com.arturkowalczyk300.cryptocurrencyprices.model.room.AlertType
import com.arturkowalczyk300.cryptocurrencyprices.model.room.PriceAlertEntity
import com.arturkowalczyk300.cryptocurrencyprices.model.room.PriceEntity
import java.lang.Thread.sleep

private const val NOTIFICATION_CHANNEL_ID = "my_notification_channel"
private const val NOTIFICATION_ID = 10

private const val INTERVAL = 30000L

class PriceAlertsService : Service() {
    private var priceAlerts: List<PriceAlertEntity>? = null
    private var thread: Thread? = null
    private var running = false
    private lateinit var repository: Repository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        repository = Repository(application)
        observeLiveData()

        try {
            thread = Thread {
                while (running) {
                    sleep(INTERVAL)

                    Log.d("myApp", "price alerts, size=${priceAlerts?.size}")
                    priceAlerts?.forEachIndexed { index, it ->
                        val price = repository.getActualPriceOfCryptocurrencySynchronously(
                            it.cryptocurrencySymbol,
                            "USD"
                        )

                        Log.d(
                            "myApp",
                            "alert number ${index}, current price=$price, alert:$it"
                        )

                        var alertOccured= false
                        if(it.alertType == AlertType.ALERT_WHEN_CURRENT_VALUE_IS_SMALLER){
                            if(price < it.valueThreshold) {
                               alertOccured = true
                                Log.e("myApp", "price is smaller than threshold!")
                            }
                        }
                        else{
                            if(price > it.valueThreshold) {
                                alertOccured = true
                                Log.e("myApp", "price is bigger than threshold!")
                            }
                        }
                    }




                    displayNotification()
                }

            }.also { it.start() }
        } catch (e: Exception) {
            Log.e("myApp", "place1, exc=$e")
        }

        return START_STICKY
    }

    private fun observeLiveData() {
        repository.getPricesAlerts().observeForever {
            priceAlerts = it
        }
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    override fun onCreate() {
        super.onCreate()

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

    private fun buildNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Price alert!")
        .setContentText("Price of currency X has changed!").build()

    private fun displayNotification() {
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
    }
}