package com.imaginadesarrollo.universalhrm.manager.garmin

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQDevice
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.manager.HrmConnection

class GarminConnectionImpl(private val context: Context,
                           private val callback: HrmCallbackMethods): HrmConnection {

    private lateinit var adapter: GarminDeviceAdapter
    private var dialogCallback: HrmConnection.AlertDialogCallback? = null
    private var sdkReady = false

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

                (context as Activity).runOnUiThread{
                    callback.setHeartRateMonitorName(deviceName)
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