package com.project.emg

import android.util.Log

interface SerialListener {
    fun onSerialConnect()
    fun onSerialConnectError(e: Exception?)
    fun onSerialRead(data: ByteArray?)
    fun onSerialIoError(e: Exception?)
}
