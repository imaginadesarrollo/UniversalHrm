package com.imaginadesarrollo.universalhrm

interface HrmCallbackMethods {

    fun setBatteryLevel(level: Int)
    fun setHeartRateValue(value: Int)
    fun setHeartRateMonitorName(name: String)
    fun setHeartRateMonitorAddress(address: String)
    fun setHeartRateMonitorProviderName(providerName: String)
    fun deviceNotSupported()
    fun scanCanceled()
    fun onDeviceConnected()
    fun onDeviceDisconnected()
}