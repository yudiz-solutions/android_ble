package com.android.bleconnection.adapter

import android.bluetooth.BluetoothDevice
import com.android.bleconnection.model.SearchModel

/**
 * Created by Hardik Lakhani on 2020-02-25.
 */
interface OnItemClick {
    fun itemClicked(device: BluetoothDevice)
}