package com.imaginadesarrollo.universalhrm.manager.mock

import android.app.Activity
import android.content.Context
import android.widget.ArrayAdapter
import com.imaginadesarrollo.universalhrm.HrmCallbackMethods
import com.imaginadesarrollo.universalhrm.manager.HrmConnection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit

/**
 * Created by Kike Bodi on 2019-09-18.
 */
class MockConnectionImpl(private val context: Context,
                         private val callback: HrmCallbackMethods) : HrmConnection {

    private var disposable = CompositeDisposable()

    init {
        disposable.clear()
        var isFirstEmission = true

        callback.setBatteryLevel(100)

        val randomObservable = Observable.range(60, 180)
        val delayObservable = Observable.interval(1, TimeUnit.SECONDS)

        val observable = Observable.zip(randomObservable, delayObservable, BiFunction<Int, Long, Int> { random, delay -> return@BiFunction random })
                .doOnSubscribe {
                    callback.onConnectionRequest()
                    callback.setHeartRateMonitorProviderName("Mock device")
                    callback.setHeartRateMonitorName("Fake HRM")
                    callback.setHeartRateMonitorAddress("XX:XX:XX:XX")
                }
                .onErrorReturn { 0 }
                .delay(3, TimeUnit.SECONDS)
                .subscribe {
                    (context as Activity).runOnUiThread {
                        if (isFirstEmission) {
                            callback.onDeviceConnected()
                            isFirstEmission = false
                        }
                        callback.setHeartRateValue(it)
                    }
                }

        disposable.addAll(observable)
    }


    override fun getAdapter(): ArrayAdapter<*> { return ArrayAdapter<String>(context, 0)} // not used
    override fun disconnect() {
        callback.onDeviceDisconnected()
        disposable.clear()
    }
    override fun addAlertDialogCallback(callback: HrmConnection.AlertDialogCallback) {}


}