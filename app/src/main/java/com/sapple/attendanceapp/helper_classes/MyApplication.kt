package com.sapple.attendanceapp.helper_classes

import android.app.Application
import com.sapple.attendanceapp.receiverclasses.ConnectivityReceiver

class MyApplication: Application() {

    companion object {

        private lateinit var mInstance: MyApplication

        @Synchronized
        fun getInstance(): MyApplication {
            return mInstance
        }
    }

    override fun onCreate() {
        super.onCreate()

        mInstance = this
    }

    fun setConnectivityListener(listener: ConnectivityReceiver.ConnectivityReceiverListener) {
        ConnectivityReceiver.connectivityReceiverListener = listener
    }
}