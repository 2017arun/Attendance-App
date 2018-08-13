package com.sapple.attendanceapp.permission

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

class AllPermissions {

    companion object {
        fun checkAndRequestPermission(activity: Activity, listOfPermissions: ArrayList<String>,
                                      requestCode: Int): Boolean {
            val localListOfPermissions = ArrayList<String>()
            for((i, value) in listOfPermissions.withIndex()) {
                val temp = ContextCompat.checkSelfPermission(activity, listOfPermissions[i])
                if(temp != PackageManager.PERMISSION_GRANTED) {
                    localListOfPermissions.add(value)
                }
            }

            return if(!listOfPermissions.isEmpty()) {
                ActivityCompat.requestPermissions(activity, listOfPermissions.toTypedArray(), requestCode)
                false
            } else {
                true
            }
        }
    }
}