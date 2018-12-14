package com.example.ianweston.clptrackerandroid

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView


class BluetoothDeviceAdapter(private val context: Context, private val dataSource: ArrayList<BluetoothDeviceModel>): BaseAdapter() {
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long{
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.bluetoothdevice, parent, false)

        var deviceName = rowView.findViewById(R.id.bluetoothDeviceName) as TextView
        var deviceDistance = rowView.findViewById(R.id.bluetoothDeviceDistance) as TextView

        var bluetoothDevice = getItem(position) as BluetoothDeviceModel

        deviceName.text = bluetoothDevice.deviceName
        deviceDistance.text = bluetoothDevice.deviceDistance

        return rowView
    }
}