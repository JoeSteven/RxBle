

# RxBle 蓝牙操作库（RxAndroidBle封装库）

### 依赖

添加以下代码到项目根目录的 build.gradle 中

```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

在要使用该库的module的 build.gradle 中添加依赖，latest-version 请以release-note中发布的最新版本号为准

```groovy
dependencies {
	implementation 'com.github.JoeSteven:RxBle:latest-version'
}
```

### 初始化

在使用该库之前需要调用初始化代码（并不需要在应用启动的时候马上执行，使用前确保初始化就行）

```java
RxBle.init(Global.context(), new BlePermissionRequester());
```

由于 Android 6.0 以上调用蓝牙需要一些运行时权限，因此需要业务层去实现这个接口`RxBle.PermissionRequester`，在权限获取成功后务必要回调给 `PermissionListener`

```java
    public interface PermissionRequester {
        void request(PermissionListener listener, String... permissions);

        boolean hasPermission(String... permissions);
    }
```

### 使用

该库主要提供`RxBle` 和`RxOperator` 两个类，后者可以通过前者构造

- `RxBle` - 主要为一些静态方法
- `RxOperator` -主要为ble的具体操作

#### 1.RxBle

```java
// 打开log日志
void enableLog(boolean enable); 

// 获取RxBleClient
RxBleClient client(); 

// 构造RxBleOperator
RxBleOperator create();

// 蓝牙是否打开
boolean isEnable();

// 注册蓝牙状态变化
PublishSubject<RxBleClient.State> registerState();

// Characteristic 是否支持 read,write,notify,indicate 
boolean isCharacteristicNotifiable(BluetoothGattCharacteristic characteristic);
boolean isCharacteristicReadable(BluetoothGattCharacteristic characteristic);
boolean isCharacteristicWritable(BluetoothGattCharacteristic characteristic);
boolean isCharacteristicIndicatable(BluetoothGattCharacteristic characteristic);
```



#### 2.蓝牙状态恢复

如果希望在使用完蓝牙功能后把状态恢复到使用前的状态（例如，打开蓝牙开小区门禁，开完用户退出应用，自动关闭蓝牙）

```java
// 使用蓝牙功能前调用
Rxble.markState();

// 使用蓝牙完毕后调用
Rxble.restoreState();
```

#### 3.Ble 相关操作

构造 operator 不需要单例，以页面为例的话，最好每个页面持有自己的operator，operator会处理订阅，避免内存泄露

```Java
// 构造 operator
RxBleOperator operator = RxBle.create();

// 打开蓝牙
operator.enable()
		.subscribe(this::success, this::error)
  
// 关闭蓝牙
operator.disable()
  
// 扫描,蓝牙未打开的话会自动打开
operator.scan(scanSettings, scanFilters)
		.subscribe(this::result, this::error);

// 停止扫描
operator.stopScan();

// 连接设备,蓝牙未打开的话会自动打开
operator.setConnectRetryTimes(3)// 重连次数
		.setConnectRetryTimes(1000)// 重连间隔
		.connect(macAddress)// 设备
		.subscribe(this::connectSuccess, this::error);

//断开连接
operator.disconnect();

// 已知连接建立的情况下读取，蓝牙未开或者未连接状态失败，write,notify,indicate同理
operator.readCharacteristic(device, uuid)
		.subscribe(this::success, this::error)
  
// 快捷方式读Characteristic，会自动打开蓝牙，建立连接，write,notify,indicate同理
operator.connect(mDevice)
		.flatMap(RxBleTransformer.readCharacteristic(mDevice, result, mOperator))
		.subscribe(this::success, this::error)

// 管理ble操作的相关订阅
operator.add();

// 释放，该操作会断开连接，停止扫描，移除所有被add过的订阅
operator.release();
```



###  Thanks

[RxAndroidBle](https://github.com/Polidea/RxAndroidBle)