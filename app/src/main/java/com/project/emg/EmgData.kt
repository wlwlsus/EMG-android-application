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
    var floatTemp = 0F
    var dataIndex = 0
    var globalTimerCounter : Timer? = null

    fun getEmgValue() {
        try{
            emgDataList = emgDataStr.split(",") as MutableList<String>
            Log.e("데이터 인덱스", dataIndex.toString())

//            if(emgDataList[0] == "" && emgDataList.size > 0){
//                Log.e("데이터 1번","$emgDataList")
//            }

            if(emgDataList.size == 0 || emgDataList.size < dataIndex){
                Log.e("데이터 배열1", emgDataList.size.toString())
                return
            }
            else{
                floatTemp = try {
                    Log.e("데이터 배열 크기", emgDataList.size.toString())
                    emgDataList[dataIndex].toFloat()
                }catch (e : Exception){
                    120F
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
        dataIndex = 0
    }

    fun timerOff() {
        if (globalTimerCounter != null) {
            globalTimerCounter!!.cancel()
        }
    }


}