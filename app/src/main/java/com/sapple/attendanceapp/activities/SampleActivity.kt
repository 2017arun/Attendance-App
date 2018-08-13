package com.sapple.attendanceapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.ConnectivityManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.sapple.attendanceapp.R
import com.sapple.attendanceapp.permission.AllPermissions
import com.sapple.attendanceapp.services.LocationUpdateIntentService
import com.sapple.attendanceapp.dataclasses.Login
import com.sapple.attendanceapp.receiverclasses.ConnectivityReceiver
import com.sapple.attendanceapp.helper_classes.ConstantStrings
import com.sapple.attendanceapp.helper_classes.MyApplication
import com.sapple.attendanceapp.helper_classes.SharedPreferenceResult
import com.sapple.attendanceapp.interfaces.RetrofitInterface
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

class SampleActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
                            GoogleApiClient.OnConnectionFailedListener,
                            SharedPreferences.OnSharedPreferenceChangeListener,
                            ConnectivityReceiver.ConnectivityReceiverListener {

    private var check: Boolean = false
    private var activeConnection: Boolean = false
    private var lat: Double = 28.618550
    private var long: Double = 77.389110
    private val defaultLocation = Location("")
    private val currentLocation = Location("")

    private val UPDATE_INTERVAL = 10 * 1000
    private val FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2
    private val MAX_WAIT_TIME = UPDATE_INTERVAL * 2
    private val REQUEST_ID_LOCATION = 1
    private val CAMERA_REQUEST_ID = 100
    private val CAMERA = 101
    private var isPermissionGranted = false
    private var encodedImage: String? = null
    var listOfPermissions = ArrayList<String>()

    private var googleApiClient: GoogleApiClient? = null
    private lateinit var locationRequest: LocationRequest
    var compositeDisposable: CompositeDisposable? = null

    //Bind views using butter knife
    @BindView(R.id.btnPunch) var btn: ImageView? = null

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()

        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)

        val connectivityReceiver = ConnectivityReceiver()
        registerReceiver(connectivityReceiver, intentFilter)

        MyApplication.getInstance().setConnectivityListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        ButterKnife.bind(this)

        defaultLocation.latitude = lat
        defaultLocation.longitude = long

        compositeDisposable = CompositeDisposable()

        val permissions = ArrayList<String>()
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE)
        listOfPermissions = permissions

//        val isOk = checkConnection()

//        if(isOk) {
            isPermissionGranted = AllPermissions.checkAndRequestPermission(this, listOfPermissions, REQUEST_ID_LOCATION)
            if (isPermissionGranted) {
                checkConnection()
            }
