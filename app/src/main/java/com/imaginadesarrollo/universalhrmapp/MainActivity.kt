package com.imaginadesarrollo.universalhrmapp

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.UniversalHrm
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity(), HrmCallbackMethods {

    private val universalHrm: UniversalHrm by lazy { UniversalHrm(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanButton.setOnClickListener {
            checkPermissionsAndScan()
        }
    }

    private fun checkPermissionsAndScan(){
        Dexter.withActivity(this)
                .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        universalHrm.scan()
                        connectButton.isEnabled = true
                    }
                    override fun onPermissionDenied(response: PermissionDeniedResponse) {}
                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {}
                }).check()
    }



    override fun setBatteryLevel(level: Int) {
        batteryLevel.text = "Battery level: $level"
    }

    override fun setHeartRateValue(value: Int) {
        hrValue.text = "$value bpm"
    }

    override fun setHeartRateMonitorName(name: String) {
        if(name.isNotBlank())
            deviceName.text = name
    }

    override fun setHeartRateMonitorAddress(address: String) {
        if(address.isNotBlank())
            deviceAddress.text = address
    }

    override fun setHeartRateMonitorProviderName(pName: String) {
        if(pName.isNotBlank())
            providerName.text = pName
    }

    override fun deviceNotSupported() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.Heart_rate_monitor_is_not_supported_for_your_device))
        val listener = DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() }
        builder.setNegativeButton(getString(R.string.ok_rats), listener)
        builder.show()
    }

    override fun scanCanceled() {
        // FYI
    }

    override fun onDeviceConnected() {
        connectButton.visibility = View.VISIBLE
        connectButton.text = "Disconnect"
        connectButton.setOnClickListener {
            universalHrm.disconnect()
        }
    }

    override fun onDeviceDisconnected() {
        connectButton.visibility = View.VISIBLE
        connectButton.text = "Connect"
        connectButton.setOnClickListener {
            universalHrm.connect()
        }
        resetFields()
    }

    private fun resetFields(){
        deviceName.text = "Device name (not connected)"
        deviceAddress.text = "Device address (not connected)"
        providerName.text = "Device provider (not connected)"
        batteryLevel.text = "Battery level: 0"
        hrValue.text = "0 bpm"
    }
}
