package tw.brad.weightscale;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private BleServiceReceiver bleServiceReceiver;
    private ProgressDialog progressDialog;
    private HashMap<String,BluetoothDevice> devices;
    private AlertDialog alertDialog = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // for android 6+
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    0);
        }else{
            init();
        }

    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
    }

    private void init(){
        bleServiceReceiver = new BleServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("BLEService");
        registerReceiver(bleServiceReceiver, filter);

        devices = new HashMap<>();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Scanning...");

        // init BLEService
        Intent intent = new Intent(this, BLEService.class);
        startService(intent);
    }

    public void scan(View view) {
        Intent intent = new Intent(this, BLEService.class);
        intent.putExtra("cmd", MainApp.BLEService_CMD_SCAN);
        startService(intent);
    }

    private void displayDevicesList(){
        Set<String> set = devices.keySet();
        String[] items = new String[devices.size()];
        final BluetoothDevice[] bdevices = new BluetoothDevice[devices.size()];
        int i = 0;
        for(String item : set){
            items[i] = item;
            bdevices[i] = devices.get(item);
            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Devices List");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                connect(bdevices[i].getAddress());
            }
        });
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void connect(String mac){
        Intent intent = new Intent(this, BLEService.class);
        intent.putExtra("cmd", MainApp.BLEService_CMD_CONNECT);
        intent.putExtra("mac", mac);
        startService(intent);
    }

    public void disconnect(View view) {
        Intent intent = new Intent(this, BLEService.class);
        intent.putExtra("cmd", MainApp.BLEService_CMD_DISCONNECT);
        startService(intent);
    }


    private class BleServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int mode = intent.getIntExtra("mode", -1);
            switch(mode){
                case MainApp.BLEService_MODE_SCAN_START:
                    devices.clear();
                    progressDialog.show();
                    break;
                case MainApp.BLEService_MODE_SCAN_A_DEVICE:
                    BluetoothDevice device = intent.getParcelableExtra("device");
                    String deviceName = intent.getStringExtra("deviceName");
                    devices.put(deviceName, device);
                    break;
                case MainApp.BLEService_MODE_SCAN_STOP:
                    progressDialog.dismiss();
                    if (devices.size()>0){
                        displayDevicesList();
                    }else{
                        Toast.makeText(MainActivity.this, "No Weight Scale Found.",
                                Toast.LENGTH_SHORT).show();
                    }

                    break;
                case MainApp.BLEService_MODE_SCAN_CALCEL:
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Scan Canceled",
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    public void finish() {
        if (bleServiceReceiver != null){
            unregisterReceiver(bleServiceReceiver);
        }
        // destroy BLEService
        Intent intent = new Intent(this, BLEService.class);
        stopService(intent);

        super.finish();
    }
}
