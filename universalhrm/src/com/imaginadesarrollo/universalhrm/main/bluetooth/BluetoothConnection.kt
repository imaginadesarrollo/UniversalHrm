package com.imaginadesarrollo.universalhrm.main.bluetooth


/**
 * Created by Kike Bodi (ebp@trifork.com) on 2019-08-27.
 * Copyright by Trifork eHealth. All rights reserved.
 */
interface BluetoothConnection {
  fun getAdapter(): BluetoothConnectionImplementation.BluetoothDeviceAdapter
  fun disconnect()
}