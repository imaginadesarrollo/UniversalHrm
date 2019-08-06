package com.imaginadesarrollo.universalhrm.main

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.os.ParcelUuid
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.R
import com.imaginadesarrollo.universalhrm.utils.Utils.convertFromInteger
import com.imaginadesarrollo.universalhrm.utils.Utils.decode
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers


internal class UniversalHrmImplementation(private val activity: android.support.v7.app.AppCompatActivity, private val caller: HrmCallbackMethods? = null): HrmImplementation{

    companion object {
        const val TAG = "UniversalHrmImpl"
    }

    private val callback: HrmCallbackMethods by lazy {  caller ?: (activity as HrmCallbackMethods) }


    override fun scan() {
        // TODO:  Create an AlertDialog to choose between Bluetooth and ANT+ types

        showBluetoothSelector()
    }

    /**
     *
     *  New code with Rx for Bluetooth
     *
     */
    val rxBleClient by lazy { RxBleClient.create(activity) }


    val UNIVERSAL_HRP_SERVICE = convertFromInteger(0x180D)
    val HEART_RATE_MEASUREMENT_CHAR_UUID = convertFromInteger(0x2A37)
    val BATTERY_LEVEL_SERVICE = convertFromInteger(0x180F)
    val BATTERY_LEVEL_CHAR_UUID = convertFromInteger(0x2A19)

    @SuppressLint("CheckResult")
    @TargetApi(23)
    private fun startRxScan() {
        val scanSettings = ScanSettings.Builder()
                /* .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                 .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)*/
                .build()

        val scanFilter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UNIVERSAL_HRP_SERVICE))
                .build()

        rxBleClient.scanBleDevices(scanSettings, scanFilter)
                //.doOnSubscribe { customBuilder.show(activity.supportFragmentManager, "dialog") }
                .doOnError { Log.e(TAG, it.message) }
                .subscribe{ scanResult ->
                    if(!listBluetoothDevices.contains(scanResult.bleDevice)) {
                        listBluetoothDevices.add(scanResult.bleDevice)
                        adapter.notifyDataSetChanged()
                    }
                }
    }

    private fun onBluetoothDeviceSelected(device: RxBleDevice) {
        callback.setHeartRateMonitorName(device.name ?: "")
        callback.setHeartRateMonitorAddress(device.macAddress)
        callback.setHeartRateMonitorProviderName("Bluetooth")


        /*client.flatMapSingle { it.readCharacteristic(BATTERY_LEVEL_CHAR_UUID) }
                .subscribe {
                    val batteryLevel = it[0].toInt()
                    activity.runOnUiThread { callback.setBatteryLevel(batteryLevel) }
                }*/



        rxBleClient.getBleDevice(device.macAddress)
                .establishConnection(true)
                .flatMap { it.setupNotification(HEART_RATE_MEASUREMENT_CHAR_UUID) }
                .doOnSubscribe {
                    //customBuilder.dismiss()
                    callback.onDeviceConnected()
                }
                .doOnComplete { callback.onDeviceDisconnected() }
                .onErrorReturn { Observable.empty() } // handle error
                .subscribe { emitter ->
                    emitter.subscribeOn(Schedulers.io())
                            .map { decode(it) }
                            .observeOn(Schedulers.io())
                            .onErrorReturn { 0 }
                            .subscribe { hrValue ->
                                activity.runOnUiThread { // Workaround to be able to post on UI.
                                    callback.setHeartRateValue(hrValue)
                                }
                            }
                }
    }

    private val listBluetoothDevices = mutableListOf<RxBleDevice>()
    private val adapter = RxBleDeviceAdapter(activity)
    private fun showBluetoothSelector(){

        AlertDialog.Builder(activity).apply {
            setSingleChoiceItems(adapter, 0) { dialog, which ->
                onBluetoothDeviceSelected(listBluetoothDevices[which])
            }
            setNegativeButton(android.R.string.cancel, null)
            setTitle(R.string.select_type_of_Bluetooth_device)
            show()
        }

        startRxScan()
    }

    inner class RxBleDeviceAdapter(private val mContext: Context) : ArrayAdapter<RxBleDevice>(mContext, 0, listBluetoothDevices) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val listItem = convertView ?: LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_single_choice, parent, false)

            listItem.findViewById<CheckedTextView>(android.R.id.text1).apply {
                text = listBluetoothDevices[position].name ?: listBluetoothDevices[position].macAddress
            }

            return listItem
        }
    }
}
