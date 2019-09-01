package com.imaginadesarrollo.universalhrmapp

import com.imaginadesarrollo.universalhrm.HrmCallbackMethods

/**
 * Copyright Imagina Desarrollo
 * @author Kike Bodi (bodi.inf@gmail.com) on 23/01/2019
 */
class YourHrmCallbackImplementation: HrmCallbackMethods {
    override fun onConnectionRequest() {
        // implement
    }

    override fun deviceNotSupported() {
        // implement
    }

    override fun onDeviceConnected() {
        // implement
    }

    override fun onDeviceDisconnected() {
        // implement
    }

    override fun setBatteryLevel(level: Int) {
        // implement
    }

    override fun setHeartRateMonitorAddress(address: String) {
        // implement
    }

    override fun setHeartRateMonitorName(name: String) {
        // implement
    }

    override fun setHeartRateMonitorProviderName(providerName: String) {
        // implement
    }

    override fun setHeartRateValue(value: Int) {
        // implement
    }
}