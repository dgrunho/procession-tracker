package com.softinsa.sf2019_tracker

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import com.google.android.gms.location.*
import org.osmdroid.util.GeoPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.fixedRateTimer

class MyService : Service() {

    private val myBinder = MyLocalBinder()

    private val uiHelper = UiHelper()

    private lateinit var locationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var restServiceWeb: RestServiceWeb = RestServiceWeb()
    private var restServiceMobile: RestServiceMobile = RestServiceMobile()

    var selected_route: Route? = null
    var post_position: Boolean = false
    var currentPoint: GeoPoint? = null

    var selected_procession: MutableList<Procession>? = null
    var processionPosition = "head"
    var isPosting: Boolean = false
    var isUpdating: Boolean = false

    var frt: Timer = Timer()



    override fun onBind(intent: Intent): IBinder? {
        return myBinder
    }


    inner class MyLocalBinder : Binder() {
        fun getService(): MyService {
            return this@MyService
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startPooling()

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        frt.cancel()
        stopForeground(true)
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        frt.cancel()
        stopSelf()
    }

    fun startPooling() {
        createLocationCallback()
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = uiHelper.getLocationRequest()

        frt = fixedRateTimer(
            name = "update-timer",
            initialDelay = 5000, period = 5000
        ) {
            getProcession()
        }

        //

        requestLocationUpdate()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdate() {
        if (uiHelper.isLocationProviderEnabled(this))
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                if (locationResult!!.lastLocation == null) return



                if (selected_route != null) {
                    PostLocation(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
                    if (processionPosition == "head") {
                        currentPoint =
                            GeoPoint(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
                        if (post_position) {
                            PostPosition(currentPoint!!.latitude, currentPoint!!.longitude)
                        }
                    } else {
                        currentPoint =
                            GeoPoint(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
                        if (post_position) {
                            PostPosition(currentPoint!!.latitude, currentPoint!!.longitude)
                        }
                    }
                }
            }
        }
    }

    var isGettingProcession: Boolean = false
    private fun getProcession() {
        isGettingProcession = true
        //Toast.makeText(applicationContext,"Ping", Toast.LENGTH_SHORT).show()
        val call = restServiceMobile.service.getProcessions()
        call.enqueue(object : Callback<MutableList<MutableList<Procession>>> {
            override fun onResponse(
                call: Call<MutableList<MutableList<Procession>>>,
                response: Response<MutableList<MutableList<Procession>>>
            ) {
                if (response.code() == 200) {
                    var processions = response.body()
                    processions?.forEach {
                        if (it.count() >= 0) {
                            if (it[0].identifier == selected_route?.identifier) {
                                selected_procession = it
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Erro a ir buscar as procissões: " + response.raw().toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                isGettingProcession = false
            }

            override fun onFailure(call: Call<MutableList<MutableList<Procession>>>, error: Throwable) {
                isGettingProcession = false
                Toast.makeText(
                    applicationContext,
                    "Erro a ir buscar as procissões: " + error.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

    }


    private fun PostPosition(latitude: Double, longitide: Double) {

        if (post_position) {
            if (selected_route != null) {
                if (isPosting != true) {
                    isPosting = true
                    //Toast.makeText(applicationContext,"Ping", Toast.LENGTH_SHORT).show()

                    val dtstart: Date = Date()

                    val call = restServiceWeb.service.postLocation(
                        LocationSpec(
                            selected_route!!.identifier,
                            Position(latitude, longitide),
                            processionPosition
                        )
                    )

                    call.enqueue(object : Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            Toast.makeText(applicationContext,"Pong", Toast.LENGTH_SHORT).show()
                            isPosting = false
                            val dtstartEnd: Date = Date()
                            /*Toast.makeText(
                                applicationContext,
                                (dtstartEnd.time - dtstart.time).toString(),
                                Toast.LENGTH_SHORT
                            ).show()*/
                            //updateRoute()
                        }

                        override fun onFailure(call: Call<String>, error: Throwable) {
                            Toast.makeText(
                                applicationContext,
                                "Erro a ir escrever as procissões: " + error.message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                            isPosting = false
                            val dtstartEnd: Date = Date()
                            /*Toast.makeText(
                                applicationContext,
                                (dtstartEnd.time - dtstart.time).toString(),
                                Toast.LENGTH_SHORT
                            ).show()*/
                        }
                    })
                }

            }

        }

    }

    private fun PostLocation(latitude: Double, longitide: Double) {
        val deviceID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val call = restServiceWeb.service.postLocationWithID(
            LocationWithIDSpec(
                Position(latitude, longitide),
                deviceID
            )
        )
        val dtstart: Date = Date()


        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                //Toast.makeText(applicationContext,"Pong", Toast.LENGTH_SHORT).show()
                //isPosting = false
                //updateRoute()
                val dtstartEnd: Date = Date()
                /*Toast.makeText(
                    applicationContext,
                    (dtstartEnd.time - dtstart.time).toString(),
                    Toast.LENGTH_SHORT
                ).show()*/
            }

            override fun onFailure(call: Call<String>, error: Throwable) {
                Toast.makeText(
                    applicationContext,
                    "Erro a ir escrever a posição: " + error.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
                //isPosting = false
            }
        })
    }


    private fun updateRoute() {
        if (post_position) {
            if (selected_route != null) {
                if (isUpdating == false) {
                    isUpdating = true

                    val call = restServiceWeb.service.updateRoute(RouteId(selected_route!!.identifier))
                    call.enqueue(object : Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            isUpdating = false

                        }

                        override fun onFailure(call: Call<String>, error: Throwable) {
                            isUpdating = false
                            Toast.makeText(
                                applicationContext,
                                "Erro a fazer update as procissões: " + error.message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                }
            }
        }
    }
}
