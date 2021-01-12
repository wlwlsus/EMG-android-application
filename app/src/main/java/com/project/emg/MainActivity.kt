package com.project.emg

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager


class MainActivity : AppCompatActivity(), FragmentManager.OnBackStackChangedListener{
    init {
        Log.e("init","MainActivity")
    }
    var btAdapter: BluetoothAdapter? = null
    private val REQUEST_ENABLE_BT = 1

    var pairedDevices: Set<BluetoothDevice>? = null
    var btArrayAdapter: ArrayAdapter<String>? = null
    var deviceAddressArray: ArrayList<String>? = null

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