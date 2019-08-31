package com.imaginadesarrollo.universalhrm.main

import android.support.v7.app.AlertDialog
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.R
import com.imaginadesarrollo.universalhrm.main.ant.AntImplementation
import com.imaginadesarrollo.universalhrm.main.bluetooth.BluetoothConnectionImplementation
import com.imaginadesarrollo.universalhrm.main.bluetooth.HrmConnection


internal class UniversalHrmImplementation(private val activity: android.support.v7.app.AppCompatActivity, private val caller: HrmCallbackMethods? = null): HrmImplementation{

    companion object {
        const val TAG = "UniversalHrmImpl"
    }

    private val callback: HrmCallbackMethods by lazy {  caller ?: (activity as HrmCallbackMethods) }


    override fun scan() {
        // TODO:  Create an AlertDialog to choose between Bluetooth and ANT+ types

        //showBluetoothSelector()

        showAntSelector()
    }

    private fun showAntSelector(){
        val antConnection = AntImplementation(activity, callback) as HrmConnection
        val adapter = antConnection.getAdapter()
        val dialog = AlertDialog.Builder(activity).run {
            setAdapter(adapter) { _, _ -> }
            setNegativeButton(android.R.string.cancel, null)
            setTitle(R.string.select_type_of_Bluetooth_device)
            show()
        }

        antConnection.addAlertDialogCallback(object : HrmConnection.AlertDialogCallback{
            override fun close() {
                dialog.dismiss()
            }
        })
    }

    private fun showBluetoothSelector(){
        val bluettoothConnection = BluetoothConnectionImplementation(activity, callback) as HrmConnection
        val adapter = bluettoothConnection.getAdapter()
        val dialog = AlertDialog.Builder(activity).run {
            setAdapter(adapter) { _, _ -> }
            setNegativeButton(android.R.string.cancel, null)
            setTitle(R.string.select_type_of_Bluetooth_device)
            show()
        }

        bluettoothConnection.addAlertDialogCallback(object : HrmConnection.AlertDialogCallback{
            override fun close() {
                dialog.dismiss()
            }
        })
    }
}