//        }
    }

    private fun buildGoogleApiClient() {
        if(googleApiClient != null) {
            return
        }
        googleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .addApi(LocationServices.API)
                .build()
    }

    @SuppressLint("MissingPermission")
    private fun locationUpdate() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, pendingIntent()
        )
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()

        locationRequest.interval = UPDATE_INTERVAL.toLong()

        //for faster update of location
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL.toLong()

        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        //maximum time to get the location update
        locationRequest.maxWaitTime = MAX_WAIT_TIME.toLong()

        locationUpdate()

    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, LocationUpdateIntentService::class.java)
        intent.action = LocationUpdateIntentService.ACTION_PROCESS_UPDATES;
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            REQUEST_ID_LOCATION -> {
                if(grantResults.isNotEmpty()) {

                    var isGranted = false
                    for(i in grantResults) {
                        if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            isGranted = true
                        } else {
                            isGranted = false
                            break
                        }
                    }
                    if(isGranted) {
                        buildGoogleApiClient()
                    } else {
                        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                            showAlertDialog("Permissions are required for this app", REQUEST_ID_LOCATION)
                        } else {
                            permissionsFromSettings("Your need permissions to continue. Do you want to go to app setting?")
                        }
                    }
                }
            }
            CAMERA_REQUEST_ID -> {
                if(grantResults.isNotEmpty()) {

                    var isGranted = false
                    for(i in grantResults) {
                        if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            isGranted = true
                        } else {
                            isGranted = false
                            break
                        }
                    }
                    if(isGranted) {
                        cameraFunctionality()
                    } else {
                        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            showAlertDialog("Permissions are required for this app", CAMERA_REQUEST_ID)
                        } else {
                            permissionsFromSettings("Your need permissions to continue. Do you want to go to app setting?")
                        }
                    }
                }
            }
        }
    }

    private fun showAlertDialog(msg: String, request_id: Int) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(resources.getString(R.string.app_name))
        alertDialog.setMessage(msg)
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK") {
            _, _ -> AllPermissions.checkAndRequestPermission(this, listOfPermissions, request_id)
        }

        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel") {
            _, _ -> alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun permissionsFromSettings(msg: String) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(resources.getString(R.string.app_name))
        alertDialog.setMessage(msg)
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK"
        ) { _, _ -> startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("com.example.sapple.attendanceapp"))) }
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel"
        ) { _, _ -> alertDialog.dismiss() }
        alertDialog.show()
    }

    override fun onConnected(p0: Bundle?) {
        Log.i("GAPIC", "Google APi Client Connected")
        createLocationRequest()
    }

    override fun onConnectionSuspended(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if(key.equals("LOCATION_UPDATE")) {
            val loc = SharedPreferenceResult.getLocation(this).split(" ")
            currentLocation.latitude = loc[0].toDouble()
            currentLocation.longitude = loc[1].toDouble()
            val value = defaultLocation.distanceTo(currentLocation)

            if(value < 50) {
                check = true
                Toast.makeText(this, "$value", Toast.LENGTH_LONG).show()
            } else {
                check = false
            }
        }
    }

    @OnClick(R.id.btnPunch)
        fun captureImage(view: View) {
        val permissions = ArrayList<String>()
        permissions.add(Manifest.permission.CAMERA)
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        listOfPermissions = permissions
        isPermissionGranted = AllPermissions.checkAndRequestPermission(this, listOfPermissions, CAMERA_REQUEST_ID)
            if(isPermissionGranted) {
                cameraFunctionality()
            }
        }

    private fun cameraFunctionality() {
        if(activeConnection) {

            val loc = SharedPreferenceResult.getLocation(this).split(" ")
            currentLocation.latitude = loc[0].toDouble()
            currentLocation.longitude = loc[1].toDouble()
            val value = defaultLocation.distanceTo(currentLocation)

            if(value < 50) {
                val intent = Intent("android.media.action.IMAGE_CAPTURE")
                startActivityForResult(intent, CAMERA)
            } else {
                Toast.makeText(this, "Cannot punch in or punch out as you are outside the specified location",
                        Toast.LENGTH_SHORT).show()
            }

            /*if(check) {
                val intent = Intent("android.media.action.IMAGE_CAPTURE")
                startActivityForResult(intent, CAMERA)
            } else {
                Toast.makeText(this, "Cannot punch in or punch out as you are outside the specified location",
                        Toast.LENGTH_SHORT).show()Toast.makeText(this, "Cannot punch in or punch out as you are outside the specified location",
                Toast.LENGTH_SHORT).show()
            }*/
        } else {
            showSnackbar(activeConnection)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(CAMERA_REQUEST_ID == 100 && resultCode == Activity.RESULT_OK && null != data) {

            val bitmap = data.extras.get("data") as Bitmap

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)
            Toast.makeText(this, "Successfull", Toast.LENGTH_SHORT).show()

            submitData()
        }
    }

    private fun submitData() {
        val requestInterace = Retrofit.Builder()
                .baseUrl(ConstantStrings.BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(RetrofitInterface::class.java)

        val jsonObj = JSONObject()
        jsonObj.put("email", "navjot.singh@sapple.co.in")
        jsonObj.put("encodedImage", encodedImage)

        val jsonParser = JsonParser()
        val jsonObject = jsonParser.parse(jsonObj.toString()) as JsonObject

        compositeDisposable?.add(requestInterace.submitData(jsonObject)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponse, this::handleError))
    }

    private fun handleResponse(response: Login) {
        if(response.status.equals("true")) {
            Toast.makeText(this, "Punch In Successful", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleError(error:Throwable) {
        Toast.makeText(this, "Punch In Falied. Please Retry", Toast.LENGTH_SHORT).show()
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        if(isConnected) {
            activeConnection = isConnected
        } else {
            activeConnection = isConnected
            showSnackbar(isConnected)
        }
    }

    private fun checkConnection(): Boolean {
        val isConnected = ConnectivityReceiver.isConnected()
        showSnackbar(isConnected)
        return isConnected
    }

    private fun showSnackbar(connected: Boolean) {
        val msg: String
        if(!connected) {
            msg = "No Internet Connection"
            val snackbar = Snackbar
                    .make(findViewById<View>(R.id.tv_snack), msg, Snackbar.LENGTH_LONG)

            snackbar.show()
        }
    }
}
