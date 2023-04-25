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
import com.arturkowalczyk300.cryptocurrencyprices.model.room.PriceAlertEntity
import com.arturkowalczyk300.cryptocurrencyprices.model.room.PriceEntity
import com.github.mikephil.charting.utils.Utils.init
import java.lang.Thread.sleep

private const val NOTIFICATION_CHANNEL_ID = "my_notification_channel"
private const val NOTIFICATION_ID = 10

private const val INTERVAL = 30000L

class PriceAlertsService : Service() {
    private var data: List<PriceAlertEntity>? = null
    private var thread: Thread? = null
    private var running = false
    private lateinit var repository: Repository

    init {
        Log.d("myApp/service", "init")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("myApp/service", "start!")
        running = true
        Toast.makeText(this, "Service started", Toast.LENGTH_LONG).show()

        repository = Repository(application)
        subscribeLiveDataOfPrice()

        try {
            thread = Thread {
                while (running) {
                    sleep(INTERVAL)
                    Log.d("myApp/service", "tick, collection size=${data?.size}")
                    displayNotification()
                }

            }.also { it.start() }
        } catch (e: Exception) {
            Log.e("myApp", "place1, exc=$e")
        }

        Log.d("myApp/service", "end of start() method!")
        return START_STICKY
    }

    private fun subscribeLiveDataOfPrice() {
        repository.getPricesAlerts().observeForever(){
            data = it
        }
    }

    override fun onDestroy() {
        running = false
        Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show()
        Log.d("myApp/service", "service is being killed!")
        super.onDestroy()
    }

    //notifications section
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

    private fun displayNotification(){
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
    }
}