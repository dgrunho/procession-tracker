package com.softinsa.sf2019_tracker

import android.Manifest
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.getDrawable
import kotlinx.android.synthetic.main.activity_main.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    companion object {
        private const val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2200
    }

    var map: MapView? = null

    val GPS_PERMISSION_ID = 42
    var restServiceMobile: RestServiceMobile = RestServiceMobile()
    var restServiceWeb: RestServiceWeb = RestServiceWeb()


    private val uiHelper = UiHelper()

    var routes: MutableList<Route>? = null


    var myService: MyService? = null
    var isBound = false

    var pts: ArrayList<GeoPoint>? = null

    var frt: Timer = Timer()

    var currentProcessionID: Int = 0

    private val PermissionsRequestCode = 123
    private lateinit var managePermissions: ManagePermissions


    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            val binder = service as MyService.MyLocalBinder
            myService = binder.getService()
            isBound = true

            getBackgroundNotification(applicationContext, myService).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }


    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var ctx = getApplicationContext()
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_main)




        map = findViewById(R.id.map111) as MapView
        map!!.setTileSource(TileSourceFactory.MAPNIK)
        map!!.setBuiltInZoomControls(true)
        map!!.setMultiTouchControls(true)

        var list = listOf<String>()

        if (Build.VERSION.SDK_INT >= 28) {
            list = listOf<String>(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.WAKE_LOCK
            )
        } else {
            list = listOf<String>(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK
            )
        }

        managePermissions = ManagePermissions(this, list, PermissionsRequestCode)

        if (managePermissions.checkPermissions() == PackageManager.PERMISSION_GRANTED) {
            FinishLoad()
        }


    }

    public override fun onResume() {
        super.onResume()
        map!!.onResume()
    }


    fun FinishLoad() {

        frt = fixedRateTimer(
            name = "update-timer",
            initialDelay = 1000, period = 1000
        ) {
            this@MainActivity.runOnUiThread {
                paintMap()
            }
        }


        //wakeLock.release()


        //spinner_route!!.setOnItemSelectedListener(this)

        radio_group.setOnCheckedChangeListener(
            RadioGroup.OnCheckedChangeListener { _, _ ->
                EnableButtons()
            })

        btn_starttracking.setOnClickListener {
            showTokenDialog("start", " para iniciar")
            //start_tracking_Click()
        }
        btn_pausetracking.setOnClickListener {
            showTokenDialog("pause", " para pausar")
            //pause_tracking_Click()
        }
        btn_stoptracking.setOnClickListener {
            showTokenDialog("stop", " para terminar")
        }

        //getRoutes()

        if (!uiHelper.isPlayServicesAvailable(this)) {
            Toast.makeText(this, "Play Services did not installed!", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Do the task now
        serviceClass = MyService::class.java
        serviceIntent = Intent(applicationContext, serviceClass)
        if (!isServiceRunning(serviceClass!!)) {
            startService(serviceIntent)
            bindService(serviceIntent, myConnection, Context.BIND_AUTO_CREATE)
        } else {
            Toast.makeText(applicationContext, "Service already running.", Toast.LENGTH_SHORT).show()
            bindService(serviceIntent, myConnection, Context.BIND_AUTO_CREATE)
        }

        showTokenDialog("login", "")
    }

    var serviceClass: Class<*>? = null
    var serviceIntent: Intent? = null

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PermissionsRequestCode -> {
                val isPermissionsGranted = managePermissions
                    .processPermissionsResult(requestCode, permissions, grantResults)

                if (isPermissionsGranted) {
                    FinishLoad()
                    Toast.makeText(applicationContext, "Permissions granted.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "Permissions denied.", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        map!!.onPause()
    }

    public override fun onDestroy() {
        super.onDestroy()
        frt.cancel()
        val serviceClass = MyService::class.java
        val serviceIntent = Intent(applicationContext, serviceClass)
        try {
            unbindService(myConnection)
        } catch (e: IllegalArgumentException) {

        }
        if (isServiceRunning(serviceClass)) {
            stopService(serviceIntent)
        } else {
            Toast.makeText(applicationContext, "Service already stopped.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun GetProcessionPoints(): ArrayList<GeoPoint>? {
        if (myService!!.selected_route != null) {
            if (myService!!.selected_procession != null) {
                var geoPoints: ArrayList<GeoPoint> = ArrayList()
                myService!!.selected_procession?.forEach({
                    geoPoints.add(GeoPoint(it.position!!.lat!!, it.position!!.lng!!))
                })
                return geoPoints
            }
        }
        return null
    }

    private fun checkPermission(vararg perm: String): Boolean {
        val havePermissions = perm.toList().all {
            checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (!havePermissions) {
            if (perm.toList().any {
                    ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }
            ) {
                /*val dialog = AlertDialog.Builder(this)
                    .setTitle("Permission")
                    .setMessage("Permission needed!")
                    .setPositiveButton("OK", {id, v ->
                        ActivityCompat.requestPermissions(
                            this, perm, GPS_PERMISSION_ID)
                    })
                    .setNegativeButton("No", {id, v -> })
                    .create()
                dialog.show()
            } else {*/
                ActivityCompat.requestPermissions(this, perm, GPS_PERMISSION_ID)
            }
            return false
        }
        return true
    }


    private fun getRoutes() {
        if (routes == null) {
            val call = restServiceMobile.service.getRoutes()
            call.enqueue(object : Callback<MutableList<Route>> {
                override fun onResponse(call: Call<MutableList<Route>>, response: Response<MutableList<Route>>) {
                    if (response.code() == 200) {
                        routes = response.body()

                        val list_of_items: ArrayList<String> = ArrayList()
                        routes?.forEach {
                            list_of_items.add(it.name.toString())
                        }

                        initProcession()
                        //val aa = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, list_of_items)
                        //aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        //spinner_route!!.setAdapter(aa)

                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Erro a retornar Cortejos: " + response.raw().toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<MutableList<Route>>, error: Throwable) {
                    Toast.makeText(
                        this@MainActivity,
                        "Erro a retornar Cortejos: " + error.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    private fun ProcessionFinish() {
        if (myService!!.selected_route != null) {
            val call = restServiceWeb.service.postLocation(
                LocationSpec(
                    myService!!.selected_route!!.identifier,
                    Position(0.0, 0.0),
                    ""
                )
            )
            call.enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {

                }

                override fun onFailure(call: Call<String>, error: Throwable) {

                }
            })
        }

    }

    var line: Polyline? = null
    var routeArea: BoundingBox? = null

    override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, id: Long) {
        myService!!.selected_route = routes!![position]
        EnableButtons();

        map!!.overlay.clear()

        var geoPoints: ArrayList<GeoPoint> = ArrayList()
        myService!!.selected_route!!.coordinates?.forEach({
            geoPoints.add(GeoPoint(it[0], it[1]))
        })
        line = Polyline()
        line!!.setPoints(geoPoints)

        myService!!.selected_procession = null

        routeArea = computeArea(geoPoints).increaseByScale(1.1f)
        map!!.zoomToBoundingBox(routeArea, false)


        paintMap()
    }

    fun initProcession() {
        routes!!.forEach {
            if (it.identifier == currentProcessionID.toString()) {
                myService!!.selected_route = it

            }
        }

        EnableButtons();

        map!!.overlay.clear()

        var geoPoints: ArrayList<GeoPoint> = ArrayList()
        myService!!.selected_route!!.coordinates?.forEach({
            geoPoints.add(GeoPoint(it[0], it[1]))
        })
        line = Polyline()
        line!!.setPoints(geoPoints)

        myService!!.selected_procession = null

        routeArea = computeArea(geoPoints).increaseByScale(1.1f)
        map!!.zoomToBoundingBox(routeArea, false)


        paintMap()
    }


    // Method to draw a mark
    fun MapView.mark(
        point: GeoPoint,
        icon: Drawable? = null,
        title: String? = null,
        textLabelBackgroundColor: Int? = null,
        textLabelFontSize: Int? = null,
        textLabelForegroundColor: Int? = null,
        onClick: (Marker) -> Unit = {},
        doOnCreate: (Marker) -> Unit = {}
    ) {
        val marker = Marker(this).apply {
            setInfoWindow(MarkerInfoWindow(R.layout.map_info_window, this@mark))
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon?.let { setIcon(it) }
            title?.let { setTitle(it) }
            textLabelBackgroundColor?.let { setTextLabelBackgroundColor(it) }
            textLabelFontSize?.let { setTextLabelFontSize(it) }
            textLabelForegroundColor?.let { setTextLabelForegroundColor(it) }
            setOnMarkerClickListener { marker, _ ->
                onClick(marker)
                true
            }
        }
        overlays.add(marker)
        doOnCreate(marker)
        //invalidate()
    }


    fun paintMap() {
        map!!.overlays.clear()
        map!!.invalidate()



        try {
            pts = GetProcessionPoints()
            if (myService!!.isGettingProcession == false) {
                pts = GetProcessionPoints()
            }

        } catch (e: Exception) {

        }
        try {
            if (line != null && routeArea != null) {
                map!!.getOverlays().remove(line)
                line!!.paint.strokeJoin = Paint.Join.ROUND
                line!!.paint.strokeCap = Paint.Cap.ROUND
                map!!.getOverlays().add(line)

            }

        } catch (e: Exception) {

        }

        try {
            if (pts != null) {

                var lineProcession: Polyline? = Polyline()
                lineProcession!!.setPoints(pts)
                lineProcession.color = Color.RED
                lineProcession.width = 20.0f
                lineProcession.paint.strokeJoin = Paint.Join.ROUND
                lineProcession.paint.strokeCap = Paint.Cap.ROUND

                map!!.getOverlays().remove(lineProcession)

                map!!.getOverlays().add(lineProcession)
            }

        } catch (e: Exception) {

        }

        try {
            if (myService!!.currentPoint != null) {
                map!!.mark(
                    myService!!.currentPoint!!,
                    getDrawable(this, R.drawable.ic_markerc),
                    "",
                    Color.RED,
                    Color.RED,
                    Color.RED,
                    {},
                    {})
            }
        } catch (e: Exception) {
        }



        try {
            if (pts != null) {
                map!!.mark(
                    pts!![pts!!.count() - 1],
                    getDrawable(this, R.drawable.ic_markerfr),
                    "",
                    Color.RED,
                    Color.RED,
                    Color.RED,
                    {},
                    {})
            }
        } catch (e: Exception) {
        }

        try {
            if (pts != null) {
                map!!.mark(
                    pts!![0],
                    getDrawable(this, R.drawable.ic_markertr),
                    "",
                    Color.RED,
                    Color.RED,
                    Color.RED,
                    {},
                    {})
            }
        } catch (e: Exception) {
        }

        map!!.invalidate()
    }

    fun computeArea(points: ArrayList<GeoPoint>): BoundingBox {

        var nord = 0.0
        var sud = 0.0
        var ovest = 0.0
        var est = 0.0

        for (i in 0 until points.size) {
            if (points[i] == null) continue

            val lat = points[i].latitude
            val lon = points[i].longitude

            if (i == 0 || lat > nord) nord = lat
            if (i == 0 || lat < sud) sud = lat
            if (i == 0 || lon < ovest) ovest = lon
            if (i == 0 || lon > est) est = lon

        }

        return BoundingBox(nord, est, sud, ovest)

    }

    override fun onNothingSelected(arg0: AdapterView<*>) {

    }

    private fun start_tracking_Click() {

        //wakeLock.acquire()

        if (rb_rear.isChecked) {
            myService!!.processionPosition = "tail"
        } else {
            myService!!.processionPosition = "head"
        }

        myService!!.post_position = true

        //spinner_route.isEnabled = false
        rb_front.isEnabled = false
        rb_rear.isEnabled = false

        btn_starttracking.visibility = View.GONE
        btn_pausetracking.visibility = View.VISIBLE
        btn_stoptracking.visibility = View.VISIBLE
        Toast.makeText(this@MainActivity, "Acompanhamento Iniciado", Toast.LENGTH_SHORT).show()
    }

    private fun pause_tracking_Click() {
        //wakeLock.release()
        myService!!.post_position = false
        btn_starttracking.visibility = View.VISIBLE
        btn_pausetracking.visibility = View.GONE
        btn_stoptracking.visibility = View.VISIBLE
        Toast.makeText(this@MainActivity, "Acompanhamento pausado", Toast.LENGTH_SHORT).show()
    }

    private fun stop_tracking_Click() {
        myService!!.post_position = false
        ProcessionFinish()
        //spinner_route.isEnabled = true
        rb_front.isEnabled = true
        rb_rear.isEnabled = true
        rb_front.isChecked = false
        rb_rear.isChecked = false

        btn_starttracking.visibility = View.GONE
        btn_pausetracking.visibility = View.GONE
        btn_stoptracking.visibility = View.GONE
        Toast.makeText(this@MainActivity, "Cortejo Terminado", Toast.LENGTH_SHORT).show()
    }

    private fun EnableButtons() {
        btn_starttracking.visibility = View.GONE
        btn_pausetracking.visibility = View.GONE
        btn_stoptracking.visibility = View.GONE
        if (myService!!.selected_route != null && (rb_front.isChecked || rb_rear.isChecked)) {
            btn_starttracking.visibility = View.VISIBLE
            //if (rb_rear.isChecked){
            //    btn_stoptracking.visibility = View.VISIBLE
            //}
        }
    }


    override fun onBackPressed() {

        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Sair")
            .setMessage("Tem a certeza que quer sair ?")
            .setPositiveButton(android.R.string.ok, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    //this.moveTaskToBack(true)
                    myService!!.post_position = false
                    finish()
                }
            })
            .setNegativeButton(android.R.string.cancel, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    dialog.cancel()
                }
            })
        val dialog = builder.create()
        //Disable outside touch cancellation
        dialog.setCanceledOnTouchOutside(false)
        //Disable back button
        dialog.setCancelable(false)
        dialog.show()


    }


    fun showTokenDialog(action: String, message: String) {
        val context = this
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Introduzir Token" + message)

        // https://stackoverflow.com/questions/10695103/creating-custom-alertdialog-what-is-the-root-view
        // Seems ok to inflate view with null rootView
        val view = layoutInflater.inflate(R.layout.dialog_token, null)

        val tokenInput = view.findViewById(R.id.tokenInput) as EditText

        builder.setView(view)

        // set up the ok button
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            val token = tokenInput.text.toString()
            val processionID = TokenManager().getProcession(token)
            if (processionID != 0) {
                if (currentProcessionID == 0 || currentProcessionID == processionID) {
                    if (action == "login") {
                        currentProcessionID = processionID
                        getRoutes()
                    }
                    if (action == "start") {
                        start_tracking_Click()
                    }
                    if (action == "pause") {
                        pause_tracking_Click()
                    }
                    if (action == "stop") {
                        stop_tracking_Click()
                    }
                }

            }
        }

        var str = "Cancelar"
        if (action == "login") {
            str = "Sair"
        }
        builder.setNegativeButton(str) { dialog, _ ->
            if (action == "login") {
                myService!!.post_position = false
                finish()
            }
            dialog.cancel()
        }
        builder.setOnDismissListener {
            if (action == "login") {
                if (currentProcessionID == 0) {
                    showTokenDialog(action, "")
                }
            }
        }

        builder.show()
    }

}

