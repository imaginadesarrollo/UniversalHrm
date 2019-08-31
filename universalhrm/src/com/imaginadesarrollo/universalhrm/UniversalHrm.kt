package com.imaginadesarrollo.universalhrm

import android.support.v7.app.AppCompatActivity
import com.imaginadesarrollo.universalhrm.manager.HrmManager
import com.imaginadesarrollo.universalhrm.manager.HrmManagerImpl

class UniversalHrm(activity: AppCompatActivity, callbackMethods: HrmCallbackMethods? = null): HrmManager{

    private val universalHrmImplementatiom = HrmManagerImpl(activity, callbackMethods)

    override fun scan() = universalHrmImplementatiom.scan()

    override fun disconnect() = universalHrmImplementatiom.disconnect()
}