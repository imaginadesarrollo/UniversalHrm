package com.imaginadesarrollo.universalhrm

import com.imaginadesarrollo.universalhrm.main.HrmImplementation
import com.imaginadesarrollo.universalhrm.main.UniversalHrmImplementation

class UniversalHrm(activity: android.support.v7.app.AppCompatActivity): HrmImplementation {

    private val universalHrmImplementatiom = UniversalHrmImplementation(activity)

    override fun scan() = universalHrmImplementatiom.scan()

    override fun connect() = universalHrmImplementatiom.connect()

    override fun disconnect() = universalHrmImplementatiom.disconnect()

    override fun isConnected(): Boolean = universalHrmImplementatiom.isConnected()

    override fun isThereSavedDevice(): Boolean = universalHrmImplementatiom.isThereSavedDevice()
}