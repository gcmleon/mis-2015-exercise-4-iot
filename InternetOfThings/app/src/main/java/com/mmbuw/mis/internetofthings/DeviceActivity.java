package com.mmbuw.mis.internetofthings;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;


public class DeviceActivity extends Activity {

    // remember to add references to Android example bluetoothLeGatt
    // how to manage errors of connection? ending the app?

    private String name;
    private String address;
    private byte[] scanRecord;
    private int rssi;

    private TextView deviceName;
    private TextView broadcastedData;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private GATTConnection gattConnection;
    // private Handler deviceHandler;

    // Information filled by ShowBTLE
    public static final String DEVICE_NAME = "DEVICE_NAME";
    public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String DEVICE_RECORD = "DEVICE_RECORD";
    public static final String DEVICE_RSSI = "DEVICE_RSSI";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        // Getting information passed by ShowBTLE activity
        Intent deviceIntent = getIntent();
        rssi = deviceIntent.getIntExtra(DEVICE_RSSI, 0);
        scanRecord = deviceIntent.getByteArrayExtra(DEVICE_RECORD);
        address = deviceIntent.getStringExtra(DEVICE_ADDRESS);
        name = deviceIntent.getStringExtra(DEVICE_NAME);

        deviceName = (TextView) findViewById(R.id.deviceName);
        deviceName.setText(name);

        broadcastedData = (TextView) findViewById(R.id.broadcast);
        broadcastedData.setText(parseData(scanRecord));

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

    private String parseData(byte[] data) {

        String firstTranslation = "Data broadcast in ASCII: \n";
        String secondTranslation = "\nData broadcast in Hex: \n";
        String inASCII = "US-ASCII charset is not supported";
        String inHex = "Hexadecimal translation not performed.";
        String parsed;
        StringBuilder sb = new StringBuilder();

        try {
            inASCII = new String(data, "US-ASCII");
            inHex = bytesToHex(data);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            inASCII = "US-ASCII charset is not supported";
        }

        // parsed = firstTranslation + inASCII + secondTranslation + inHex;

        sb.append(firstTranslation);
        sb.append(inASCII);
        sb.append(secondTranslation);
        sb.append(inHex);

        parsed = sb.toString();

        return parsed;
    }

 // http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // public void connectToServer(){

    // }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gattConnection= ((GATTConnection.LocalBinder) service).getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            gattConnection = null;
        }
    };

    public void startGATTConnection (final View view) {

        Intent  gattServiceIntent = new Intent(this, GATTConnection.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }
}
