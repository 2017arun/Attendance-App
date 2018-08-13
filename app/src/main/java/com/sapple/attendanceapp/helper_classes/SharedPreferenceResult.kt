package com.sapple.attendanceapp.helper_classes

import android.content.Context
import android.location.Location
import android.preference.PreferenceManager

class SharedPreferenceResult(private val context: Context, private val location: Location) {

    private fun getLocationText(): String {
        return this.location.latitude.toString() + " " + this.location.longitude.toString()
    }

    fun saveLocation() {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString("LOCATION_UPDATE", getLocationText())
                .apply()
    }

    companion object {
        fun getLocation(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("LOCATION_UPDATE", "")
        }
    }
}