package com.mmbuw.mis.internetofthings;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;


public class DeviceActivity extends Activity {

    // remember to add references to Android example bluetoothLeGatt
    // how to manage errors of connection? ending the app?

    private String name;
    private String address;

    private TextView deviceName;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    // Information filled by ShowBTLE
    public static final String DEVICE_NAME = "";
    public static final String DEVICE_ADDRESS = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        // Getting information passed by ShowBTLE activity
        Intent deviceIntent = getIntent();
        name = deviceIntent.getStringExtra(DEVICE_NAME);
        address = deviceIntent.getStringExtra(DEVICE_ADDRESS);

        deviceName = (TextView) findViewById(R.id.deviceName);
        deviceName.setText(name);

        // Get bluetooth device by its address
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            System.out.println("Bluetooth manager could not be initialized");
            // finish(); ?
        }

        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_LONG).show();
            // finish();
        }

        bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        if (bluetoothDevice == null) {
            Toast.makeText(this, "Device not found.  Unable to connect.", Toast.LENGTH_LONG).show();
            // return false;
        }

        // double check if it's LE?
        // BluetoothLeAdvertiser

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device, menu);
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
}
