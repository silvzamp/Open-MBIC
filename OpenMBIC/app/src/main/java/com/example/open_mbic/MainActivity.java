package com.example.open_mbic;

import static java.lang.String.valueOf;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ScanResultAdapter.OnScanListener {

    int ENABLE_BLUETOOTH_REQUEST_CODE = 1;
    int LOCATION_PERMISSION_REQUEST_CODE = 2;

    boolean isSensor2tracked = false;
    boolean isSensor3tracked = false;
    boolean isDemo = false;
    boolean toTX = false;
    boolean tosavelocally = false;

    // VARIABLES FOR DESIGN
    Button scan_button;
    Button read_button;
    Button flush_button;
    Button restart_button;

    Switch demo_switch;
    Switch server_switch;
    Switch save_switch;

    TextView param_tv;
    TextView param_tv2a;
    TextView param_tv2b;
    TextView ip_info;
    TextView ip_address;

    // VARIABLES FOR SCROLLING LIST
    RecyclerView recyclerView;
    ArrayList<ScanResult> scanResults = new ArrayList<ScanResult>();
    ScanResultAdapter scanResultAdapter = new ScanResultAdapter(scanResults, this);

    // VARIABLES FOR BLUETOOTH
    BluetoothAdapter btAdapter;
    BluetoothLeScanner bleScanner;
    BluetoothGatt GlobalGatt1;
    BluetoothGatt GlobalGatt2;
    BluetoothGatt GlobalGatt3;

    // VARIABLES FOR TCP/IP TX
    String ipString = "";

    // DATA BUFFERS for each device
    // Some sensors may require more buffers
    ArrayList<String> buffer1TX = new ArrayList<String>();
    ArrayList<String> buffer1SAVE = new ArrayList<String>();
    ArrayList<String> buffer1TIME = new ArrayList<String>();
    ArrayList<String> buffer2aTX = new ArrayList<String>();
    ArrayList<String> buffer2aSAVE = new ArrayList<String>();
    ArrayList<String> buffer2aTIME = new ArrayList<String>();
    ArrayList<String> buffer2bTX = new ArrayList<String>();
    ArrayList<String> buffer2bSAVE = new ArrayList<String>();
    ArrayList<String> buffer2bTIME = new ArrayList<String>();
    ArrayList<String> buffer3TX = new ArrayList<String>();
    ArrayList<String> buffer3SAVE = new ArrayList<String>();
    ArrayList<String> buffer3TIME = new ArrayList<String>();

    // TO STORE DATA LOCALLY
    private static final int CREATE_FILE = 1;
    Uri uri;
    ParcelFileDescriptor parcelFileDescriptor = null;
    FileOutputStream fileOutputStream = null;

    /*
    Function to create a file with the results are saved locally.
    */
    protected void createFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/csv");

        Calendar cal = Calendar.getInstance();
        int const_year = 1;
        int const_month = 2 ;
        int const_day = 5;
        int const_hour = 11;
        int const_minute = 12;

        int day = cal.get(const_day);
        int month = cal.get(const_month) + 1;
        int year = cal.get(const_year);
        int hour = cal.get(const_hour);
        int minute = cal.get(const_minute);

        String filename = "APPLICATIONNAME_" + year + month +
                day + "_" + hour + "h" + minute + ".csv";
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        startActivityForResult(intent, CREATE_FILE);

        onActivityResult(CREATE_FILE, CREATE_FILE, intent);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK && requestCode == CREATE_FILE)
        {
            // The result data contains a URI for the document or directory that
            // the user selected.
            if (resultData != null) {
                uri = resultData.getData();

                // Write header
                try {
                    parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "w");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
                try {
                    fileOutputStream.write(("Value1Sensor2_Timestamps" + "\tValue1Sensor2_Value" + "\tValue2Sensor2_Timestamps" + "\tValue2Sensor2_value" + "\tValue1Sensor1_Timestamps" + "\tValue1Sensor1_value" + "\tValue1Sensor3_Timestamps" + "\tValue1Sensor3_value\n").getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
    Function to fill the file with the data that are temporarily stored in the buffers.
    */
    protected void writeData(Uri uri) {
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "wa");
            fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

            while (buffer2aSAVE.size() > 0 || buffer2bSAVE.size() > 0 || buffer1SAVE.size() > 0 || buffer3SAVE.size() > 0) {
                if (buffer2aSAVE.size() > 0) {
                    fileOutputStream.write((buffer2aTIME.get(0)).getBytes());
                    fileOutputStream.write(("\t" + buffer2aSAVE.get(0) + "\t").getBytes());

                    buffer2aSAVE.remove(0);
                    buffer2aTIME.remove(0);
                } else
                    fileOutputStream.write(("\t\t").getBytes());

                if (buffer2bSAVE.size() > 0) {
                    fileOutputStream.write((buffer2bTIME.get(0)).getBytes());
                    fileOutputStream.write(("\t" + buffer2bSAVE.get(0) + "\t").getBytes());

                    buffer2bSAVE.remove(0);
                    buffer2bTIME.remove(0);
                } else
                    fileOutputStream.write(("\t\t").getBytes());

                if (buffer1SAVE.size() > 0) {
                    fileOutputStream.write((buffer1TIME.get(0)).getBytes());
                    fileOutputStream.write(("\t" + buffer1SAVE.get(0)).getBytes());

                    buffer1SAVE.remove(0);
                    buffer1TIME.remove(0);

                } else
                    fileOutputStream.write(("\t\t\t").getBytes());

                if (buffer3SAVE.size() > 0) {
                    fileOutputStream.write((buffer3TIME.get(0)).getBytes());
                    fileOutputStream.write(("\t" + buffer3SAVE.get(0)).getBytes());
                    buffer3SAVE.remove(0);
                    buffer3TIME.remove(0);
                }

                fileOutputStream.write(("\n").getBytes());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkPermission(btAdapter);
        bleScanner = btAdapter.getBluetoothLeScanner();

        scan_button = findViewById(R.id.id_scan_button);

        read_button = findViewById(R.id.id_read_button);
        read_button.setVisibility(View.INVISIBLE);

        flush_button = findViewById(R.id.id_flush_button);
        flush_button.setVisibility(View.INVISIBLE);

        param_tv = findViewById(R.id.id_param_tv);
        param_tv.setVisibility(View.INVISIBLE);

        param_tv2a = findViewById(R.id.id_param_tv2a);
        param_tv2a.setVisibility(View.INVISIBLE);

        param_tv2b = findViewById(R.id.id_param_tv2b);
        param_tv2b.setVisibility(View.INVISIBLE);

        ip_info = findViewById(R.id.id_ipText);
        ip_info.setVisibility(View.INVISIBLE);

        ip_address = findViewById(R.id.id_ipText2);
        ip_address.setVisibility(View.INVISIBLE);

        demo_switch = findViewById(R.id.id_demo_switch);
        demo_switch.setVisibility(View.INVISIBLE);

        server_switch = findViewById(R.id.id_server_switch);
        server_switch.setVisibility(View.INVISIBLE);

        save_switch = findViewById(R.id.id_save_switch);
        save_switch.setVisibility(View.INVISIBLE);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setVisibility(View.INVISIBLE);
        // Attach the adapter to the recyclerview to populate items
        recyclerView.setAdapter(scanResultAdapter);
        // Set layout manager to position the items
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        server_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ip_address.setVisibility(View.VISIBLE);
                    ip_info.setVisibility(View.VISIBLE);
                    demo_switch.setVisibility(View.VISIBLE);
                    toTX = true;
                } else {
                    ip_address.setVisibility(View.INVISIBLE);
                    ip_info.setVisibility(View.INVISIBLE);
                    demo_switch.setVisibility(View.INVISIBLE);
                    toTX = false;
                }
            }
        });

        demo_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    flush_button.setVisibility(View.VISIBLE);
                    isDemo = true;
                } else {
                    flush_button.setVisibility(View.INVISIBLE);
                    isDemo = false;
                }
            }
        });

        save_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    createFile();
                    tosavelocally = true;
                }
                else {
                    Toast.makeText(MainActivity.this, "The app stops recording", Toast.LENGTH_LONG).show();
                    tosavelocally = false;

                    // This writes all data in all buffers, at the end of the acquisition phase
                    // This because we access the file just once, reducing delay and errors in formatting data
                    writeData(uri);

                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (parcelFileDescriptor != null) {
                        try {
                            parcelFileDescriptor.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        scan_button.setOnClickListener(v -> startBleScan());

        flush_button.setOnClickListener(v -> {
            if (buffer1TX.size() > 0) {
                buffer1TX.clear();
            }
            if (buffer2aTX.size() > 0) {
                buffer2aTX.clear();
            }
            if (buffer2bTX.size() > 0) {
                buffer2bTX.clear();
            }
            if (buffer3TX.size() > 0) {
                buffer3TX.clear();
            }
        });

        ip_address.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.MaterialAlertDialog_MaterialComponents_Title_Text_CenterStacked);
            final View ipAddressDialog = getLayoutInflater().inflate(R.layout.ip_address_dialog, null);
            builder.setView(ipAddressDialog);
            builder.setTitle("Type IP Server Address");
            builder.setPositiveButton("Ok", (dialog, which) -> {
                EditText ipAddressDialogEditText = ipAddressDialog.findViewById(R.id.id_dialog_edit_text);
                ipString = ipAddressDialogEditText.getText().toString();
                if (!ipString.equals("")) {
                    String lastChar = ipString.substring(ipString.length() - 1);
                    if (lastChar.equals(" ")) {
                        ipString = ipString.substring(0, ipString.length() - 1);
                    }
                    ip_address.setText(ipString);
                }
            });
            builder.setNegativeButton("no", (dialog, id) -> {
                // User cancelled the dialog
                Toast.makeText(MainActivity.this, "The app won't transmit at all", Toast.LENGTH_LONG).show();
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        });
        read_button.setOnClickListener(v -> {
            if (!isSensor2tracked) {
                try {
                    enableSensor2Service2Tracking();
                    isSensor2tracked = true;
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!isSensor3tracked) {
                try {
                    //enable IMU tracking
                    enableSensor3Tracking();
                    isSensor3tracked = true;
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //enable all the other communication
            enableNotification();
        });


        restart_button = findViewById(R.id.restart_button);
        restart_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Finish to close the app processes
                finish();
                // To open our application
                startActivity(getIntent());
                overridePendingTransition(0, 0);
            }
        });
    } // end onCreate


    private void startBleScan() {
        recyclerView.setVisibility(View.VISIBLE);
        scanResults.clear();
        scanResultAdapter.notifyDataSetChanged();
        Toast.makeText(MainActivity.this,"Scanning Started",Toast.LENGTH_SHORT).show();
        bleScanner.startScan(scanCallback);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String address = device.getAddress();
            String name = device.getName();

            if(scanResults.size()>0){
                boolean flag = false;
                int position = 0;
                while(!flag && position<scanResults.size()){
                    ScanResult tmp_scan = scanResults.get(position);
                    String tmp_mac = tmp_scan.getDevice().getAddress();
                    if(tmp_mac.equals(address)){
                        flag = true;
                        break;
                    }
                    position++;
                }

                if(flag){
                    // The element was already scanned - just upload values
                    scanResults.set(position, result);
                    scanResultAdapter.notifyItemChanged(position);
                } else {
                    // New discovery, add the element
                    scanResults.add(result);
                    scanResultAdapter.notifyItemInserted(scanResults.size() - 1);
                }
            } else {
                // First discovery
                scanResults.add(result);
                scanResultAdapter.notifyDataSetChanged();
            }
        }
    };

    public void checkPermission(BluetoothAdapter mBtAdapter){
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Location and Bluetooth permission required");
            builder.setMessage("Do you want to activate the Bluetooth and allow the location access?");
            // Add the buttons
            builder.setPositiveButton("ok", (dialog, id) -> {
                Intent enableBtIntent = new Intent();
                enableBtIntent.setAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE);
                // VERY IMPORTANT STEP: the permission to access location, otherwise the scancallback does not start without any error
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                }
            });
            builder.setNegativeButton("no", (dialog, id) -> {
                // User cancelled the dialog
                Toast.makeText(MainActivity.this,"Please, reboot the app",Toast.LENGTH_LONG).show();
            });
            // Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    @Override
    public void onScanClick(int position) {
        BluetoothDevice device4connection = scanResults.get(position).getDevice();
        bleScanner.stopScan(scanCallback);
        String name = device4connection.getName();

        String deviceName1 = "SENSOR NAME 1";
        String deviceName2 = "SENSOR NAME 2";
        String deviceName3 = "SENSOR NAME 3";

        if(name.startsWith(deviceName1)){
            GlobalGatt1 = device4connection.connectGatt(this, false, gattCallback);
        }
        else if(name.startsWith(deviceName2)){
            GlobalGatt2 = device4connection.connectGatt(this, false, gattCallback);
        }
        else if(name.startsWith(deviceName3)){
            GlobalGatt3 = device4connection.connectGatt(this, false, gattCallback);
        }
        read_button.setVisibility(View.VISIBLE);
        save_switch.setVisibility(View.VISIBLE);
        server_switch.setVisibility(View.VISIBLE);
        param_tv.setVisibility(View.VISIBLE);
        param_tv2a.setVisibility(View.VISIBLE);
        param_tv2b.setVisibility(View.VISIBLE);
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String deviceAddress = gatt.getDevice().getAddress();
            String deviceName = gatt.getDevice().getName();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to " + deviceAddress);
                    MyRunnable myRunnable = new MyRunnable(deviceName);
                    MainActivity.this.runOnUiThread(myRunnable);
                    // VERY IMPORTANT STEP: without complete discovery the connection will crash
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from " + deviceAddress);
                    gatt.close();
                }
            } else {
                Log.w("BluetoothGattCallback", "Error " + status + " encountered for " + deviceAddress + "! Disconnecting...");
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            List<BluetoothGattService> serviceList = gatt.getServices();

            for (BluetoothGattService service : serviceList) {
                Log.i("BluetoothGattServices", "Service " + service.getUuid());
                List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristicList) {
                    Log.i("BluetoothGattChars", "Characteristic " + characteristic.getUuid());
                }
            }
            MyRunnable myRunnable = new MyRunnable("You can READ");
            MainActivity.this.runOnUiThread(myRunnable);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("MTU changed", "onMtuChanged: " + mtu);
            } else {
                Log.d("MTU changed failed", "onMtuChanged: " + mtu);
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] value = characteristic.getValue();
            Log.d("READ", "onCharacteristicRead: " + value);
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("WRITE", "onCharacteristicWrite: OK");
            }
        }


        /* In this method you have the access to data from your sensors
        Remember that usually is necessary a decoding, at least from byte array -> hex numbers -> decimal numbers
        You can:
         - show them on display
         - save locally
         - retransmit

         Here some examples.
        */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            byte[] value = characteristic.getValue();

            if (characteristic.getUuid().toString().equals(GattAttributes.Sensor1Char.toString())) {

                int valueSensor1 = value[1];
                param_tv.setText("Value 1 - Sensor 1: " + valueSensor1);

                if (tosavelocally) {
                    buffer1TIME.add(String.valueOf(System.currentTimeMillis()));
                    buffer1SAVE.add(valueOf(valueSensor1));
                }

                if (toTX) {
                    buffer1TX.add(valueOf(valueSensor1));
                    new ConnAsyncTask(ipString, 6661, buffer1TX).execute();
                }

            }
            if (characteristic.getUuid().toString().equals(GattAttributes.Sensor2Char.toString())) {
                String value1Sensor2 = Utils.byteArrayToHexString(value);
                param_tv2a.setText("Value 1 - Sensor 2: " + value1Sensor2);

                if (tosavelocally) {
                    buffer2aTIME.add(String.valueOf(System.currentTimeMillis()));
                    buffer2aSAVE.add(value1Sensor2);
                }

                if (toTX) {
                    buffer2aTX.add(value1Sensor2);
                    new ConnAsyncTask(ipString, 6660, buffer2aTX).execute();
                }
            }
            if (characteristic.getUuid().toString().equals(GattAttributes.Sensor2CharData.toString())) {

                int flag = characteristic.getProperties();
                int format = -1;

                // Heart rate bit number format
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                }
                final int value2Sensor2 = characteristic.getIntValue(format, 1);
                param_tv2b.setText("Value 2 - Sensor 2: " + value2Sensor2);
                if (tosavelocally) {
                    buffer2bTIME.add(String.valueOf(System.currentTimeMillis()));
                    buffer2bSAVE.add(String.valueOf(value2Sensor2));
                }
            }

            if (characteristic.getUuid().toString().equals(GattAttributes.Sensor3CharData.toString())) {
                String valueSensor3 = Utils.byteArrayToHexString(value);
                Log.d("Sensor 3 - Data", valueSensor3);

                if (toTX) {
                    buffer3TX.add(valueSensor3);
                    new ConnAsyncTask(ipString, 6662, buffer3TX).execute();
                }

                if (tosavelocally) {
                    buffer3TIME.add(String.valueOf(System.currentTimeMillis()));
                    buffer3SAVE.add(valueSensor3);

                }
            }
        }
    };

    public class MyRunnable implements Runnable {
        private final String message;
        MyRunnable(final String message) {
            this.message = message;
        }
        public void run() {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }


    /*
    Depending on the sensor firmware, the protocol to enable notification changes.
    Here some examples.
    */
    private void enableNotification(){
        if(GlobalGatt1!=null) {
            //Sensor1 Monitor
            BluetoothGattCharacteristic sensor1Char = GlobalGatt1.getService(GattAttributes.Sensor1Service).getCharacteristic(GattAttributes.Sensor1Char);
            GlobalGatt1.setCharacteristicNotification(sensor1Char, true);
            BluetoothGattDescriptor sensor1Descr = sensor1Char.getDescriptor(GattAttributes.SensorsDescr);
            sensor1Descr.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            GlobalGatt1.writeDescriptor(sensor1Descr);
        }

        if(GlobalGatt2 != null) {
            // Sensor2 Monitor
            BluetoothGattCharacteristic sensor2Char = GlobalGatt2.getService(GattAttributes.Sensor2Service).getCharacteristic(GattAttributes.Sensor2Char);
            GlobalGatt2.setCharacteristicNotification(sensor2Char, true);
            BluetoothGattDescriptor sensor2Descr = sensor2Char.getDescriptor(GattAttributes.SensorsDescr);
            sensor2Descr.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            GlobalGatt2.writeDescriptor(sensor2Descr);
        }
    }

    public void enableSensor2Service2Tracking() throws InterruptedException, IOException {
        String stringSensor2w = "02000001820001010e00"; // String to enable communication, can vary
        byte[] newSensor2w = Utils.hexStringToByteArray(stringSensor2w);

        if (GlobalGatt2 != null){
            BluetoothGattCharacteristic controlChar = GlobalGatt2.getService(GattAttributes.Sensor2Service2).getCharacteristic(GattAttributes.Sensor2CharControl);
            BluetoothGattCharacteristic dataChar = GlobalGatt2.getService(GattAttributes.Sensor2Service2).getCharacteristic(GattAttributes.Sensor2CharData);
            BluetoothGattDescriptor descriptorData = dataChar.getDescriptor(GattAttributes.SensorsDescr);
            BluetoothGattDescriptor descriptorControl = controlChar.getDescriptor(GattAttributes.SensorsDescr);

            boolean isDone = false;

            while(!isDone){
                isDone = GlobalGatt2.requestMtu(232);  // Length of Maximum Transmission Unit can vary depending on sensor protocol
            }
            if(isDone){
                Log.d("enableS2S2Tracking", "Setting required MTU");
                isDone = false;
            }
            Thread.sleep(1000);

            // Enable control indication
            while(!isDone){
                isDone = GlobalGatt2.setCharacteristicNotification(controlChar, true);
            }
            if(isDone){
                Log.d("enableS2S2Tracking", "CONTROL: setCharacteristicNotification");
                isDone = false;
            }
            while(!isDone){
                isDone = descriptorControl.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            }
            if(isDone){
                Log.d("enableS2S2Tracking", "CONTROL: setValue");
                isDone = false;
            }
            while(!isDone){
                isDone = GlobalGatt2.writeDescriptor(descriptorControl);
            }
            if(isDone){
                Log.d("enableS2S2Tracking", "CONTROL: writeDescriptor");
                isDone = false;
            }

            // Enable data notification
            while(!isDone){
                isDone = GlobalGatt2.setCharacteristicNotification(dataChar, true);
            }
            if(isDone){
                Log.d("enableS2S2Tracking", "DATA: setCharacteristicNotification");
                isDone = false;
            }
            while(!isDone){
                isDone = descriptorData.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }
            if(isDone){
                Log.d("enableS2S2Tracking", "DATA: setValue");
                isDone = false;
            }
            while(!isDone){
                isDone = GlobalGatt2.writeDescriptor(descriptorData);
            }
            if(isDone){
                Log.d("enableS2S2Tracking", "DATA: writeDescriptor");
                isDone = false;
            }

            // Host request Sensor2 stream
            while(!isDone){
                controlChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                isDone = controlChar.setValue(newSensor2w);
            }
            if(isDone){
                Log.d("enableS2S2Tracking", "Sensor 2 STREAM: setWriteType, setValue");
                isDone = false;
            }
            while(!isDone){
                isDone = GlobalGatt2.writeCharacteristic(controlChar);
            }
            if(isDone){
                Log.d("enableS2S2Tracking", "Sensor 2 STREAM: writeCharacteristic");
                isDone = false;
            }
        }


    }

    public void enableSensor3Tracking() throws InterruptedException, IOException {
         String stringSensor3w = "0203060102000000000000000000000000000000"; // In this example, needed 20 bytes

        byte[] newIMUw = Utils.hexStringToByteArray(stringSensor3w);

        if (GlobalGatt3 != null) {
            BluetoothGattCharacteristic controlChar = GlobalGatt3.getService(GattAttributes.Sensor3Service).getCharacteristic(GattAttributes.Sensor3CharControl);
            BluetoothGattCharacteristic dataChar = GlobalGatt3.getService(GattAttributes.Sensor3Service).getCharacteristic(GattAttributes.Sensor3CharData);
            BluetoothGattDescriptor descriptorData = dataChar.getDescriptor(GattAttributes.SensorsDescr);
            BluetoothGattDescriptor descriptorControl = controlChar.getDescriptor(GattAttributes.SensorsDescr);

            boolean isDone = false;

            // Enable control indication
            while (!isDone) {
                isDone = GlobalGatt3.setCharacteristicNotification(controlChar, true);
            }
            if (isDone) {
                Log.d("enableS3Tracking", "CONTROL: setCharacteristicNotification");
                isDone = false;
            }
            while (!isDone) {
                isDone = descriptorControl.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }
            if (isDone) {
                Log.d("enableS3Tracking", "CONTROL: setValue");
                isDone = false;
            }
            while (!isDone) {
                isDone = GlobalGatt3.writeDescriptor(descriptorControl);
            }
            if (isDone) {
                Log.d("enableS3Tracking", "CONTROL: writeDescriptor");
                isDone = false;
            }

            // Enable data notification
            while (!isDone) {
                isDone = GlobalGatt3.setCharacteristicNotification(dataChar, true);
            }
            if (isDone) {
                Log.d("enableS3Tracking", "DATA: setCharacteristicNotification");
                isDone = false;
            }
            while (!isDone) {
                isDone = descriptorData.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }
            if (isDone) {
                Log.d("enableS3Tracking", "DATA: setValue");
                isDone = false;
            }
            while (!isDone) {
                isDone = GlobalGatt3.writeDescriptor(descriptorData);
            }
            if (isDone) {
                Log.d("enableS3Tracking", "DATA: writeDescriptor");
                isDone = false;
            }

            // Get IMU scale
            while (!isDone) {
                isDone = controlChar.setValue(Utils.hexStringToByteArray("C000000000000000000000000000000000000000"));
            }
            if (isDone) {
                Log.d("enableS3Tracking", "IMU FS: get full scale");
                isDone = false;
            }
            while (!isDone) {
                isDone = GlobalGatt3.writeCharacteristic(controlChar);
            }
            if (isDone) {
                Log.d("enableS3Tracking", "IMU STREAM: writeCharacteristic");
                isDone = false;
            }

            // Host request IMU stream
            while (!isDone) {
                //controlChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                isDone = controlChar.setValue(newIMUw);
            }
            if (isDone) {
                Log.d("enableS3Tracking", "IMU STREAM: start stream");
                isDone = false;
            }
            while (!isDone) {
                isDone = GlobalGatt3.writeCharacteristic(controlChar);
            }
            if (isDone) {
                Log.d("enableS3Tracking", "IMU STREAM: writeCharacteristic");
                isDone = false;
            }
        }
    }

    protected void onStart() {

        super.onStart();
    }

    protected void onRestart() {

        super.onRestart();
    }

    protected void onResume() {

        super.onResume();
    }

    protected void onPause() {

        super.onPause();
    }

    protected void onStop() {

        super.onStop();
    }

    protected void onDestroy() {

        super.onDestroy();
    }
}