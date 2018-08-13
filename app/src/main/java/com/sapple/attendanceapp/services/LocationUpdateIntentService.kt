package com.sapple.attendanceapp.services

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationResult
import com.sapple.attendanceapp.helper_classes.SharedPreferenceResult

class LocationUpdateIntentService: IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        if(intent != null) {
            val action = intent.action
            if(ACTION_PROCESS_UPDATES == action) {
                val locationResult = LocationResult.extractResult(intent)
                if(locationResult != null) {
                    val listOfLocations = locationResult.locations
                    val firstLocation = listOfLocations[0]
                    val sharedPreferenceResult = SharedPreferenceResult(this, firstLocation)
                    sharedPreferenceResult.saveLocation()
                    Log.i("Latitude", firstLocation.latitude.toString())
                    Log.i("Longitude", firstLocation.longitude.toString())
                }
            }
        }
    }

    companion object {

        internal val ACTION_PROCESS_UPDATES = "com.appsgit.locationupdate.retrofit.action" + ".PROCESS_UPDATES"

        private val TAG = LocationUpdateIntentService::class.java.simpleName
    }
}