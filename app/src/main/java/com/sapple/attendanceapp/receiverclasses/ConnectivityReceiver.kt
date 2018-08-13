package com.sapple.attendanceapp.receiverclasses

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.sapple.attendanceapp.helper_classes.MyApplication

class ConnectivityReceiver : BroadcastReceiver() {


    companion object {
        lateinit var connectivityReceiverListener: ConnectivityReceiverListener

        fun isConnected(): Boolean {
            val cm = MyApplication.getInstance().applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val activeNetwork = cm.activeNetworkInfo
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {

         val cm = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting

        if(connectivityReceiverListener != null) {
            connectivityReceiverListener.onNetworkConnectionChanged(isConnected)
        }
    }

    interface ConnectivityReceiverListener {
        fun onNetworkConnectionChanged(isConnected: Boolean)
    }
}