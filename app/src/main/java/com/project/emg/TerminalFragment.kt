package com.project.emg

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.project.emg.EmgData.dataIndex
import com.project.emg.EmgData.emgDataStr
import com.project.emg.EmgData.floatTemp
import com.project.emg.EmgData.getEmgValue
import com.project.emg.EmgData.plusDataIndex
import com.project.emg.EmgData.resetDataIndex
import kotlinx.android.synthetic.main.fragment_terminal.*
import java.util.*


@Suppress("DEPRECATION")
class TerminalFragment : Fragment(), ServiceConnection, SerialListener {
    init {
        Log.e("init", "TerminalFragment")
    }
    private enum class Connected {
        False, Pending, True
    }

    private var deviceAddress: String? = null
    private var service: SerialService? = null
    private var receiveText: TextView? = null
    private var lineChart : LineChart? = null
    private var sendText: TextView? = null
    private var hexWatcher: TextUtil.HexWatcher? = null
    private var connected = Connected.False
    private var initialStart = true
    private var hexEnabled = false
    private var pendingNewline = false
    private var newline: String = TextUtil.newline_crlf
    private var thread = Thread()

    /*
     * Lifecycle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
        deviceAddress = arguments!!.getString("device")

    }

    override fun onDestroy() {
        if (connected != Connected.False) disconnect()
        activity!!.stopService(Intent(activity, SerialService::class.java))
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        if (service != null) (service as SerialService).attach(this) else activity!!.startService(Intent(activity, SerialService::class.java)) // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    override fun onStop() {
        if (service != null && !activity!!.isChangingConfigurations) (service as SerialService).detach()
        super.onStop()
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        getActivity()!!.bindService(Intent(getActivity(), SerialService::class.java), this, Context.BIND_AUTO_CREATE)
    }

    override fun onDetach() {
        try {
            activity!!.unbindService(this)
        } catch (ignored: Exception) {
        }
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()
        Log.e("check", "onResume")
        Log.e("check", "$initialStart")
        Log.e("check", "$service")

        if (initialStart && service != null) {
            initialStart = false
            receive_btn.setOnClickListener {
                resetDataIndex()
                setChart()
                activity!!.runOnUiThread { connect() }
            }
        }
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        service = (binder as SerialService.SerialBinder).service
        service?.attach(this)
        Log.e("check", "onServiceConnected $initialStart")
        Log.e("check", "onServiceConnected $isResumed")
        if (initialStart && isResumed) {
            initialStart = false
            receive_btn.setOnClickListener {
                resetDataIndex()
                setChart()
                activity!!.runOnUiThread { connect() }
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        service = null
    }

    /*
     * UI
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.fragment_terminal, container, false)
        receiveText = view.findViewById(R.id.receive_text) // TextView performance decreases with number of spans
        lineChart = view.findViewById(R.id.lineChart)
        (receiveText as TextView).setTextColor(resources.getColor(R.color.colorRecieveText)) // set as default color to reduce number of spans
        (receiveText as TextView).movementMethod = ScrollingMovementMethod.getInstance()
        //sendText = view.findViewById(R.id.send_text)
        //hexWatcher = TextUtil.HexWatcher(sendText as TextView)
        //hexWatcher!!.enable(hexEnabled)
        //(sendText as TextView).addTextChangedListener(hexWatcher)
        //(sendText as TextView).hint = if (hexEnabled) "HEX mode" else ""
        //val sendBtn = view.findViewById<View>(R.id.send_btn)
        //sendBtn.setOnClickListener { send((sendText as TextView).text.toString()) }
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_terminal, menu)
        //menu.findItem(R.id.hex).isChecked = hexEnabled
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clear -> {
                receiveText!!.text = ""
                true
            }
//            R.id.newline -> {
//                val newlineNames = resources.getStringArray(R.array.newline_names)
//                val newlineValues = resources.getStringArray(R.array.newline_values)
//                val pos = listOf(*newlineValues).indexOf(newline)
//                val builder = AlertDialog.Builder(activity)
//                builder.setTitle("Newline")
//                builder.setSingleChoiceItems(newlineNames, pos) { dialog: DialogInterface, item1: Int ->
//                    newline = newlineValues[item1]
//                    dialog.dismiss()
//                }
//                builder.create().show()
//                true
//            }
//            R.id.hex -> {
//                hexEnabled = !hexEnabled
//                sendText!!.text = ""
//                hexWatcher?.enable(hexEnabled)
//                sendText!!.hint = if (hexEnabled) "HEX mode" else ""
//                item.isChecked = hexEnabled
//                true
//            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    /**
     * Graph
     */
    private fun setChart(){
        val xAxis : XAxis = lineChart!!.xAxis

        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            //textSize = 15f
            //granularity = 1f
            //axisMinimum = 0f
            setDrawLabels(false)
            isGranularityEnabled = false
            setDrawGridLines(false)
        }
        lineChart?.apply {
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 60f
            axisLeft.axisMaximum = 200f
            legend.apply {
                textSize = 15f
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                setDrawMarkers(false)
            }
        }

