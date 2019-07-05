package com.softinsa.sf2019_tracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build

class getBackgroundNotification(private val context: Context, private var myService: MyService?) :
    AsyncTask<Long, Void, Any>() {

    lateinit var mNotification: Notification
    private val mNotificationId: Int = 1000

    override fun doInBackground(vararg params: Long?): Any? {

        //Create Channel
        createChannel(context)

        var notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifyIntent = Intent(context, MainActivity::class.java)

        val title = "Tracking Position"
        //val message = "You have received a sample notification. This notification will take you to the details page."

        notifyIntent.putExtra("title", title)
        //notifyIntent.putExtra("message", message)
        notifyIntent.putExtra("notification", true)

        notifyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK


        val pendingIntent = PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            mNotification = Notification.Builder(context, CHANNEL_ID)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_app_icon)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setTimeoutAfter(0)
                //.setStyle(Notification.BigTextStyle()
                //.bigText(message))
                //.setContentText(message)
                .build()
        } else {

            mNotification = Notification.Builder(context)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_app_icon)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle(title)
                //.setStyle(Notification.BigTextStyle()
                //.bigText(message))
                //.setContentText(message)
                .build()

        }

        myService?.startForeground(999, mNotification)

        return null
    }


    companion object {
        const val CHANNEL_ID = "samples.notification.smarterfest.com.CHANNEL_ID"
        const val CHANNEL_NAME = "Sample Notification"
    }

    private fun createChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
            notificationChannel.enableVibration(false)
            notificationChannel.setShowBadge(false)
            notificationChannel.enableLights(false)
            notificationChannel.lightColor = Color.parseColor("#e8334a")
            notificationChannel.description = ""
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            notificationManager.createNotificationChannel(notificationChannel)
        }

    }
}