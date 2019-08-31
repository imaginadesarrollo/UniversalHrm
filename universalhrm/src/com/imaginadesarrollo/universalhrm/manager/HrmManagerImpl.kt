package com.imaginadesarrollo.universalhrm.manager

import android.support.v7.app.AlertDialog
import android.widget.ArrayAdapter
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.R
import com.imaginadesarrollo.universalhrm.manager.ant.AntConnectionImpl
import com.imaginadesarrollo.universalhrm.manager.bluetooth.BluetoothConnectionImpl


internal class HrmManagerImpl(private val activity: android.support.v7.app.AppCompatActivity, private val caller: HrmCallbackMethods? = null): HrmManager{

    companion object {
        const val TAG = "UniversalHrmImpl"
    }

    private val callback: HrmCallbackMethods by lazy {  caller ?: (activity as HrmCallbackMethods) }
    private var connection: HrmConnection? = null


    override fun scan() {

        val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_single_choice, listOf("Ant", "Bluettoth"))
        val dialog = AlertDialog.Builder(activity).run {
            setAdapter(adapter) { dialog, which ->  when(which){
                0 -> showAntSelector()
                else -> showBluetoothSelector()
            }
            }
            setNegativeButton(android.R.string.cancel, null)
            setTitle(R.string.select_type_of_Bluetooth_device)
            show()
        }

    }

    override fun disconnect() {

    }

    private fun showAntSelector(){
        connection = AntConnectionImpl(activity, callback) as HrmConnection
        val adapter = connection?.getAdapter()
        val dialog = AlertDialog.Builder(activity).run {
            setAdapter(adapter) { _, _ -> }
            setNegativeButton(android.R.string.cancel, null)
            setTitle(R.string.select_type_of_Bluetooth_device)
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
            setTitle(R.string.select_type_of_Bluetooth_device)
            show()
        }

        connection?.addAlertDialogCallback(object : HrmConnection.AlertDialogCallback{
            override fun close() {
                dialog.dismiss()
            }
        })
    }
}
