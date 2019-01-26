package com.imaginadesarrollo.universalhrm.main

import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.R
import com.imaginadesarrollo.universalhrm.manager.HRDeviceRef
import com.imaginadesarrollo.universalhrm.manager.HRManager
import com.imaginadesarrollo.universalhrm.manager.HRProvider
import com.imaginadesarrollo.universalhrm.ui.custom.DeviceDialogFragment
import java.util.*

internal class UniversalHrmImplementation(private val activity: android.support.v7.app.AppCompatActivity, private val caller: HrmCallbackMethods? = null): HrmImplementation, HRProvider.HRClient {

    companion object {
        const val TAG = "UniversalHrmImpl"
    }

    private val callback: HrmCallbackMethods by lazy {  caller ?: (activity as HrmCallbackMethods) }
    private val deviceAdapter: DeviceAdapter by lazy { DeviceAdapter(activity) }
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(activity) }
    private val handler: Handler by lazy { Handler() }

    private var btName: String = ""
    private var btAddress: String = ""
    private var btProviderName: String = ""
    private var hrProviderSelected: Boolean = false

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
        if(::hrProvider.isInitialized){
            Log.d(TAG, hrProvider.providerName + ".disconnect()")
            hrProvider.disconnect()
            callback.onDeviceDisconnected()
        }
    }

    override fun isConnected(): Boolean {
        return (::hrProvider.isInitialized && (hrProvider.isConnecting || hrProvider.isConnected))
    }

    override fun isThereSavedDevice(): Boolean { return btAddress.isNotBlank()}

    /** MAIN FRAGMENT IMPLEMENTATION **/

    private fun startScan(hrProvider: HRProvider, deviceAdapter: DeviceAdapter) {
        Log.d(hrProvider.providerName, ".startScan()")
        deviceAdapter.clear()
        hrProvider.startScan()

        val customBuilder = DeviceDialogFragment(deviceAdapter)
        customBuilder.show(activity.supportFragmentManager, "dialog")
    }

    private fun selectProvider() {
        val items = arrayOfNulls<CharSequence>(providers.size)
        val itemNames = arrayOfNulls<CharSequence>(providers.size)
        for (i in items.indices) {
            items[i] = providers.get(i).getProviderName()
            itemNames[i] = providers.get(i).getName()
        }
        val builder = android.support.v7.app.AlertDialog.Builder(activity)
        builder.setTitle(activity.getString(R.string.select_type_of_Bluetooth_device))
        builder.setPositiveButton(activity.getString(R.string.OK)
        ) { dialog, which -> open() }
        builder.setNegativeButton(activity.getString(R.string.cancel)
        ) { dialog, which ->
            mIsScanning = false
            load()
            open()
            dialog.dismiss()
        }
        builder.setSingleChoiceItems(itemNames, -1
        ) { arg0, arg1 ->
            hrProvider = HRManager.getHRProvider(activity,
                    items[arg1].toString())
            hrProviderSelected = true
            Log.d(TAG, "hrProvider = " + hrProvider.providerName)
        }
        builder.show()
    }

    private fun retrieveDeviceName(): String =
        prefs.getString(activity.getString(R.string.pref_bt_name), "") ?: ""

    private fun retrieveDeviceAddress(): String =
            prefs.getString(activity.getString(R.string.pref_bt_address), "") ?: ""

    private fun retrieveProviderName(): String =
            prefs.getString(activity.getString(R.string.pref_bt_provider), "") ?: ""

    private fun saveDevice(deviceName: String, deviceAddress: String, providerName: String) {
        prefs.edit().apply {
            putString(activity.resources.getString(R.string.pref_bt_name), deviceName)
            putString(activity.resources.getString(R.string.pref_bt_address), deviceAddress)
            putString(activity.resources.getString(R.string.pref_bt_provider), providerName)
        }.apply()
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

    private var hrReader: Timer? = null
    private fun startTimer() {
        hrReader = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    handler.post(object : Runnable {
                        override fun run() {
                            readHR()
                        }
                    })
                }
            }, 0, 500)
        }
    }

    private fun stopTimer() {
        hrReader?.apply {
            cancel()
            purge()
        }
        hrReader = null
    }

    private fun readHR() {
        if (hrProviderSelected == true && ::hrProvider.isInitialized) {
            hrProvider.hrData.let {
                var hrValue: Long = 0
                if (it.hasHeartRate)
                    hrValue = it.hrValue

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
        deviceAdapter.addDevice(device)
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