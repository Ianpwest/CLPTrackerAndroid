package com.example.ianweston.clptrackerandroid

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ListView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_home.*
import org.altbeacon.beacon.*
import org.altbeacon.beacon.MonitorNotifier
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import com.android.volley.AuthFailureError
import com.android.volley.VolleyError




class HomeActivity : BeaconConsumer, AppCompatActivity() {

    private val PERMISSION_REQUEST_COARSE_LOCATION = 1
    private var beaconManager: BeaconManager? = null
    private val TAG = "MonitoringActivity"

    private var bluetoothDeviceNames = ArrayList<BluetoothDeviceModel>()

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                message.setText(R.string.title_home)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                message.setText(R.string.title_dashboard)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                message.setText(R.string.title_notifications)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                var builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle("CLP Tracker must access bluetooth to scan for CLP courses.")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_COARSE_LOCATION)
                }

                builder.show()
            }
        }

        beaconManager = BeaconManager.getInstanceForApplication(this)

        // Detect the main identifier (UID) frame:
        beaconManager!!.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT))

        // Detect the telemetry (TLM) frame:
        beaconManager!!.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT))

        // Detect the URL frame:
        beaconManager!!.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT))


        beaconManager!!.bind(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager!!.unbind(this)
    }

    override fun onBeaconServiceConnect() {
        beaconManager!!.addRangeNotifier(object : RangeNotifier {
            override fun didRangeBeaconsInRegion(beacons: Collection<Beacon>, region: Region) {
                if (beacons.size > 0) {

                    beacons.forEach{
                        if (it.distance <= 3)
                        {
                            logBeacon(it.id1.toString(), it.distance)
                        }
                    }
                }
            }
        })

        beaconManager!!.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region) {
                // logBeaconFindingStatus("Just saw a beacon")
            }

            override fun didExitRegion(region: Region) {
                // logBeaconFindingStatus("I no longer see an beacon")
            }

            override fun didDetermineStateForRegion(state: Int, region: Region) {
                // logBeaconFindingStatus("I have just switched from seeing/not seeing beacons: $state")
            }
        })

        try {
            beaconManager!!.startRangingBeaconsInRegion(Region("CLPTrackerBeaconRangeMonitor", null, null, null))
            beaconManager!!.startMonitoringBeaconsInRegion(Region("CLPTrackerBeaconMonitor", null, null, null))
        }
        catch (e: RemoteException) {
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode)
        {
            PERMISSION_REQUEST_COARSE_LOCATION ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d("Debug", "Coarse location permission granted")
                }
                else
                {
                    var builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle("Functionality limited")
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover CLP courses.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.show()
                }
        }
    }

    fun logBeaconFindingStatus(beaconStatus: String){
        var bluetoothDeviceModel = BluetoothDeviceModel("Device Status", beaconStatus)

        var deviceFound = false
        bluetoothDeviceNames.forEach{
            if(it.deviceName == "Device Status")
            {
                it.deviceDistance = beaconStatus
                deviceFound = true
            }
        }

        if(!deviceFound)
        {
            bluetoothDeviceNames.add(bluetoothDeviceModel)
        }

        val bluetoothDevicesList : ListView = findViewById(R.id.bluetooth_list_view)
        var bluetoothDeviceAdapter = BluetoothDeviceAdapter(this, bluetoothDeviceNames)

        bluetoothDevicesList.adapter = bluetoothDeviceAdapter
    }

    fun logBeacon(beaconName : String, beaconDistance : Double)
    {
        var beaconDistantCalculated = "%.2f".format(beaconDistance) + " m"
        var bluetoothDeviceModel = BluetoothDeviceModel(beaconName, beaconDistantCalculated)

        var deviceFound = false
        bluetoothDeviceNames.forEach{
            if(it.deviceName == beaconName)
            {
                it.deviceDistance = beaconDistantCalculated
                deviceFound = true
            }
        }

        if(!deviceFound)
        {
            bluetoothDeviceNames.add(bluetoothDeviceModel)
        }

        val bluetoothDevicesList : ListView = findViewById(R.id.bluetooth_list_view)
        var bluetoothDeviceAdapter = BluetoothDeviceAdapter(this, bluetoothDeviceNames)

        bluetoothDevicesList.adapter = bluetoothDeviceAdapter

        // Send to database via restful api call
        val queue = Volley.newRequestQueue(this)
        val url = "https://clptracker.azurewebsites.net/api/status/LogUserActivity"

        var simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")

        val deviceModelJSONObject = HashMap<String, String>()
        deviceModelJSONObject["UserId"] = "ian.p.weston@gmail.com"
        deviceModelJSONObject["BeaconIdentifier"] = beaconName
        deviceModelJSONObject["BeaconDistance"] = beaconDistance.toString()
        deviceModelJSONObject["DateReceived"] = simpleDateFormat.format(Date())

        val strReq = object : StringRequest(Request.Method.POST,
            url, Response.Listener { response ->
                logBeaconFindingStatus(response.toString())
            }, Response.ErrorListener { error ->
                logBeaconFindingStatus(error.toString())
            }) {
            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String> {
                return deviceModelJSONObject
            }
        }

        // Add the request to the RequestQueue.
        queue.add(strReq)
    }
}