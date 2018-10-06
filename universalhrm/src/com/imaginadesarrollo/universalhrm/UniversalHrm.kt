package com.imaginadesarrollo.universalhrm

import android.app.Activity
import com.imaginadesarrollo.universalhrm.main.HrmImplementation
import com.imaginadesarrollo.universalhrm.main.UniversalHrmImplementation

class UniversalHrm(activity: Activity): HrmImplementation {

    private val universalHrmImplementatiom = UniversalHrmImplementation(activity)

    override fun scan() = universalHrmImplementatiom.scan()

    override fun connect() = universalHrmImplementatiom.connect()

    override fun disconnect() = universalHrmImplementatiom.disconnect()

    override fun isConnected(): Boolean = universalHrmImplementatiom.isConnected()

    override fun isThereSavedDevice(): Boolean = universalHrmImplementatiom.isThereSavedDevice()
}