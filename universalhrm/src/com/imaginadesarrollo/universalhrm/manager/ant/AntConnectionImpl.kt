package com.imaginadesarrollo.universalhrm.manager.ant

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.R
import com.imaginadesarrollo.universalhrm.manager.HrmConnection


/**
 * Created by Kike Bodi (bodi.inf@gamil.com) on 2019-08-25.
 * Copyright by Imagina desarrollo. All rights reserved.
 */
class AntConnectionImpl(private val context: Context,
                        private val callback: HrmCallbackMethods): HrmConnection {

  protected var releaseHandle: PccReleaseHandle<AntPlusHeartRatePcc>? = null
  internal var hrScanCtrl: AsyncScanController<AntPlusHeartRatePcc>? = null

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
    adapter.notifyDataSetChanged()
  }
  
  /**
   * Requests the asynchronous scan controller
   */
  private fun requestAccessToPcc() {
    //initScanDisplay()
    hrScanCtrl = AntPlusHeartRatePcc.requestAsyncScanController(
      context,
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
    (context as Activity).runOnUiThread {
      //mTextView_Status.setText("Connecting to " + asyncScanResultDeviceInfo.deviceDisplayName)
      releaseHandle = hrScanCtrl?.requestDeviceAccess(
        asyncScanResultDeviceInfo,
        AntPluginPcc.IPluginAccessResultReceiver { result, resultCode, initialDeviceState ->
          if (resultCode == RequestAccessResult.SEARCH_TIMEOUT) {
            //On a connection timeout the scan automatically resumes, so we inform the user, and go back to scanning
            callback.onDeviceDisconnected()
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
          callback.onDeviceConnected()
          subscribeToHrEvents(result)
        }
        RequestAccessResult.DEPENDENCY_NOT_INSTALLED -> {
          //tv_status.setText("Error. Do Menu->Reset.")
          val adlgBldr = AlertDialog.Builder(context)
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

            context.startActivity(startStore)
          }
          adlgBldr.setNegativeButton(
            "Cancel"
          ) { dialog, which -> dialog.dismiss() }
          
          val waitDialog = adlgBldr.create()
          waitDialog.show()
        }
        else ->{
          callback.deviceNotSupported()
        }
      }
    }
  
  /**
   * Switches the active view to the data display and subscribes to some of the data events
   */
  fun subscribeToHrEvents(hrPcc: AntPlusHeartRatePcc) {
    hrPcc.subscribeHeartRateDataEvent(AntPlusHeartRatePcc.IHeartRateDataReceiver { estTimestamp, eventFlags, computedHeartRate, heartBeatCount, heartBeatEventTime, dataState ->
      (context as Activity).runOnUiThread {
        callback.setHeartRateValue(computedHeartRate)
      }
    })
    
    hrPcc.subscribeManufacturerAndSerialEvent { estTimestamp, eventFlags, manufacturerID, serialNumber -> }
    
    hrPcc.subscribeVersionAndModelEvent { estTimestamp, eventFlags, hardwareVersion, softwareVersion, modelNumber -> }
  }


  /** New implementation **/

  private lateinit var adapter: AntDeviceAdapter
  private var dialogCallback: HrmConnection.AlertDialogCallback? = null


  override fun getAdapter(): AntDeviceAdapter {
    // this call means that we should start looking for data
    adapter = AntDeviceAdapter(context)
    handleReset()
    return adapter
  }

  override fun disconnect() {
    releaseHandle?.close()
    hrScanCtrl?.closeScanController()
    dialogCallback?.close()
    callback.onDeviceDisconnected()
  }

  override fun addAlertDialogCallback(callback: HrmConnection.AlertDialogCallback) {
    dialogCallback = callback
  }

  inner class AntDeviceAdapter(private val mContext: Context) : ArrayAdapter<AsyncScanController.AsyncScanResultDeviceInfo>(mContext, 0, scannedDevices) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

      val listItem = convertView ?: LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_single_choice, parent, false)
      val deviceName = scannedDevices[position].deviceDisplayName ?: scannedDevices[position].antDeviceNumber.toString()
      val address = scannedDevices[position].scanResultInternalIdentifier?.toString() ?: ""


      listItem.findViewById<CheckedTextView>(android.R.id.text1).text = deviceName

      listItem.setOnClickListener {
        requestConnectToResult(scannedDevices[position])
        dialogCallback?.close()

        (context as Activity).runOnUiThread{
          callback.setHeartRateMonitorName(deviceName)
          callback.setHeartRateMonitorProviderName(context.getString(R.string.hrm_ant))
          callback.setHeartRateMonitorAddress(address)
        }
      }

      return listItem
    }
  }

}