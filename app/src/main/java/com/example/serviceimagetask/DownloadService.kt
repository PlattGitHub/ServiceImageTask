package com.example.serviceimagetask

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.net.URL

/**
 * Simple [Service] subclass that downloads image from URL.
 *
 * @author Alexander Gorin
 */
class DownloadService : Service() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var connectivityManager: ConnectivityManager

    var bitmap: Bitmap? = null
        private set

    private val mIBinder = LocalBinder()
    private val resultIntent = Intent(ACTION_DOWNLOAD)

    private var isDownloadCancelled = false
    private var isNetworkAvailable = false
    private var imageURL: String = ""

    override fun onCreate() {
        super.onCreate()

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationBuilder = NotificationCompat.Builder(this, createNotificationChannel())

        registerReceiver(connectivityReceiver, IntentFilter(ACTION_CONNECTIVITY))
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mIBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isNetworkAvailable) startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(connectivityReceiver)
    }

    fun downloadImage(imageURL: String) {
        this.imageURL = imageURL
        if (isNetworkAvailable) {
            Thread {
                bitmap =
                    BitmapFactory.decodeStream((URL(imageURL)).openConnection().getInputStream())
                if (!isConnectedToNetwork()) {
                    isDownloadCancelled = true
                    resultIntent.putExtra(ACTION_SUCCESS, false)
                }
                sendBroadcast(resultIntent)
                stopForeground(true)
            }.start()
        } else {
            isDownloadCancelled = true
        }
    }

    private fun downloadImage() {
        startForeground(NOTIFICATION_ID, createNotification())
        Thread {
            bitmap = BitmapFactory.decodeStream((URL(imageURL)).openConnection().getInputStream())
            if (!isConnectedToNetwork()) {
                isDownloadCancelled = true
                resultIntent.putExtra(ACTION_SUCCESS, false)
            }
            sendBroadcast(resultIntent)
            stopForeground(true)
        }.start()
    }

    private fun createNotification(): Notification {
        notificationBuilder
            .setSmallIcon(R.drawable.ic_file_download)
            .setContentTitle(getString(R.string.file_is_downloading))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setProgress(0, 0, true)
        return notificationBuilder.build()
    }

    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.download_channel_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            notificationManager.createNotificationChannel(channel)
            return CHANNEL_ID
        }
        return ""
    }

    private fun isConnectedToNetwork(): Boolean {
        val activeNetwork = connectivityManager.activeNetworkInfo ?: return false
        return activeNetwork.isConnected
    }

    /**
     * [BroadcastReceiver] that triggers when action [DownloadService.ACTION_CONNECTIVITY] occurs.
     * Registered/Unregistered in onCreate/onDestroy.
     *
     * @author Alexander Gorin
     */
    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context?.let {
                if (!isConnectedToNetwork()) {
                    isNetworkAvailable = false
                    resultIntent.putExtra(ACTION_SUCCESS, false)
                } else {
                    resultIntent.putExtra(ACTION_SUCCESS, true)
                    isNetworkAvailable = true
                }

                if (isDownloadCancelled && isNetworkAvailable) {
                    downloadImage()
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        val instance: DownloadService
            get() = this@DownloadService
    }

    companion object {
        private const val NOTIFICATION_ID = 111
        private const val CHANNEL_ID = "CHANNEL_ID"
        const val ACTION_DOWNLOAD = "com.example.serviceimagetask.ACTION_DOWNLOAD"
        const val ACTION_SUCCESS = "com.example.serviceimagetask.ACTION_SUCCESS"
        private const val ACTION_CONNECTIVITY = "android.net.conn.CONNECTIVITY_CHANGE"
    }
}