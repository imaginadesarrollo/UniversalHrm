package com.imaginadesarrollo.universalhrm.main

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle


/**
 * Created by Kike Bodi (bodi.inf@gamil.com) on 2019-08-25.
 * Copyright by Imagina desarrollo. All rights reserved.
 */
class AntImplementation(val activity: Activity) {
  
  protected var releaseHandle: PccReleaseHandle<AntPlusHeartRatePcc>? = null
  internal var hrScanCtrl: AsyncScanController<AntPlusHeartRatePcc>? = null
  
  init {
    handleReset()
  }
  
  /**
   * Resets the PCC connection to request access again and clears any existing display data.
   */
  protected fun handleReset() {
    //Release the old access if it exists
    releaseHandle?.close()
    requestAccessToPcc()
  }
  
  // Variables for this method:
  val scannedDevices = mutableListOf<AsyncScanController.AsyncScanResultDeviceInfo>()
  
  fun addDevice(device: AsyncScanController.AsyncScanResultDeviceInfo){
    scannedDevices.forEach {
      if(it.antDeviceNumber == device.antDeviceNumber) return
    }
    scannedDevices.add(device)
    
    //TODO remove this. Only for test.
    requestConnectToResult(device)
  }
  
  /**
   * Requests the asynchronous scan controller
   */
  protected fun requestAccessToPcc() {
    //initScanDisplay()
    hrScanCtrl = AntPlusHeartRatePcc.requestAsyncScanController(
      activity,
      0,
      object : AsyncScanController.IAsyncScanResultReceiver {
        
        override fun onSearchStopped(reasonStopped: RequestAccessResult) {
          //The triggers calling this function use the same codes and require the same actions as those received by the standard access result receiver
          base_IPluginAccessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD)
        }
        
        override fun onSearchResult(deviceFound: AsyncScanController.AsyncScanResultDeviceInfo) {
          
          addDevice(deviceFound)
          // Manage if the device was already connected.
          
        }
        
      })
  }
  
  /**
   * Requests access to the given search result.
   * @param asyncScanResultDeviceInfo The search result to attempt to connect to.
   */
  protected fun requestConnectToResult(asyncScanResultDeviceInfo: AsyncScanController.AsyncScanResultDeviceInfo) {
    //Inform the user we are connecting
    activity.runOnUiThread {
      //mTextView_Status.setText("Connecting to " + asyncScanResultDeviceInfo.deviceDisplayName)
      releaseHandle = hrScanCtrl?.requestDeviceAccess(
        asyncScanResultDeviceInfo,
        AntPluginPcc.IPluginAccessResultReceiver { result, resultCode, initialDeviceState ->
          if (resultCode == RequestAccessResult.SEARCH_TIMEOUT) {
            //On a connection timeout the scan automatically resumes, so we inform the user, and go back to scanning
            activity.runOnUiThread {
              Toast.makeText(
                activity,
                "Timed out attempting to connect, try again",
                Toast.LENGTH_LONG
              ).show()
              //mTextView_Status.setText("Scanning for heart rate devices asynchronously...")
            }
          } else {
            //Otherwise the results, including SUCCESS, behave the same as
            base_IPluginAccessResultReceiver.onResultReceived(
              result,
              resultCode,
              initialDeviceState
            )
            hrScanCtrl = null
          }
        },
        base_IDeviceStateChangeReceiver
      )
    }
  }
  
  //Receives state changes and shows it on the status display line
  protected var base_IDeviceStateChangeReceiver =
    AntPluginPcc.IDeviceStateChangeReceiver { newDeviceState ->
    
    }
  
  protected var base_IPluginAccessResultReceiver: AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc> =
    AntPluginPcc.IPluginAccessResultReceiver { result, resultCode, initialDeviceState ->
      //Handle the result, connecting to events on success or reporting failure to user.
      when (resultCode) {
        RequestAccessResult.SUCCESS -> {
          subscribeToHrEvents(result)
        }
        RequestAccessResult.CHANNEL_NOT_AVAILABLE -> {
          Toast.makeText(
            activity,
            "Channel Not Available",
            Toast.LENGTH_SHORT
          ).show()
          //tv_status.setText("Error. Do Menu->Reset.")
        }
        RequestAccessResult.ADAPTER_NOT_DETECTED -> {
          Toast.makeText(
            activity,
            "ANT Adapter Not Available. Built-in ANT hardware or external adapter required.",
            Toast.LENGTH_SHORT
          ).show()
          //tv_status.setText("Error. Do Menu->Reset.")
        }
        RequestAccessResult.BAD_PARAMS -> {}
        RequestAccessResult.OTHER_FAILURE -> {}
        RequestAccessResult.DEPENDENCY_NOT_INSTALLED -> {
          //tv_status.setText("Error. Do Menu->Reset.")
          val adlgBldr = AlertDialog.Builder(activity)
          adlgBldr.setTitle("Missing Dependency")
          adlgBldr.setMessage("The required service\n\"" + AntPlusHeartRatePcc.getMissingDependencyName() + "\"\n was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?")
          adlgBldr.setCancelable(true)
          adlgBldr.setPositiveButton(
            "Go to Store"
          ) { dialog, which ->
            var startStore: Intent? = null
            startStore = Intent(
              Intent.ACTION_VIEW,
              Uri.parse("market://details?id=" + AntPlusHeartRatePcc.getMissingDependencyPackageName())
            )
            startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            activity.startActivity(startStore)
          }
          adlgBldr.setNegativeButton(
            "Cancel"
          ) { dialog, which -> dialog.dismiss() }
          
          val waitDialog = adlgBldr.create()
          waitDialog.show()
        }
        RequestAccessResult.UNRECOGNIZED -> {}
        else ->{}
      }
    }
  
  /**
   * Switches the active view to the data display and subscribes to some of the data events
   */
  fun subscribeToHrEvents(hrPcc: AntPlusHeartRatePcc) {
    hrPcc.subscribeHeartRateDataEvent(AntPlusHeartRatePcc.IHeartRateDataReceiver { estTimestamp, eventFlags, computedHeartRate, heartBeatCount, heartBeatEventTime, dataState ->
      // Mark heart rate with asterisk if zero detected
      val textHeartRate =
        computedHeartRate.toString() + if (AntPlusHeartRatePcc.DataState.ZERO_DETECTED == dataState) "*" else ""
      
      // Mark heart beat count and heart beat event time with asterisk if initial value
      val textHeartBeatCount =
        heartBeatCount.toString() + if (AntPlusHeartRatePcc.DataState.INITIAL_VALUE == dataState) "*" else ""
      val textHeartBeatEventTime =
        heartBeatEventTime.toString() + if (AntPlusHeartRatePcc.DataState.INITIAL_VALUE == dataState) "*" else ""
      
      
    })
    
    hrPcc.subscribeManufacturerAndSerialEvent { estTimestamp, eventFlags, manufacturerID, serialNumber ->
    
    }
    
    hrPcc.subscribeVersionAndModelEvent { estTimestamp, eventFlags, hardwareVersion, softwareVersion, modelNumber ->
    
    }
  }
  
}
