# UniversalHrm

Library to manage all types of heart rate monitor devices.

## How to use it:

1- Add the gradle dependency:
```
implementation "com.imaginadesarrollo.universalhrm:universalhrm:2.0.1"
```

2- Implement interface HrmCallbackMethods
```
class YourActivity : Activity(), HrmCallbackMethods{
    private val universalHrm: UniversalHrm by lazy { UniversalHrm(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
    
            scanButton.setOnClickListener {
                // Check location permissions first.
                universalHrm.scan()
            }
    
            universalHrm.disconnect()
        }
}
```

or if you want to implement the callback in another class:
```
class YourActivity : Activity(), HrmCallbackMethods{
    private val callback = YourHrmCallbackImplementation()
    private val universalHrm: UniversalHrm by lazy { UniversalHrm(this,callback) }
    ...
}
```


```
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
```



## Tested devices:

- Polar H7
- CooSpo
- Garmin HRM-Dual
- Suunto Dual Comfort Belt

## Devices to test #helpneeded:

- Zephyr Chest strap (Bluetooth)

## Wishlist:

- Apple Watch
- Fitbit Charge 3
- Garmin Vivoactive 3

## License
Unless otherwise stated, the code for this project is under GNU GPL v3. See [LICENSE](LICENSE) for more information.

