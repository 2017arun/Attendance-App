package com.sapple.attendanceapp.receiverclasses

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sapple.attendanceapp.services.NotificationIntentService

class NotificationReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        val i = Intent(context, NotificationIntentService::class.java)
        context!!.startService(i)
    }
}