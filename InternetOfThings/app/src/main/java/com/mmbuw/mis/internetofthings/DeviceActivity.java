package com.mmbuw.mis.internetofthings;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 Activity in charge of showing the data broadcast and the list of services and characteristics.
 It calls the service GATTConnection to perform the connection and updates the interface
 according to the results obtained by the listener.
 Reference: http://developer.android.com/samples/BluetoothLeGatt/index.html
 */
public class DeviceActivity extends Activity {

    private final static String TAG = DeviceActivity.class.getSimpleName();

    private String name;
    private String address;
    private byte[] scanRecord;
    private int rssi;

    private TextView deviceName;
    private TextView broadcastedData;
    private TextView mConnectionState;
    private TextView mDataField;
    private ExpandableListView mGattServicesList;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private GATTConnection gattConnection;

    // Information filled by ShowBTLE
    public static final String DEVICE_NAME = "DEVICE_NAME";
    public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String DEVICE_RECORD = "DEVICE_RECORD";
    public static final String DEVICE_RSSI = "DEVICE_RSSI";
    private boolean connected = false;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothGattCharacteristic notifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private boolean alreadyPressed = false;


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


        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        // Get bluetooth device by its address
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            System.out.println("Bluetooth manager could not be initialized");
            finish();
        }

        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_LONG).show();
            // finish();
        }

        bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        if (bluetoothDevice == null) {
            Toast.makeText(this, "Device not found.  Unable to connect.", Toast.LENGTH_LONG).show();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (GATTConnection.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (GATTConnection.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                //clearUI();
            } else if (GATTConnection.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(gattConnection.getSupportedGattServices());
            } else if (GATTConnection.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(GATTConnection.EXTRA_DATA));
            }
        }
    };

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

        sb.append(firstTranslation);
        sb.append(inASCII + "\n");
        sb.append(secondTranslation);
        sb.append(inHex + "\n");

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

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gattConnection = ((GATTConnection.LocalBinder) service).getService();

            if (!gattConnection.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            gattConnection.connect(address);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            gattConnection = null;
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, gattService.toString());
            //currentServiceData.put(LIST_NAME, unknownServiceString);
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                String serviceValue = gattCharacteristic.toString();

                currentCharaData.put(LIST_NAME, serviceValue);
                //currentCharaData.put(LIST_NAME, unknownCharaString);
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    public void startGATTConnection(final View view) {

        if (!alreadyPressed) {
            mGattServicesList.setOnChildClickListener(servicesListClickListener);

        } else {
            unregisterReceiver(mGattUpdateReceiver);
            unbindService(serviceConnection);
            clearUI();

        }

        Intent gattServiceIntent = new Intent(this, GATTConnection.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (gattConnection != null) {
            final boolean result = gattConnection.connect(address);
            Log.d(TAG, "Connect request result =" + result);
        }

    }

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }


    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private final ExpandableListView.OnChildClickListener servicesListClickListener = new ExpandableListView.OnChildClickListener() {

        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

            if (mGattCharacteristics != null) {
                final BluetoothGattCharacteristic characteristic =
                        mGattCharacteristics.get(groupPosition).get(childPosition);
                final int charaProp = characteristic.getProperties();
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user interface.
                    if (notifyCharacteristic != null) {
                        gattConnection.setCharacteristicNotification(notifyCharacteristic, false);
                        notifyCharacteristic = null;
                    }
                    gattConnection.readCharacteristic(characteristic);
                }
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    notifyCharacteristic = characteristic;
                    gattConnection.setCharacteristicNotification(characteristic, true);
                }
                return true;
            }
            return false;
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GATTConnection.ACTION_GATT_CONNECTED);
        intentFilter.addAction(GATTConnection.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(GATTConnection.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(GATTConnection.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
