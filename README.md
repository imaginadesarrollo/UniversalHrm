# UniversalHrm

Library to manage all types of heart rate monitor devices.

How to use it:

1- Add the gradle dependency:
```
implementation "com.imaginadesarrollo.universalhrm:universalhrm:1.0.0"
```

Implement interface HrmCallbackMethods
```
class YourActivity : Activity(), HrmCallbackMethods{
    private val universalHrm: UniversalHrm by lazy { UniversalHrm(this) }
    ...
}
```

or if you want to implement the callback in another class:
```
class YourActivity : Activity(), HrmCallbackMethods{
    private val callback = YourHrmCallbackImplementation()
    private val universalHrm: UniversalHrm by lazy { UniversalHrm(this,callback) }
    ...
}

class YourHrmCallbackImplementation: HrmCallbackMethods {
    override fun deviceNotSupported() {}

    override fun onDeviceConnected() {}

    override fun onDeviceDisconnected() {}

    override fun scanCanceled() {}

    override fun setBatteryLevel(level: Int) {}

    override fun setHeartRateMonitorAddress(address: String) {}

    override fun setHeartRateMonitorName(name: String) {}

    override fun setHeartRateMonitorProviderName(providerName: String) {}

    override fun setHeartRateValue(value: Int) {}
}

```

To clone the repo:

```
git clone https://github.com/imaginadesarrollo/UniversalHrm
cd UniversalHrm/universalhrm/
git clone https://github.com/ant-wireless/ANT-Android-SDKs

```

Tested devices:

- Polar H7
- 
- 
- 