        val lineData = LineData()
        lineChart!!.data = lineData
        feedMultiple()
    }

    // Get Data from UI Thread
    private fun feedMultiple(){
        if(thread != null){
            thread.interrupt()
        }

        val runnable = Runnable {
            addEntry()
        }

        thread = Thread {
            while (true) {
                if(activity == null)
                    return@Thread
                activity!!.runOnUiThread(runnable)
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
        thread.start()
    }

    private fun addEntry(){
        val data = lineChart!!.data

        // line Chart
        data?.let{
            var set : ILineDataSet? = data.getDataSetByIndex(0)

            // 임의의 데이터셋
            if (set == null){
                set = createSet()
                data.addDataSet(set)
            }
            if(dataIndex <= 300){
                getEmgValue()
                data.addEntry(Entry(set.entryCount.toFloat(), floatTemp), 0)
                plusDataIndex()
            }


            // 데이터 엔트리 추가 Entry(x값, y값)
            data.notifyDataChanged()
            lineChart!!.apply{
                notifyDataSetChanged()
                moveViewToX(data.entryCount.toFloat())
                setVisibleXRangeMaximum(80f)
                setPinchZoom(false)
                isDoubleTapToZoomEnabled = false
                description.text = "시간"
                description.textSize = 7F
                setBackgroundColor(resources.getColor(R.color.white))
                setExtraOffsets(8f, 16f, 8f, 16f)
            }
        }
    }

    private fun createSet() : LineDataSet {
        val set = LineDataSet(null, "근전도 진폭")

        set.apply {
            axisDependency = YAxis.AxisDependency.LEFT
            color = resources.getColor(R.color.colorRecieveText)

            //setCircleColor(R.color.colorRecieveText)
            valueTextSize = 10F
            lineWidth = 1f
            //circleRadius = 3f
            fillAlpha = 0
            fillColor = resources.getColor(R.color.colorRecieveText)
            highLightColor = Color.BLACK
            setDrawCircles(false)
            valueTextColor = Color.WHITE;
            setDrawValues(true)
        }
        return set
    }


    /*
     * Serial + UI
     */
    private fun connect() {
        try {
            // 데이터 초기화
            emgDataStr = ""

            Log.e("check", "connect")
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            status("연결 중 입니다...")
            connected = Connected.Pending
            val socket = SerialSocket(activity!!.applicationContext, device)
            service?.connect(socket)
        } catch (e: Exception) {
            onSerialConnectError(e)
        }
    }

    private fun disconnect() {
        connected = Connected.False
        service?.disconnect()
    }

    private fun send(str: String) {
        if (connected != Connected.True) {
            Toast.makeText(activity, "기기와 연결되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val msg: String
            val data: ByteArray
            if (hexEnabled) {
                val sb = StringBuilder()
                TextUtil.toHexString(sb, TextUtil.fromHexString(str))
                TextUtil.toHexString(sb, newline.toByteArray())
                msg = sb.toString()
                data = TextUtil.fromHexString(msg)
            } else {
                msg = str
                data = (str + newline).toByteArray()
            }
            val spn = SpannableStringBuilder("""
    $msg
    
    """.trimIndent())
            spn.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorSendText)), 0, spn.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            receiveText!!.append(spn)
            service?.write(data)
        } catch (e: Exception) {
            onSerialIoError(e)
        }
    }

    private fun receive(data: ByteArray) {
        val receiveChar: CharSequence
        if (hexEnabled) {
            receiveText!!.append(TextUtil.toHexString(data) + '\n')
        } else {
            var msg = String(data)
            if (newline == TextUtil.newline_crlf && msg.isNotEmpty()) {
                // don't show CR as ^M if directly before LF
                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf)
                // special handling if CR and LF come in separate fragments
                if (pendingNewline && msg[0] == '\n') {
                    val edt = receiveText!!.editableText
                    if (edt != null && edt.length > 1) edt.replace(edt.length - 2, edt.length, "")
                }
                pendingNewline = msg[msg.length - 1] == '\r'
            }
            receiveChar = TextUtil.toCaretString(msg, newline.isNotEmpty())
            //Log.e("CharSequence", (receiveChar as String))
            //Log.e("CharSequence", (receiveText?.length().toString()))
            //Log.e("체크 데이터 : ", receiveChar.toString())

            if(emgDataStr.length > 1500){
                Log.e("데이터 String 길이", emgDataStr.length.toString())
                getEmgValue()
                resultMessage()
                disconnect()
            }
            //receiveText?.append(receiveChar)
            emgDataStr += receiveChar
        }
    }

    private fun resultMessage(){
        val random = Random()
        val num = random.nextInt(10)
        if(num > 5) receiveText!!.text = "예"
        else receiveText!!.text = "아니오"
    }



    private fun status(str: String) {
        val spn = SpannableStringBuilder(
                """
        $str
        """.trimIndent())
            spn.setSpan(ForegroundColorSpan(resources.getColor(R.color.black)), 0, spn.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            //receiveText!!.append(spn)
            receiveText!!.text = spn

    }

    /*
     * SerialListener
     */
    override fun onSerialConnect() {
        status("분석 중 입니다..")
        connected = Connected.True
    }

    override fun onSerialConnectError(e: Exception?) {
        if (e != null) {
//            status("연결 실패: " + e.message)
            status("연결 실패 : 올바른 기기가 아닙니다.")
        }
        disconnect()
    }

    override fun onSerialRead(data: ByteArray?) {
        val timer = Timer()
        status("분석 중 입니다.")
        //emgDataStr = ""
        receive(data!!)
    }

    override fun onSerialIoError(e: Exception?) {
        if (e != null) {
            status("연결 끊김")
        }
        disconnect()
    }
}
