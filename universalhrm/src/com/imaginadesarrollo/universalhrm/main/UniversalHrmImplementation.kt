package com.imaginadesarrollo.universalhrm.main

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.R
import com.imaginadesarrollo.universalhrm.manager.HRDeviceRef
import com.imaginadesarrollo.universalhrm.manager.HRManager
import com.imaginadesarrollo.universalhrm.manager.HRProvider
import java.util.*

internal class UniversalHrmImplementation(private val activity: Activity): HrmImplementation, HRProvider.HRClient {

    companion object {
        const val TAG = "UniversalHrm"
    }

    private val callback: HrmCallbackMethods by lazy { activity as HrmCallbackMethods }
    private val deviceAdapter: DeviceAdapter by lazy { DeviceAdapter(activity) }
    private val handler: Handler by lazy { Handler() }

    private var btName: String = ""
    private var btAddress: String = ""
    private var btProviderName: String = ""
    private var hrProviderSelected = false

    private val providers: List<HRProvider>
    private lateinit var hrProvider: HRProvider


    init {
        btName = retrieveDeviceName()
        btAddress = retrieveDeviceAddress()
        btProviderName = retrieveProviderName()

        providers = HRManager.getHRProviderList(activity)
        if(providers.isEmpty()){
            // Does not have Bluetooth or Ant+ capabilities
            callback.deviceNotSupported()
        }

        load()
        open()
    }

    override fun scan() {
        scanAction()
    }

    override fun connect() {connect2()}

    override fun disconnect() {
        Log.d(TAG, hrProvider.providerName + ".disconnect()")
        hrProvider.disconnect()
        callback.onDeviceDisconnected()
        return
    }

    override fun isConnected(): Boolean {
        return (::hrProvider.isInitialized && (hrProvider.isConnecting || hrProvider.isConnected))
    }

    override fun isThereSavedDevice(): Boolean { return btAddress.isNotBlank()}

    /** MAIN FRAGMENT IMPLEMENTATION **/

