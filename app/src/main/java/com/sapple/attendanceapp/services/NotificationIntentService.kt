package com.sapple.attendanceapp.services

import android.app.IntentService
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat
import com.sapple.attendanceapp.R
import com.sapple.attendanceapp.activities.LoginActivity
import java.util.*

class NotificationIntentService: IntentService("") {

    companion object {
        const val NOTIFICATION_ID = 3
    }

    override fun onHandleIntent(intent: Intent?) {

        val nb = Notification.Builder(this)
        nb.setContentTitle("Punch Missing")

        nb.setContentText("You have missed the punch")
        nb.setSmallIcon(R.drawable.password_icon)

        val notifyIntent = Intent(this, LoginActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this,2, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        nb.setContentIntent(pendingIntent)
        val notificationCompat = nb.build()
        val notificationManagerCompat = NotificationManagerCompat.from(this)
        notificationManagerCompat.notify((Date().time / 1000L % Integer.MAX_VALUE).toInt(), notificationCompat)

    }
}