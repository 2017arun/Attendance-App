package com.sapple.attendanceapp.helper_classes

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.sapple.attendanceapp.R

class PermissionUtils {
    companion object {
        private const val REQUEST_PERMISSION_SETTING = 101
        private var isPermissionGrant = false
        private var stopRecursiveCallSecondTimePermission = true
        private var sentToSetting = false

        /**
         * this method check permission int three stage
         * 1) if app is install first time and then ask for required permission
         *    to run the app and in that case return false.
         * 2) if first time someone deny the any one of the permission or all the permission
         *    and in that case return false.
         * 3) if app permission deny and clicked on checkbox
         *   (the msg mention on that "don't ask again") and in that case return false.
         * 4) if all the permission is granted then return true.
         * @param activity point to current activity
         * @param permissionList list of permission
         * @param PERMISSION_CONSTANT specific number constant to permission
         * @return if all permission is granted then return true else false
         */
        fun permissionGranted(activity: Activity, permissionList: Array < String >, PERMISSION_CONSTANT: Int) {
            val permissionStatus : SharedPreferences = activity.getSharedPreferences("permissionStatus", MODE_PRIVATE)
            var isPermissionNotGranted = false
            if ( permissionList.isNotEmpty() ) {
                permissionList.forEach {
                    val isTrue = ActivityCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
                    if ( isTrue ) {
                        isPermissionNotGranted = true
                    }
                }

                if ( isPermissionNotGranted ) {
                    val isPermissionGranted = isShouldShowRequestPermissionRationale(activity, permissionList)
                    if ( isPermissionGranted ) {
                        //Show Information about why you need the permission
                        if ( stopRecursiveCallSecondTimePermission )
                        secondTimePermission(activity, permissionList, PERMISSION_CONSTANT)
                    }
                    else if ( permissionStatus.all.isNotEmpty() && !sentToSetting ) {
                        for ( permissionName in permissionList ) {
                            if ( permissionStatus.getBoolean(permissionName, false) ) {
                                // Previously Permission Request was cancelled with Don't Ask Again',
                                // Redirect to Settings after showing Information about why you need the permission
                                val builder = AlertDialog.Builder(activity)
                                builder.setTitle(R.string.app_name)
                                builder.setMessage("This app are need these permissions.")
                                builder.setPositiveButton("Grant") { dialog, _ ->
                                    dialog.cancel()
                                    openSettings(activity)
                                }
                                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                                builder.show()
                                break
                            }
                        }
                    }  else {
                        if ( !sentToSetting ) {
                            ActivityCompat.requestPermissions(activity, permissionList, PERMISSION_CONSTANT)
                        }
                    }
                    val editor = permissionStatus.edit()
                    for ( permission in permissionList ) {
                        editor.putBoolean(permission, true)
                    }
                    editor.commit()
                    setGrant(false)
                } else {
                    //You already have the permission, just go ahead.
                    proceedAfterPermission(activity)
                    setGrant(true)
                }
            }
        }

        /**
         * this method is used if someone first time deny the permission
         * all or any one of that permission then android will automatically
         * display default permission dialog box provided by android
         * @param activity point to current activity
         * @param permissionList list of permission
         * @param PERMISSION_CONSTANT specific number constant to permission
         */
        fun secondTimePermission(activity: Activity, permissionList: Array<String>, PERMISSION_CONSTANT: Int) {
            stopRecursiveCallSecondTimePermission = false
            ActivityCompat.requestPermissions(activity, permissionList, PERMISSION_CONSTANT)
        }

        /**
        * this method is use when someone first time deny the permission
        * and this is called to check permission is granted or not
        * @param activity point to current activity
        * @param permissionList list of permission
        * @return if all permission is not granted then return true else false
        */
        fun isShouldShowRequestPermissionRationale(activity: Activity, permissionList: Array<String>): Boolean {
            if ( permissionList.isNotEmpty() ) {
                for ( permission in permissionList ) {
                    val isPermissionTrue = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                    if ( isPermissionTrue ) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * this method check current android version of tab
         * if current android version is Marshmallow or above this
         * then its return true else false
         */
        fun isAndroidVersionMorHigher(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        }

        /**
         * this method is called after when all permission is granted to show the msg
         * @param activity point current activity
         */
        fun proceedAfterPermission(activity: Activity) {
            //We've got the permission, now we can proceed further
            val msg = "We got the required Permissions"
            toast(activity, msg)
        }

        fun isGranted(): Boolean {
            return isPermissionGrant
        }

        fun setGrant(b: Boolean) {
            isPermissionGrant = b
        }

        /**
         * show the current msg that we want to display
         * @param activity point to current Activity
         * @param msg point to current msg
         */
        private fun toast(activity: Activity, msg: String) {
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        }

        /**
         * this method is used to open setting where we will give permission to app manually
         * that necessary for the app. this method is called when someone deny the permission
         * and clicked on the checked (in which mention don't ask again)
         * @param activity point to current activity
         */
        fun openSettings(activity: Activity) {
            sentToSetting = true
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivityForResult(intent, REQUEST_PERMISSION_SETTING)
            val msg = "Go to Setting to grant permission"
            toast(activity, msg)
        }
    }
}