package com.imaginadesarrollo.universalhrm.ui.custom

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.imaginadesarrollo.universalhrm.R
import com.imaginadesarrollo.universalhrm.main.DeviceAdapter
import kotlinx.android.synthetic.main.custom_fragment_dialog.*

/**
 * Created by kike on 19/08/2018.
 */
@SuppressLint("ValidFragment")
class DeviceDialogFragment constructor(private val adapter: DeviceAdapter): DialogFragment() {

    // onCreate --> (onCreateDialog) --> onCreateView --> onActivityCreated
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.custom_fragment_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deviceRecyclerView.layoutManager = LinearLayoutManager(context)
        deviceRecyclerView.adapter = adapter

        buttonPos.setOnClickListener {
            // Positive answer
            dismiss()
        }

        buttonNeg.setOnClickListener {
            // Negative answwer
            // If shown as dialog, cancel the dialog (cancel --> dismiss)
            if (showsDialog)
                dialog.cancel()
            else
                dismiss()// If shown as Fragment, dismiss the DialogFragment (remove it from view)
        }
    }
}