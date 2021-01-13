package com.project.emg

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.project.emg.EmgData.emgDataStr
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_terminal.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch


@Suppress("UNCHECKED_CAST")
class MainActivity : AppCompatActivity(), FragmentManager.OnBackStackChangedListener{
    init {
        Log.e("init", "MainActivity")
    }
    //Real Time Graph
//    var chart: LineChart? = null
//    var X_RANGE = 50
//    var DATA_RANGE = 30
//
//    var xVal: ArrayList<Map.Entry<*, *>>? = null
//    var setXcomp: LineDataSet? = null
//    var xVals: ArrayList<String>? = null
//    var lineDataSets: ArrayList<ILineDataSet>? = null
//    var lineData: LineData? = null

    private var floatTemp = 36.5F

    var btAdapter: BluetoothAdapter? = null
    private val REQUEST_ENABLE_BT = 1

    var pairedDevices: Set<BluetoothDevice>? = null
    var btArrayAdapter: ArrayAdapter<String>? = null
    var deviceAddressArray: ArrayList<String>? = null
    private var thread = Thread()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        // show paired devices
        //showPairedDevice()

        // 권한 체크
        permissionCheck()

        // Enable bluetooth
        startBluetooth()

        //val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportFragmentManager.addOnBackStackChangedListener(this)

        if (savedInstanceState == null) supportFragmentManager.beginTransaction().add(R.id.fragment, DevicesFragment(), "devices").commit() else onBackStackChanged()


    }
//
//    private fun init(){
//        chart = findViewById(R.id.chart)
//        chartInit()
//    }
//
//    private fun chartInit() {
//        chart!!.isAutoScaleMinMaxEnabled = true
//        xVal = ArrayList()
//        setXcomp = LineDataSet(xVal as List<Entry>, "X")
//        setXcomp!!.color = Color.RED
//        setXcomp!!.setDrawValues(false)
//        setXcomp!!.setDrawCircles(false)
//        setXcomp!!.axisDependency = YAxis.AxisDependency.LEFT
//        lineDataSets = ArrayList()
//        lineDataSets!!.add(setXcomp!!)
//
//        xVals = ArrayList()
//        for (i in 0 until X_RANGE) {
//            xVals!!.add("")
//        }
//
//        lineData = LineData(xVal, lineDataSets)
//        chart!!.data = lineData
//        chart!!.invalidate()
//    }
//
//    fun chartUpdate(x: Int) {
//        if (xVal!!.size > DATA_RANGE) {
//            xVal!!.removeAt(0)
//            for (i in 0 until DATA_RANGE) {
//
//            }
//        }
//        xVal!!.add(Entry(x.toFloat(), xVal!!.size.toFloat()) as Map.Entry<*, *>)
//        setXcomp!!.notifyDataSetChanged()
//        chart!!.notifyDataSetChanged()
//        chart!!.invalidate()
//    }
//
//    var handler: Handler = object : Handler(Looper.getMainLooper()) {
//        override fun handleMessage(msg: Message) {
//            if (msg.what == 0) { // Message id 가 0 이면
//                var a = 0
//                a = (Math.random() * 100).toInt()
//                chartUpdate(a)
//            }
//        }
//    }
//
//    internal class MyThread : Thread() {
//        override fun run() {
//            while (true) {
//                MainActivity().handler.sendEmptyMessage(0)
//                try {
//                    sleep(50)
//                } catch (e: InterruptedException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//    }
//
//    private fun threadStart() {
//        val thread = MyThread()
//        thread.isDaemon = true
//        thread.start()
//    }

    private fun permissionCheck(){
        // Get permission
        val permissionList = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )

        ActivityCompat.requestPermissions(this@MainActivity, permissionList, 1)
    }

    private fun startBluetooth(){
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!(btAdapter as BluetoothAdapter).isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun showPairedDevice(){
        btArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        deviceAddressArray = ArrayList()
        //listview.adapter = btArrayAdapter
    }

    fun onClickButtonPaired(view: View?) {
        btArrayAdapter!!.clear()
        if (deviceAddressArray != null && !deviceAddressArray!!.isEmpty()) {
            deviceAddressArray!!.clear()
        }
        pairedDevices = btAdapter!!.bondedDevices
        if ((pairedDevices as MutableSet<BluetoothDevice>?)!!.size > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (device in (pairedDevices as MutableSet<BluetoothDevice>?)!!) {
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                btArrayAdapter!!.add(deviceName)
                deviceAddressArray!!.add(deviceHardwareAddress)
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackStackChanged() {
        supportActionBar!!.setDisplayHomeAsUpEnabled(supportFragmentManager.backStackEntryCount > 0)
    }
}