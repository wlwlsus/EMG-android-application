package com.project.emg

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.*
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
    private var sendText: TextView? = null
    private var hexWatcher: TextUtil.HexWatcher? = null
    private var connected = Connected.False
    private var initialStart = true
    private var hexEnabled = false
    private var pendingNewline = false
    private var newline: String = TextUtil.newline_crlf

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
            activity!!.runOnUiThread { connect() }
        }
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        service = (binder as SerialService.SerialBinder).service
        service?.attach(this)
        Log.e("check", "onServiceConnected $initialStart")
        Log.e("check", "onServiceConnected $isResumed")
        if (initialStart && isResumed) {
            initialStart = false
            activity!!.runOnUiThread { connect() }
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
        (receiveText as TextView).setTextColor(resources.getColor(R.color.colorRecieveText)) // set as default color to reduce number of spans
        (receiveText as TextView).movementMethod = ScrollingMovementMethod.getInstance()
        sendText = view.findViewById(R.id.send_text)
        hexWatcher = TextUtil.HexWatcher(sendText as TextView)
        hexWatcher!!.enable(hexEnabled)
        (sendText as TextView).addTextChangedListener(hexWatcher)
        (sendText as TextView).hint = if (hexEnabled) "HEX mode" else ""
        val sendBtn = view.findViewById<View>(R.id.send_btn)
        sendBtn.setOnClickListener { send((sendText as TextView).text.toString()) }

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_terminal, menu)
        menu.findItem(R.id.hex).isChecked = hexEnabled
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clear -> {
                receiveText!!.text = ""
                true
            }
            R.id.newline -> {
                val newlineNames = resources.getStringArray(R.array.newline_names)
                val newlineValues = resources.getStringArray(R.array.newline_values)
                val pos = listOf(*newlineValues).indexOf(newline)
                val builder = AlertDialog.Builder(activity)
                builder.setTitle("Newline")
                builder.setSingleChoiceItems(newlineNames, pos) { dialog: DialogInterface, item1: Int ->
                    newline = newlineValues[item1]
                    dialog.dismiss()
                }
                builder.create().show()
                true
            }
            R.id.hex -> {
                hexEnabled = !hexEnabled
                sendText!!.text = ""
                hexWatcher?.enable(hexEnabled)
                sendText!!.hint = if (hexEnabled) "HEX mode" else ""
                item.isChecked = hexEnabled
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    /*
     * Serial + UI
     */
    private fun connect() {
        try {
            Log.e("check", "connect")
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            status("연결 중 입니다...\n")
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
            Toast.makeText(activity, "블루투스와 연결되지 않았습니다.", Toast.LENGTH_SHORT).show()
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
        Log.e("", "")
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
            Log.e("CharSequence", (receiveChar as String))
            receiveText?.append(receiveChar);
        }
    }

    private fun status(str: String) {
        val spn = SpannableStringBuilder(
                """
        $str
        """.trimIndent())
            spn.setSpan(ForegroundColorSpan(resources.getColor(R.color.black)), 0, spn.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            receiveText!!.append(spn)
    }

    /*
     * SerialListener
     */
    override fun onSerialConnect() {
        status("연결 되었습니다.\n")
        connected = Connected.True
    }

    override fun onSerialConnectError(e: Exception?) {
        if (e != null) {
            status("연결 실패: " + e.message)
        }
        disconnect()
    }

    override fun onSerialRead(data: ByteArray?) {
        receive(data!!)
    }

    override fun onSerialIoError(e: Exception?) {
        if (e != null) {
            status("연결 끊김: " + e.message)
        }
        disconnect()
    }
}
