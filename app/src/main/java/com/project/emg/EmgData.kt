package com.project.emg

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.TextView
import java.lang.Exception
import java.util.*
import kotlin.concurrent.timer

object EmgData {

    var emgDataStr = ""
    var emgDataList = mutableListOf<String>()
    var emgIntArray = mutableListOf<Int>()
    var floatTemp = 0F
    var dataIndex = 0

    fun getEmgValue() {
        try{
            emgDataList = emgDataStr.split(",") as MutableList<String>

            if(emgDataList.size == 0 || emgDataList.size < dataIndex){
                return
            }

            else{
                try {
                    emgIntArray.add(emgDataList[dataIndex].toInt())
                    floatTemp = emgDataList[dataIndex].toFloat()

                }catch (e : Exception){
                    emgIntArray.add(emgDataList[dataIndex].toInt())
                    floatTemp = 120F
                }

            }
        }catch (e : Exception){
            e.printStackTrace()
        }
    }

    fun plusDataIndex(){
        dataIndex++
    }

    fun resetDataIndex(){
        emgIntArray = mutableListOf()
        dataIndex = 0
    }
}