    private fun startScan(hrProvider: HRProvider, deviceAdapter: DeviceAdapter) {
        Log.d(hrProvider.providerName, ".startScan()")
        deviceAdapter.deviceList.clear()
        hrProvider.startScan()
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(activity.getString(R.string.Scanning))
        builder.setPositiveButton(activity.getString(R.string.Connect),
                object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        scanConnect()
                        dialog.dismiss()
                    }
                })
        if (hrProvider.isBondingDevice) {
            builder.setNeutralButton("Pairing", object : DialogInterface.OnClickListener {

                override fun onClick(dialog: DialogInterface, which: Int) {
                    dialog.cancel()
                    val i = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    activity.startActivityForResult(i, 123)
                }
            })
        }
        builder.setNegativeButton(activity.getString(R.string.Cancel),
                object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        scanCancel()
                        dialog.dismiss()
                    }
                })

        builder.setSingleChoiceItems(deviceAdapter, -1,
                object : DialogInterface.OnClickListener {
                    override fun onClick(arg0: DialogInterface, arg1: Int) {
                        val hrDevice = deviceAdapter.deviceList[arg1]
                        //presenter.setDeviceName(hrDevice.name)
                        btName = hrDevice.name
                        btAddress = hrDevice.address
                        btProviderName = hrDevice.provider
                        //callback.setHeartRateMonitorName(hrDevice.name)
                        //presenter.setDeviceAddress(hrDevice.address)
                       // callback.setHeartRateMonitorAddress(hrDevice.address)
                    }
                })
        builder.show()
    }

    private fun selectProvider() {
        val items = arrayOfNulls<CharSequence>(providers.size)
        val itemNames = arrayOfNulls<CharSequence>(providers.size)
        for (i in items.indices) {
            items[i] = providers.get(i).getProviderName()
            itemNames[i] = providers.get(i).getName()
        }
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(activity.getString(R.string.Select_type_of_Bluetooth_device))
        builder.setPositiveButton(activity.getString(R.string.OK)
        ) { dialog, which -> open() }
        builder.setNegativeButton(activity.getString(R.string.Cancel)
        ) { dialog, which ->
            mIsScanning = false
            load()
            open()
            dialog.dismiss()
        }
        builder.setSingleChoiceItems(itemNames, -1
        ) { arg0, arg1 ->
            /*hrProvider = HRManager.getHRProvider(this@HRSettingsActivity,
                    items[arg1].toString())*/
            hrProvider = HRManager.getHRProvider(activity,
                    items[arg1].toString())
            hrProviderSelected = true
            Log.d(TAG, "hrProvider = " + hrProvider.providerName)
        }
        builder.show()
    }

    private fun retrieveDeviceName(): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        return prefs.getString(activity.resources.getString(R.string.pref_bt_name), "")
    }

    private fun retrieveDeviceAddress(): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        return prefs.getString(activity.resources.getString(R.string.pref_bt_address), "")
    }

    private fun retrieveProviderName(): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        return prefs.getString(activity.resources.getString(R.string.pref_bt_provider), "")
    }

    /** END MAIN FRAGMENT IMPLEMENTATION **/

    /** PRESENTER IMPLEMENTATION **/

    private fun load() {
        if (btProviderName.isNotBlank()) {
            Log.d(TAG, "HRManager.get($btProviderName)")
            hrProvider = HRManager.getHRProvider(activity, btProviderName)
        }
    }

    private fun open() {
        if (hrProviderSelected == true && ::hrProvider.isInitialized && !hrProvider.isEnabled) {
            if (hrProvider.startEnableIntent(activity, 0)) {
                return
            }
            //hrProvider = null
            hrProviderSelected = false
        }
        if (hrProviderSelected == true && ::hrProvider.isInitialized) {
            Log.d(hrProvider.providerName, ".open(this)")
            hrProvider.open(handler, this)
        }
    }

    private fun scanConnect() {
        Log.d(TAG, hrProvider.providerName + ".stopScan()")
        hrProvider.stopScan()
        connect2()
    }

    private fun scanCancel() {
        Log.d(TAG, hrProvider.providerName + ".stopScan()")
        callback.scanCanceled()
        hrProvider.stopScan()
        load()
        open()
    }

    private fun connect2() {
        stopTimer()
        if (!::hrProvider.isInitialized || btName.isBlank() || btAddress.isBlank()) {
            return
        }
        if (hrProvider.isConnecting || hrProvider.isConnected) {
            disconnect()
        }

        callback.setHeartRateMonitorName(getName().toString())
        callback.setHeartRateValue(0)

        var name: String = btName
        if (btName.isBlank()) {
            name = btAddress
        }
        Log.d(TAG, hrProvider.providerName + ".connect(" + name + ")")

        hrProvider.connect(HRDeviceRef.create(hrProvider.providerName, btName, btAddress))
    }



    private fun getName(): CharSequence {
        return if (btName != null && btName!!.isNotEmpty()) btName!! else btAddress!!
    }

    private fun saveDevice(deviceName: String, deviceAddress: String, providerName: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val ed = prefs.edit()
        ed.putString(activity.resources.getString(R.string.pref_bt_name), deviceName)
        ed.putString(activity.resources.getString(R.string.pref_bt_address), deviceAddress)
        ed.putString(activity.resources.getString(R.string.pref_bt_provider), providerName)
        ed.apply()
    }

    private var hrReader: Timer? = null
    private fun startTimer() {
        hrReader = Timer()
        hrReader!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post(object : Runnable {
                    override fun run() {
                        readHR()
                    }
                })
            }
        }, 0, 500)
    }

    private fun stopTimer() {
        hrReader?.let {
            it.cancel()
            it.purge()
            hrReader = null
        }
    }

    private fun readHR() {
        if (hrProviderSelected == true && ::hrProvider.isInitialized) {
            val data = hrProvider.hrData
            if (data != null) {
                val age = data!!.timestamp
                var hrValue: Long = 0
                if (data!!.hasHeartRate)
                    hrValue = data!!.hrValue

                //getView()?.setHeartRateValue(hrValue.toInt())
                callback.setHeartRateValue(hrValue.toInt())
            }
        }
    }

    private fun scanAction() {
        clear()
        stopTimer()

        close()
        mIsScanning = true
        Log.d(TAG, "select HR-provider")
        selectProvider()
    }

    private fun clear() {
        btAddress = ""
        btName = ""
        btProviderName = ""
    }

    private fun close() {
        if(hrProviderSelected == true && ::hrProvider.isInitialized){
            Log.d(hrProvider.providerName, ".close()")
            hrProvider.close()
            //hrProvider = null
            hrProviderSelected = false
        }
    }

    /** END PRESENTER IMPLEMENTATION **/

    /** HR CLIENT IMPLEMENTATION **/

    private var mIsScanning = false

    override fun onOpenResult(ok: Boolean) {
        Log.d(TAG, hrProvider.providerName + "::onOpenResult(" + ok + ")")
        if (mIsScanning) {
            mIsScanning = false
            startScan(hrProvider, deviceAdapter)
            return
        }
    }

    override fun onScanResult(device: HRDeviceRef) {
        Log.d(TAG, (hrProvider.providerName + "::onScanResult(" + device.address + ", "
                + device.name + ")"))
        deviceAdapter.deviceList.add(device)
        deviceAdapter.notifyDataSetChanged()
    }

    override fun onConnectResult(connectOK: Boolean) {
        Log.d(TAG, hrProvider.providerName + "::onConnectResult(" + connectOK + ")")
        if (connectOK) {
            callback.onDeviceConnected()
            callback.setHeartRateMonitorName(hrProvider.name)
            callback.setHeartRateMonitorProviderName(hrProvider.providerName)
            callback.setHeartRateMonitorAddress(btAddress)
            saveDevice(hrProvider.name, btAddress, hrProvider.providerName)
            if (hrProvider.batteryLevel > 0) {
                val level = hrProvider.batteryLevel
                callback.setBatteryLevel(level)
            }
            startTimer()
        }
    }

    override fun onDisconnectResult(disconnectOK: Boolean) {
        if (disconnectOK){
            callback.onDeviceDisconnected()
        }
        Log.d(TAG, hrProvider.providerName + "::onDisconnectResult(" + disconnectOK + ")")
    }

    override fun onCloseResult(closeOK: Boolean) {
        Log.d(TAG, hrProvider.providerName + "::onCloseResult(" + closeOK + ")")
    }

    override fun log(src: HRProvider?, msg: String?) {
        Log.d(TAG, "$src?.name: $msg?")
    }

    /** END HR CLIENT IMPLEMENTATION **/


}