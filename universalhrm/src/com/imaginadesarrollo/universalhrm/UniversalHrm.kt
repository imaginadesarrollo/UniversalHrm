package com.imaginadesarrollo.universalhrm

import android.support.v7.app.AppCompatActivity
import com.imaginadesarrollo.universalhrm.main.HrmImplementation
import com.imaginadesarrollo.universalhrm.main.UniversalHrmImplementation

class UniversalHrm(activity: AppCompatActivity, callbackMethods: HrmCallbackMethods? = null): HrmImplementation {

    private val universalHrmImplementatiom = UniversalHrmImplementation(activity, callbackMethods)

    override fun scan() = universalHrmImplementatiom.scan()

    /*override fun connect() = universalHrmImplementatiom.connect()

    override fun disconnect() = universalHrmImplementatiom.disconnect()

    override fun isConnected(): Boolean = universalHrmImplementatiom.isConnected()

    override fun isThereSavedDevice(): Boolean = universalHrmImplementatiom.isThereSavedDevice()*/
}