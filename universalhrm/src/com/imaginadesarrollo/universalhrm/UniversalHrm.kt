package com.imaginadesarrollo.universalhrm

import android.app.Activity
import com.imaginadesarrollo.universalhrm.manager.HrmManager
import com.imaginadesarrollo.universalhrm.manager.HrmManagerImpl

class UniversalHrm(activity: Activity, callbackMethods: HrmCallbackMethods? = null): HrmManager{

    private val universalHrmImplementation = HrmManagerImpl(activity, callbackMethods)

    override fun scan() = universalHrmImplementation.scan()

    override fun disconnect() = universalHrmImplementation.disconnect()
}