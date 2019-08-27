package com.imaginadesarrollo.universalhrm.main

import android.support.v7.app.AlertDialog
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.R
import com.imaginadesarrollo.universalhrm.main.bluetooth.BluetoothConnection
import com.imaginadesarrollo.universalhrm.main.bluetooth.BluetoothConnectionImplementation


internal class UniversalHrmImplementation(private val activity: android.support.v7.app.AppCompatActivity, private val caller: HrmCallbackMethods? = null): HrmImplementation{

    companion object {
        const val TAG = "UniversalHrmImpl"
    }

    private val callback: HrmCallbackMethods by lazy {  caller ?: (activity as HrmCallbackMethods) }


    override fun scan() {
        // TODO:  Create an AlertDialog to choose between Bluetooth and ANT+ types

        showBluetoothSelector()
        
      //AntImplementation(activity)
    }

    

    private val bluettoothConnection = BluetoothConnectionImplementation(activity, callback) as BluetoothConnection
    private val adapter = bluettoothConnection.getAdapter()
    private fun showBluetoothSelector(){

        AlertDialog.Builder(activity).apply {
            setAdapter(adapter, { dialog, which ->
                // try to dismiss the dialog.
            })
            setNegativeButton(android.R.string.cancel, null)
            setTitle(R.string.select_type_of_Bluetooth_device)
            show()
        }
    }
}
