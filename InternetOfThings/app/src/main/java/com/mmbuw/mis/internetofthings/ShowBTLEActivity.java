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
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/*
 Main activity that shows the list of Bluetooth Low Energy devices in radio range
 */
public class ShowBTLEActivity extends ListActivity {

    private BluetoothAdapter bluetoothAdapter;
    private ListView devicesList;
    private boolean scanning = false;
    private Handler handler;
    ArrayAdapter<String> mArrayAdapter = null;
    private  static final long SCAN_PERIOD = 10000;

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        if (scanning){
            bluetoothAdapter.startLeScan(leScanCallback); //why use leScanCallback??

        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        if (scanning){
            bluetoothAdapter.stopLeScan(leScanCallback); //why use leScanCallback??

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (scanning){
            bluetoothAdapter.stopLeScan(leScanCallback); //why use leScanCallback??
            scanning = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_btle);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        // Set devices list
        devicesList = getListView();
        mArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item);



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

        // Checks if the device doesn't support Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_LONG).show();
            // finish();
        }

        setListAdapter(mArrayAdapter);

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

        Intent deviceIntent = new Intent(this, DeviceActivity.class);
        startActivity(deviceIntent);
    }

    //used with normal bluetooth
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Toast.makeText(getApplicationContext(), "Name or Address"+ device.getName()
                        + "\n" + device.getAddress(), Toast.LENGTH_LONG).show();
                System.out.print("^^");
            }
        }
    };
    /*
     Starts looking for other BTLE devices, as a client
     */
    public void startScan(View view) {
        Toast.makeText(this, "Starting to scan!", Toast.LENGTH_LONG).show();

        scanning = true;
        bluetoothAdapter.startLeScan(leScanCallback); // deprecated for later versions than ours :(

         //bluetoothAdapter.startDiscovery(); // asynchronous call...

        // Coursera bluetooth example
        // It seems to refer only to devices already paired... try this!
        // if not, try using startLeScan


        // The button should be unabled for some time, while the search is being done
        // Probably, enable it again when time is up
        // (coursera code example)


    }
  /*  public void stopScan (){

    }*/


    // Callback to indicate actions on BTLE devices scanning
    // Created to use with startLeScan(leScanCallback), which is deprecated =/
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        Toast.makeText(getApplicationContext(), "Device Address:" + device.getAddress(), Toast.LENGTH_LONG).show();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // bluetoothAdapter.addDevice(device);
                    //bluetoothAdapter.notifyDataSetChanged();
                    System.out.print("Thread" + device.getName() + "\n" + device.getAddress());
                    Toast.makeText(getApplicationContext(), "Device Name:" + device.getName(), Toast.LENGTH_LONG).show();
                    mArrayAdapter.add(device.getName() + device.getAddress());

                }
            });
            // do something with device - maybe on a thread?
        }
    };
}
