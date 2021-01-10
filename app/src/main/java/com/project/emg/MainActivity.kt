package com.project.emg

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_example.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    var btAdapter: BluetoothAdapter? = null
    private val REQUEST_ENABLE_BT = 1

    var pairedDevices: Set<BluetoothDevice>? = null
    var btArrayAdapter: ArrayAdapter<String>? = null
    var deviceAddressArray: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        // show paired devices
        showPairedDevice()

        // 권한 체크
        permissionCheck()

        // Enable bluetooth
        startBluetooth()

    }

    private fun permissionCheck(){
        // Get permission
        val permissionList = arrayOf<String>(
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
        listview.adapter = btArrayAdapter
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
}