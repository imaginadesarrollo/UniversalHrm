package com.imaginadesarrollo.universalhrm.main.bluetooth

import android.annotation.SuppressLint
import android.annotation.TargetApi
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
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers


/**
 * Created by Kike Bodi (bodi.inf@gmail.com) on 2019-08-27.
 * Copyright by Imagina Desarrollo. All rights reserved.
 */
class BluetoothConnectionImplementation(private val context: Context,
                                        private val callback: HrmCallbackMethods)
  : BluetoothConnection {
  private val compositeDisposable = CompositeDisposable()
  private val rxBleClient by lazy { RxBleClient.create(context) }
  private val listBluetoothDevices = mutableListOf<RxBleDevice>()
  private lateinit var adapter: BluetoothDeviceAdapter
  
  private var scanObservable: Observable<ScanResult>? = null
  
  override fun getAdapter(): BluetoothDeviceAdapter{
    // This call means that we should start looking for data
    adapter = BluetoothDeviceAdapter(context)
    startBluetoothScan()
    return adapter
  }
  
  override fun disconnect() {
    compositeDisposable.clear()
  }
  
  private fun onDeviceSelected(device: RxBleDevice) {
    compositeDisposable.clear()
    
    callback.setHeartRateMonitorName(device.name ?: "")
    callback.setHeartRateMonitorAddress(device.macAddress)
    callback.setHeartRateMonitorProviderName("Bluetooth")
    
    
    /*client.flatMapSingle { it.readCharacteristic(BATTERY_LEVEL_CHAR_UUID) }
            .subscribe {
                val batteryLevel = it[0].toInt()
                activity.runOnUiThread { callback.setBatteryLevel(batteryLevel) }
            }*/
    
    
    
    val hrDisposable = rxBleClient.getBleDevice(device.macAddress)
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
          .map { Utils.decode(it) }
          .observeOn(Schedulers.io())
          .onErrorReturn { 0 }
          .subscribe { hrValue ->
            (context as Activity).runOnUiThread { // Workaround to be able to post on UI.
              callback.setHeartRateValue(hrValue)
            }
          }
      }
    
    compositeDisposable.add(hrDisposable)
  }
  
  @TargetApi(23)
  @SuppressLint("CheckResult")
  private fun startBluetoothScan(){
    val scanSettings = ScanSettings.Builder()
      /* .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
       .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)*/
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
      
      listItem.findViewById<CheckedTextView>(android.R.id.text1).apply {
        text = listBluetoothDevices[position].name ?: listBluetoothDevices[position].macAddress
      }
      
      listItem.setOnClickListener {
        onDeviceSelected(listBluetoothDevices[position])
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