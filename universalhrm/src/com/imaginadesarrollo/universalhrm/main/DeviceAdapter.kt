package com.imaginadesarrollo.universalhrm.main

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import com.imaginadesarrollo.universalhrm.manager.HRDeviceRef

/**
 * Created by kike on 19/08/2018.
 */
class DeviceAdapter(val callback: OnDeviceSelected): RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val deviceList = mutableListOf<HRDeviceRef>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder =
        DeviceViewHolder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_single_choice, parent, false))

    override fun getItemCount(): Int = deviceList.size

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(deviceList[position])
    }

    fun addDevice(device: HRDeviceRef) {
        deviceList.add(device)
        notifyDataSetChanged()
    }

    fun clear() {
        deviceList.clear()
    }

    inner class DeviceViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bind(item: HRDeviceRef) = with(itemView){
            (itemView as? CheckedTextView)?.apply {
                tag = item
                text = item.name

                setOnClickListener {
                    isChecked = true
                    callback.onDeviceSelected(item)
                }
            }
        }
    }

    interface OnDeviceSelected{
        fun onDeviceSelected(device: HRDeviceRef)
    }
}