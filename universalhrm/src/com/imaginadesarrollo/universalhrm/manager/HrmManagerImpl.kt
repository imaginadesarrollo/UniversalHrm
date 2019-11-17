package com.imaginadesarrollo.universalhrm.manager

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.widget.ArrayAdapter
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.R
import com.imaginadesarrollo.universalhrm.manager.ant.AntConnectionImpl
import com.imaginadesarrollo.universalhrm.manager.bluetooth.BluetoothConnectionImpl
import com.imaginadesarrollo.universalhrm.manager.garmin.GarminConnectionImpl
import com.imaginadesarrollo.universalhrm.manager.mock.MockConnectionImpl


internal class HrmManagerImpl(private val activity: Activity, private val caller: HrmCallbackMethods? = null): HrmManager{

    companion object {
        const val TAG = "UniversalHrmImpl"
    }

    private val callback: HrmCallbackMethods by lazy {  caller ?: (activity as HrmCallbackMethods) }
    private var connection: HrmConnection? = null


    override fun scan() {
        connection?.disconnect()

        val connectionTypesList = listOf(
                activity.getText(R.string.hrm_bluetooth),
                activity.getText(R.string.hrm_ant),
                activity.getText(R.string.hrm_garmin)
        )
        val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_single_choice, connectionTypesList)
        val dialog = AlertDialog.Builder(activity).run {
            setAdapter(adapter) { dialog, which ->  when(which){
                0 -> showBluetoothSelector()
                1 -> showAntSelector()
                else -> connectWithGarmin()
            }
            }
            setNegativeButton(android.R.string.cancel, null)
            setTitle(R.string.hrm_select_type_of_connection)
            show()
        }

    }

    override fun disconnect() {
        connection?.disconnect()
        connection = null
    }

    private fun showAntSelector(){
        connection = AntConnectionImpl(activity, callback) as HrmConnection
        val adapter = connection?.getAdapter()
        val dialog = AlertDialog.Builder(activity).run {
            setAdapter(adapter) { _, _ -> }
            setNegativeButton(android.R.string.cancel, null)
            setTitle(R.string.hrm_select_device)
            show()
        }

        connection?.addAlertDialogCallback(object : HrmConnection.AlertDialogCallback{
            override fun close() {
                dialog.dismiss()
            }
        })
    }

    private fun showBluetoothSelector(){
        connection = BluetoothConnectionImpl(activity, callback) as HrmConnection
        val adapter = connection?.getAdapter()
        val dialog = AlertDialog.Builder(activity).run {
            setAdapter(adapter) { _, _ -> }
            setNegativeButton(android.R.string.cancel, null)
            setTitle(R.string.hrm_select_device)
            show()
        }

        connection?.addAlertDialogCallback(object : HrmConnection.AlertDialogCallback{
            override fun close() {
                dialog.dismiss()
            }
        })
    }

    private fun connectWithGarmin() {
        connection = GarminConnectionImpl(activity, callback) as HrmConnection
        val adapter = connection?.getAdapter()
        val dialog = AlertDialog.Builder(activity).run {
            setAdapter(adapter) { _, _ -> }
            setNegativeButton(android.R.string.cancel, null)
            setTitle(R.string.hrm_select_device)
            show()
        }

        connection?.addAlertDialogCallback(object : HrmConnection.AlertDialogCallback{
            override fun close() {
                dialog.dismiss()
            }
        })
    }

    private fun showMockSelector(){
        connection = MockConnectionImpl(activity, callback)
    }
}
