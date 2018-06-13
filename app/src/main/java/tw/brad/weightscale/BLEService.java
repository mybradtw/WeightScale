package tw.brad.weightscale;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.beacon.Beacon;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothLog;

import java.util.UUID;

import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_CONNECTED;
import static com.inuker.bluetooth.library.Constants.STATUS_DISCONNECTED;
import static tw.brad.weightscale.Utils.toHexString;

public class BLEService extends Service {
    private BluetoothClient mClient;
    private boolean isBluetoothOpen = false;
    private String mac = null;

    // GATT Service UUID
    private final UUID serviceUUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");

    // character UUID 通知/寫入
    private final UUID writeUUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private final UUID notifyUUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    private final BluetoothStateListener mBluetoothStateListener = new BluetoothStateListener() {
        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            isBluetoothOpen = openOrClosed;
        }
    };
    private final BleConnectStatusListener mBleConnectStatusListener = new BleConnectStatusListener() {
        @Override
        public void onConnectStatusChanged(String mac, int status) {
            if (status == STATUS_CONNECTED) {
                Log.v("brad", "connected");
                initConnectDevice();
            } else if (status == STATUS_DISCONNECTED) {
                Log.v("brad", "disconnected");
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mClient = new BluetoothClient(this);
        mClient.registerBluetoothStateListener(mBluetoothStateListener);
        if(!isBluetoothOpen) mClient.openBluetooth();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int cmd = intent.getIntExtra("cmd", -1);
        switch (cmd){
            case MainApp.BLEService_CMD_SCAN:
                scanDevices();
                break;
            case MainApp.BLEService_CMD_CONNECT:
                mac = intent.getStringExtra("mac");
                connectDevice();
                break;
            case MainApp.BLEService_CMD_DISCONNECT:
                if (mac != null){
                    mClient.disconnect(mac);
                    mac = null;
                }
                break;

        }
        return super.onStartCommand(intent, flags, startId);
    }

    // Scan BLE Devices
    private void scanDevices(){
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(3000, 3)
                .searchBluetoothClassicDevice(5000)
                .searchBluetoothLeDevice(2000)
                .build();

        mClient.search(request, new SearchResponse() {
            @Override
            public void onSearchStarted() {
                Intent intent = new Intent("BLEService");
                intent.putExtra("mode", MainApp.BLEService_MODE_SCAN_START);
                sendBroadcast(intent);
            }

            @Override
            public void onDeviceFounded(SearchResult device) {
                BluetoothDevice sDevice = device.device;
                if (sDevice != null && sDevice.getName().substring(0,3).equals("MCF")) {
                    Intent intent = new Intent("BLEService");
                    intent.putExtra("mode", MainApp.BLEService_MODE_SCAN_A_DEVICE);
                    intent.putExtra("deviceName", sDevice.getName());
                    intent.putExtra("device", sDevice);
                    sendBroadcast(intent);
                }
            }
            @Override
            public void onSearchStopped() {
                Intent intent = new Intent("BLEService");
                intent.putExtra("mode", MainApp.BLEService_MODE_SCAN_STOP);
                sendBroadcast(intent);
            }

            @Override
            public void onSearchCanceled() {
                Intent intent = new Intent("BLEService");
                intent.putExtra("mode", MainApp.BLEService_MODE_SCAN_CALCEL);
                sendBroadcast(intent);
            }
        });
    }

    // Connect a BLE Device
    private void connectDevice(){
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(3)   // 连接如果失败重试3次
                .setConnectTimeout(30000)   // 连接超时30s
                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();
        mClient.registerConnectStatusListener(mac, mBleConnectStatusListener);
        mClient.connect(mac, options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile profile) {
                if (code == REQUEST_SUCCESS) {

                }
            }
        });
    }

    // Init Connect a BLE Device
    private void initConnectDevice(){
        mClient.notify(mac, serviceUUID, notifyUUID, new BleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {
                // 先將設備傳過來的byte[]轉成16進制
                String mData = toHexString(value);
                Log.v("brad", "notify = " + mData);
            }

            @Override
            public void onResponse(int code) {
                if (code == REQUEST_SUCCESS) {
                    Log.v("brad", "notify:request success");
                }
            }
        });

    }



    @Override
    public void onDestroy() {
        mClient = null;
        if (mac != null){
            mClient.disconnect(mac);
            mac = null;
        }
        super.onDestroy();
    }
}
