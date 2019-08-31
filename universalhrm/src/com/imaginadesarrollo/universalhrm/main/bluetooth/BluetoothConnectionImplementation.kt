package com.imaginadesarrollo.universalhrm.main.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.utils.Utils
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction


/**
 * Created by Kike Bodi (bodi.inf@gmail.com) on 2019-08-27.
 * Copyright by Imagina Desarrollo. All rights reserved.
 */
class BluetoothConnectionImplementation(private val context: Context,
                                        private val callback: HrmCallbackMethods)
  : HrmConnection {

  override fun addAlertDialogCallback(callback: HrmConnection.AlertDialogCallback) {
    dialogCallback = callback
  }

  private val compositeDisposable = CompositeDisposable()
  private val rxBleClient by lazy { RxBleClient.create(context) }
  private val listBluetoothDevices = mutableListOf<RxBleDevice>()
  private lateinit var adapter: BluetoothDeviceAdapter
  private var dialogCallback: HrmConnection.AlertDialogCallback? = null

  override fun getAdapter(): BluetoothDeviceAdapter{
    // This call means that we should start looking for data
    adapter = BluetoothDeviceAdapter(context)
    startBluetoothScan()
    return adapter
  }
  
  override fun disconnect() {
    dialogCallback?.close()
    compositeDisposable.clear()
  }

  private fun onDeviceSelected(device: RxBleDevice) {
    compositeDisposable.clear()

    callback.setHeartRateMonitorName(device.name ?: "")
    callback.setHeartRateMonitorAddress(device.macAddress)
    callback.setHeartRateMonitorProviderName("Bluetooth")

    val deviceConnectionDisposable = rxBleClient.getBleDevice(device.macAddress)
            .establishConnection(true)
            .publish { it ->
              // Battery
              val batteryObs = it.flatMapSingle { it.readCharacteristic(BATTERY_LEVEL_CHAR_UUID) }
                      .map { byte -> byte[0].toInt() }
              // Heart rate
              val heartRateObs = it.map { it.setupNotification(HEART_RATE_MEASUREMENT_CHAR_UUID) }
              Observable.zip(
                      batteryObs,
                      heartRateObs,
                      BiFunction<Int, Observable<Observable<ByteArray>>, Pair<Int, Observable<Observable<ByteArray>>>> { t1, t2 -> Pair(t1, t2) })
            }
            .doOnSubscribe {
              //customBuilder.dismiss()
              callback.onDeviceConnected()
            }
            .doOnComplete { callback.onDeviceDisconnected() }
            .onErrorReturn { Pair(-1, Observable.just(Observable.just(byteArrayOf()))) }
            .subscribe {
              (context as Activity).runOnUiThread {
                // Workaround to be able to post on UI.
                callback.setBatteryLevel(it.first)
              }

              it.second.subscribe { observableByteArray ->
                observableByteArray.map { byteArray -> Utils.decode(byteArray) }
                        .onErrorReturn { 0 }
                        .subscribe { hrValue ->
                          context.runOnUiThread {
                            // Workaround to be able to post on UI.
                            callback.setHeartRateValue(hrValue)
                          }
                        }
              }
            }

    compositeDisposable.add(deviceConnectionDisposable)
  }
  
  @SuppressLint("CheckResult")
  private fun startBluetoothScan(){
    val scanSettings = ScanSettings.Builder()
      .build()
  
    val scanFilter = ScanFilter.Builder()
      .setServiceUuid(ParcelUuid(UNIVERSAL_HRP_SERVICE))
      .build()
  
    val scanDisposable = rxBleClient.scanBleDevices(scanSettings, scanFilter)
      .doOnError { Log.e(TAG, it.message) }
      .subscribe{ scanResult ->
        if(!listBluetoothDevices.contains(scanResult.bleDevice)) {
          listBluetoothDevices.add(scanResult.bleDevice)
          adapter.notifyDataSetChanged()
        }
      }
    
    compositeDisposable.add(scanDisposable)
  }
  
  
  
  
  inner class BluetoothDeviceAdapter(private val mContext: Context) : ArrayAdapter<RxBleDevice>(mContext, 0, listBluetoothDevices) {
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
      
      val listItem = convertView ?: LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_single_choice, parent, false)
      val deviceName = listBluetoothDevices[position].name ?: listBluetoothDevices[position].macAddress
      val address = listBluetoothDevices[position].macAddress

      listItem.findViewById<CheckedTextView>(android.R.id.text1).text = deviceName
      
      listItem.setOnClickListener {
        onDeviceSelected(listBluetoothDevices[position])
        dialogCallback?.close()

        (context as Activity).runOnUiThread {
          callback.setHeartRateMonitorName(deviceName)
          callback.setHeartRateMonitorProviderName("Bluetooth")
          callback.setHeartRateMonitorAddress(address)
        }
      }
      
      return listItem
    }
  }
  
  companion object{
    val UNIVERSAL_HRP_SERVICE = Utils.convertFromInteger(0x180D)
    val HEART_RATE_MEASUREMENT_CHAR_UUID = Utils.convertFromInteger(0x2A37)
    val BATTERY_LEVEL_SERVICE = Utils.convertFromInteger(0x180F)
    val BATTERY_LEVEL_CHAR_UUID = Utils.convertFromInteger(0x2A19)
    
    const val TAG = "BluetoothConnImpl"
  }
}