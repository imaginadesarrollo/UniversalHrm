package com.imaginadesarrollo.universalhrmapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.UniversalHrm
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : android.support.v7.app.AppCompatActivity(), HrmCallbackMethods {

    private val universalHrm: UniversalHrm by lazy { UniversalHrm(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanButton.setOnClickListener {
            checkPermissionsAndScan()
        }

        connectButton.setOnClickListener {
            universalHrm.disconnect()
        }
    }

    private fun checkPermissionsAndScan(){
        Dexter.withActivity(this)
                .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        // TODO remember to enable GPS.
                        universalHrm.scan()
                        connectButton.isEnabled = true
                    }
                    override fun onPermissionDenied(response: PermissionDeniedResponse) {}
                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {}
                }).check()
    }

    override fun setBatteryLevel(level: Int) {
        batteryLevel.text = getString(R.string.battery_level_value, level)
    }

    override fun setHeartRateValue(value: Int) {
        hrValue.text = getString(R.string.bpms, value)
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

    override fun deviceNotSupported() =
        Toast.makeText(this, R.string.heart_rate_monitor_is_not_supported_for_your_device, Toast.LENGTH_LONG).show()


    override fun onDeviceConnected() {
        connectButton.visibility = View.VISIBLE
    }

    override fun onDeviceDisconnected() {
        connectButton.visibility = View.GONE
        resetFields()
    }

    private fun resetFields(){
        deviceName.text = getString(R.string.device_name_empty)
        deviceAddress.text = getString(R.string.device_address_empty)
        providerName.text = getString(R.string.device_provider_empty)
        batteryLevel.text = getString(R.string.battery_level_value, 0)//"Battery level: 0"
        hrValue.text = getString(R.string.bpms, 0)//"0 bpm"
    }
}
