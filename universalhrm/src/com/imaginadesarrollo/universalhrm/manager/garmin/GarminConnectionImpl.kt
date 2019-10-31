package com.imaginadesarrollo.universalhrm.manager.garmin

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.Toast
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.R
import com.imaginadesarrollo.universalhrm.manager.HrmConnection

class GarminConnectionImpl(private val context: Context,
                           private val callback: HrmCallbackMethods): HrmConnection {

    private lateinit var adapter: GarminDeviceAdapter
    private var dialogCallback: HrmConnection.AlertDialogCallback? = null
    private var sdkReady = false
    private var iqApp: IQApp = IQApp(MY_APP)
    private var appIsOpen = false

    private val listener = object : ConnectIQ.ConnectIQListener {

        override fun onInitializeError(errStatus: ConnectIQ.IQSdkErrorStatus) {
            sdkReady = false
        }

        override fun onSdkReady() {
            sdkReady = true
            loadDevices()
        }

        override fun onSdkShutDown() {
            sdkReady = false
        }

    }
    private val mConnectIQ = ConnectIQ.getInstance(context, ConnectIQ.IQConnectType.WIRELESS).apply {
        initialize(context, true, listener)
    }

    private fun loadDevices(){
        val devices = mConnectIQ.knownDevices
        scannedDevices.addAll(devices)
    }



    override fun getAdapter(): ArrayAdapter<*> {
        // this call means that we should start looking for data
        adapter = GarminDeviceAdapter(context)
        handleReset()
        return adapter
    }

    protected fun handleReset() {
        //Release the old access if it exists
        scannedDevices.clear()
    }

    override fun disconnect() {
        dialogCallback?.close()
        callback.onDeviceDisconnected()
    }

    override fun addAlertDialogCallback(callback: HrmConnection.AlertDialogCallback) {
        dialogCallback = callback
    }

    /**
     * Requests access to the given search result.
     * @param asyncScanResultDeviceInfo The search result to attempt to connect to.
     */
    protected fun requestConnectToResult(device: IQDevice){
        mConnectIQ.registerForDeviceEvents(device) { device, status ->
            // Since we will only get updates for this device, just display the status
            //mDeviceStatus.setText(status.name());
        }

        // Let's check the status of our application on the device.
        try {
            mConnectIQ.getApplicationInfo(MY_APP, device, object : ConnectIQ.IQApplicationInfoListener {

                override fun onApplicationInfoReceived(app: IQApp) {
                    // Send a message to open the app
                    try {
                        Toast.makeText(context, "Opening app...", Toast.LENGTH_SHORT).show()
                        mConnectIQ.openApplication(
                                device,
                                app
                        ) { _, _, status ->
                            appIsOpen = (status == ConnectIQ.IQOpenApplicationStatus.APP_IS_ALREADY_RUNNING)
                        }
                    } catch (ex: Exception) {
                    }
                }

                override fun onApplicationNotInstalled(applicationId: String) {
                    // The Comm widget is not installed on the device so we have
                    // to let the user know to install it.
                    val dialog = AlertDialog.Builder(context)
                    dialog.setTitle(R.string.missing_widget)
                    dialog.setMessage(R.string.missing_widget_message)
                    dialog.setPositiveButton(android.R.string.ok, null)
                    dialog.create().show()
                }

            })
        } catch (e1: InvalidStateException) {
        } catch (e1: ServiceUnavailableException) {
        }

        // Let's register to receive messages from our application on the device.
        try {
            mConnectIQ.registerForAppEvents(device, iqApp) { device, app, message, status ->
                // We know from our Comm sample widget that it will only ever send us strings, but in case
                // we get something else, we are simply going to do a toString() on each object in the
                // message list.
                val receivedText = StringBuilder().run {
                    message.forEach { str -> append(str.toString()) }
                    toString()
                }

                val textToInt = Integer.parseInt(receivedText.replace("HR2: ",""))
                callback.setHeartRateValue(textToInt)
            }
        } catch (e: InvalidStateException) {
            Toast.makeText(context, "ConnectIQ is not in a valid state", Toast.LENGTH_SHORT).show()
        }
    }

    val scannedDevices = mutableListOf<IQDevice>()
    inner class GarminDeviceAdapter(context: Context): ArrayAdapter<IQDevice>(context, 0, scannedDevices){

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val listItem = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_single_choice, parent, false)
            val deviceName = scannedDevices[position].friendlyName
            val address = scannedDevices[position].deviceIdentifier.toString()


            listItem.findViewById<CheckedTextView>(android.R.id.text1).text = deviceName

            listItem.setOnClickListener {

                val device: IQDevice = scannedDevices[position]
                requestConnectToResult(device)
                dialogCallback?.close()

                (context as Activity).runOnUiThread{
                    callback.setHeartRateMonitorName(deviceName.capitalize())
                    callback.setHeartRateMonitorProviderName("Garmin")
                    callback.setHeartRateMonitorAddress(address)
                }
            }

            return listItem
        }
    }


    companion object{

        const val IQDEVICE = "IQDevice"
        const val MY_APP = "99c56bf44c8d4a6a87d2960bb6f80b73"

        const val TAG = "GarminConnectionImpl"
    }
}