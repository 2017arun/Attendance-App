package com.sapple.attendanceapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.telephony.TelephonyManager
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.sapple.attendanceapp.R
import com.sapple.attendanceapp.dataclasses.Login
import com.sapple.attendanceapp.receiverclasses.ConnectivityReceiver
import com.sapple.attendanceapp.helper_classes.ConstantStrings
import com.sapple.attendanceapp.helper_classes.MyApplication
import com.sapple.attendanceapp.interfaces.RetrofitInterface
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sapple.attendanceapp.database.DbWorkerThread
import com.sapple.attendanceapp.database.DbHelper
import com.sapple.attendanceapp.datamodel.LoginData
import com.sapple.attendanceapp.permission.AllPermissions
import com.sapple.attendanceapp.receiverclasses.NotificationReceiver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class LoginActivity : AppCompatActivity(), View.OnClickListener,
        ConnectivityReceiver.ConnectivityReceiverListener {

    private lateinit var etUserId: EditText
    private lateinit var etPassword: EditText
    private lateinit var imgLogo: ImageView
    private lateinit var loginBtn: Button
    private var dbHelper: DbHelper? = null
    private lateinit var mDbWorkerThread: DbWorkerThread
    private val mUiHandler = Handler()
    private var string = "Arun"

    private var userId: String? = null
    private var password: String? = null
    private var imei: String? = null
    private var PHONE_STATE_REQUEST_ID = 1
    private var activeConnection: Boolean = false

    private var isPermissionGranted = false
    private var listOfPermissions = ArrayList<String>()

    private var compositeDisposable: CompositeDisposable? = null

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
        setContentView(R.layout.activity_login)

        createNotification()

        initViews()

        mDbWorkerThread = DbWorkerThread("dbWorkerThread")
        mDbWorkerThread.start()
        dbHelper = DbHelper.getInstance(this)

        //set font style
        val tf = Typeface.createFromAsset(this.assets, "fonts/OpenSansLight.ttf")
        etUserId.typeface = tf
        etPassword.typeface = tf
        loginBtn.typeface = tf

        setAnimation()

        imgLogo.setOnClickListener(this)
        loginBtn.setOnClickListener(this)

        compositeDisposable = CompositeDisposable()

        val permissions = ArrayList<String>()
        permissions.add(Manifest.permission.READ_PHONE_STATE)
        listOfPermissions = permissions

        isPermissionGranted = AllPermissions.checkAndRequestPermission(this, listOfPermissions, PHONE_STATE_REQUEST_ID)
        if (isPermissionGranted) {
            getImeiNumber()
        }
    }

    private fun createNotification() {
        val list = mutableListOf(58,60)
        for(i in list) {
            val calendar1 = Calendar.getInstance()
            calendar1.set(Calendar.HOUR_OF_DAY, 17)
            calendar1.set(Calendar.MINUTE, i)
            calendar1.set(Calendar.SECOND, 0)

            val notifyIntent = Intent(this, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(this, 2, notifyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar1.timeInMillis,
                    1000 * 60 * 24 * 24, pendingIntent)
        }
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun getImeiNumber() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            telephonyManager.imei
        } else {
            telephonyManager.deviceId
        }
        Toast.makeText(this, imei, Toast.LENGTH_SHORT).show()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            PHONE_STATE_REQUEST_ID -> {
                if (grantResults.isNotEmpty()) {
                    var isGranted = false
                    for (i in grantResults) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            isGranted = true
                        } else {
                            isGranted = false
                            break
                        }
                    }
                    if (isGranted) {
                        getImeiNumber()
                    } else {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                            showAlertDialog("Permissions are required for this app", PHONE_STATE_REQUEST_ID)
                        } else {
                            permissionsFromSettings("Your need permissions to continue. Do you want to go to app setting?")
                        }
                    }
                }
            }
        }
    }

    //initialize all the views
    private fun initViews() {
        etPassword = findViewById<View>(R.id.et_password) as EditText
        etUserId = findViewById<View>(R.id.et_userId) as EditText

        imgLogo = findViewById<View>(R.id.img_logo) as ImageView
        loginBtn = findViewById<View>(R.id.login_btn) as Button
    }

    //Load animation
    private fun setAnimation() {
        val slideUp = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
        etUserId.startAnimation(slideUp)
        etPassword.startAnimation(slideUp)
        loginBtn.startAnimation(slideUp)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.login_btn -> {
                userId = etUserId.text.toString()
                password = etPassword.text.toString()
                if ( activeConnection ) {
                    loginServiceTask()
//                    val intent = Intent(applicationContext, SampleActivity::class.java)
//                    startActivity(intent)
                } else {
                    showSnackBar(activeConnection)
                }
            }
        }
    }

    private fun loginServiceTask() {
        val requestInterface = Retrofit.Builder()
                .baseUrl(ConstantStrings.BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(RetrofitInterface::class.java)

        val jsonObj = JSONObject()
        jsonObj.put("username", userId)
        jsonObj.put("password", password)
        jsonObj.put("imei", imei)

        val jsonParser = JsonParser()
        val jsonObject = jsonParser.parse(jsonObj.toString()) as JsonObject

        compositeDisposable?.add(requestInterface.getData(jsonObject)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponse, this::handleError))
    }

    private fun handleResponse(response: Login) {
        if ( response.status.equals("true") ) {
            val loginData = LoginData()
            loginData.userId = userId
            loginData.imeiNumber = imei
//            dbHelper?.attendanceDao()?.insertEmployeeLoginData(loginData)
            val task = Runnable { dbHelper?.attendanceDao()?.insertEmployeeLoginData(loginData) }
            mDbWorkerThread.postTask(task)

            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
            val intent = Intent(applicationContext, SampleActivity::class.java)
            startActivity(intent)
        } else {
            Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleError(error: Throwable) {
        Toast.makeText(this, "Login Failed $error", Toast.LENGTH_SHORT).show()
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        if ( isConnected ) {
            activeConnection = isConnected
        } else {
            activeConnection = isConnected
            showSnackBar(isConnected)
        }
    }

    private fun showSnackBar(connected: Boolean) {
        val msg: String
        if(!connected) {
            msg = "No Internet Connection"
            val snackBar = Snackbar
                    .make(findViewById<View>(R.id.tv_snack), msg, Snackbar.LENGTH_LONG)
            snackBar.show()
        }
    }

    override fun onDestroy() {
        DbHelper.destroyInstance()
        mDbWorkerThread.quit()
        super.onDestroy()
        compositeDisposable?.clear()
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
        ) { _, _ -> startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("com.sapple.attendanceapp"))) }
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel"
        ) { _, _ -> alertDialog.dismiss() }
        alertDialog.show()
    }
}
