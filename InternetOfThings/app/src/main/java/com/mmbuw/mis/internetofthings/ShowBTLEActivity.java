package com.mmbuw.mis.internetofthings;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.TimerTask;
import java.util.Timer;
import java.util.logging.LogRecord;

/*
 Main activity that shows the list of Bluetooth Low Energy devices in radio range
 */
public class ShowBTLEActivity extends ListActivity {

    private BluetoothAdapter bluetoothAdapter;
    private ListView devicesList;
    private boolean scanning = false;
    private Handler mHandler;
    private ArrayAdapter<String> mArrayAdapter;
    private ArrayList<BluetoothDevice> leDevices;
    private static final long SCAN_PERIOD = 50000;
    private byte[] broadcastedData;
    private int deviceRSSI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_btle);
        //IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //registerReceiver(mReceiver, filter);
        // Set devices list
        devicesList = getListView();
        mArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item);
        leDevices = new ArrayList<BluetoothDevice>();
        mHandler = new Handler();

        // Check if device is a BTLE-capable one
        // https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.btle_not_supported, Toast.LENGTH_LONG).show();
            // finish();
        } else {
            Toast.makeText(this, "BTLE supported", Toast.LENGTH_LONG).show();
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if the device doesn't support Bluetooth or it's not enabled
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_LONG).show();
            // It asks the user to enable bluetooth
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBluetooth);
            finish();
            return;
        }

        // bluetoothAdapter.getDefaultAdapter();
        setListAdapter(mArrayAdapter);
    }

    /*
    NOT SURE ABOUT THIS CODE!
    Check Android example...
     */
    @Override
    protected void onResume() {
        super.onResume();
        // IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //registerReceiver(mReceiver, filter);
        if (scanning){
            beginScan();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mReceiver);
        if (scanning){
            stopScan();
            mArrayAdapter.clear(); //remove previous scan results
            leDevices.clear();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(mReceiver);
        if (scanning){
            stopScan();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_btle, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     When a device is chosen, it passes to the activity where the device information is shown
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // passing name, address to next activity
        BluetoothDevice selectedDevice = leDevices.get(position);

        if (selectedDevice == null) {
            return;
        }

        Intent deviceIntent = new Intent(this, DeviceActivity.class);
        deviceIntent.putExtra(DeviceActivity.DEVICE_NAME, selectedDevice.getName());
        deviceIntent.putExtra(DeviceActivity.DEVICE_ADDRESS, selectedDevice.getAddress());
        deviceIntent.putExtra(DeviceActivity.DEVICE_RECORD, broadcastedData);
        deviceIntent.putExtra(DeviceActivity.DEVICE_RSSI, deviceRSSI);

        if (scanning) {
            stopScan();
        }

        startActivity(deviceIntent);
    }

    //used with normal bluetooth

    /*
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Toast.makeText(getApplicationContext(), "CLASSIC: Name or Address" + device.getName()
                               + "\n" + device.getAddress(), Toast.LENGTH_LONG).show();
                leDevices.add(device);

                System.out.print("^^");
            }
        }
    };*/

    // Starts looking for other BTLE devices, as a client
    public void startScan(final View view) {

        beginScan();

        /*
       final Timer timer = new Timer();
        runOnUiThread(new Runnable() {
                          public void run() {
                              timer.schedule(new TimerTask() {
                                  @Override

                                  public void run() {

                                      Looper.prepare();
                                      // http://developer.android.com/guide/components/processes-and-threads.html
                                      // update UI thread after timeout finished on worker thread
                                      bluetoothAdapter.getDefaultAdapter();

                                      view.post(new Runnable() {
                                          @Override
                                          public void run() {
                                              scanning = false;
                                              System.out.print("Stop Scanning!");
                                              Toast.makeText(getApplicationContext(), "Time out! Stop Scanning!", Toast.LENGTH_LONG).show();
                                              bluetoothAdapter.stopLeScan(leScanCallback);
                                          }
                                      });

                                      Looper.loop();

                                  }
                              }, SCAN_PERIOD);
                          }
                      });*/

    }

    private void beginScan () {

        mArrayAdapter.clear(); //remove previous scan results
        leDevices.clear();

        Toast.makeText(this, "Before starting to scan", Toast.LENGTH_LONG).show();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
                Toast.makeText(getApplicationContext(), "Scanning stopped", Toast.LENGTH_LONG).show();
            }
        }, SCAN_PERIOD);


        /* Other options:
        The button could be disabled for some time, while the search is being done
        Probably, enable it again when time is up
         */

        Toast.makeText(this, "Starting to scan", Toast.LENGTH_LONG).show();
        scanning = true;
        bluetoothAdapter.startLeScan(leScanCallback);

    }

    private void stopScan () {
        scanning = false;
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    // Callback to indicate actions on BTLE devices scanning
    // Created to use with startLeScan(leScanCallback), which is deprecated =/
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            Toast.makeText(getApplicationContext(), "Device Address:" + device.getAddress(), Toast.LENGTH_LONG).show();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // bluetoothAdapter.getDefaultAdapter();

                    System.out.print("Thread" + device.getName() + "\n" + device.getAddress());
                    Toast.makeText(getApplicationContext(), "BTLE: Device Name:" + device.getName(), Toast.LENGTH_LONG).show();

                    mArrayAdapter.add(device.getName() + device.getAddress());
                    leDevices.add(device);
                    mArrayAdapter.notifyDataSetChanged();

                    broadcastedData = scanRecord;
                    deviceRSSI = rssi;

                }
            });
            // do something with device - maybe on a thread?
        }
    };
}
