package com.imaginadesarrollo.universalhrm.main

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.imaginadesarrollo.universalhrm.manager.HRDeviceRef
import java.util.*

/**
 * Created by kike on 19/08/2018.
 */
@SuppressLint("InflateParams")
internal class DeviceAdapter
// --Commented out by Inspection (2017-08-11 13:06):Resources resources = null;

(ctx: Context) : BaseAdapter() {

    val deviceList = ArrayList<HRDeviceRef>()
    var inflater: LayoutInflater? = null

    init {
        inflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        //resources = ctx.getResources();
    }

    override fun getCount(): Int {
        return deviceList.size
    }

    override fun getItem(position: Int): Any {
        return deviceList[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row: View
        if (convertView == null) {
            //Note: Parent is AlertDialog so parent in inflate must be null
            row = inflater!!.inflate(android.R.layout.simple_list_item_single_choice, null)
        } else {
            row = convertView
        }
        val tv = row.findViewById<TextView>(android.R.id.text1)
        //tv.setTextColor(resources.getColor(R.color.black));

        val btDevice = deviceList[position]
        tv.tag = btDevice
        tv.text = btDevice.name

        return tv
    }
}