package com.fomchenkovoutlook.artem.android_remote_monitor_example;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Control connection and I/O (Class):
    private class ConnectionThread extends Thread {

        private BluetoothSocket bluetoothSocket;
        private InputStream inputStream;
        private OutputStream outputStream;

        void initializeStreams(@NonNull BluetoothSocket socket) {
            bluetoothSocket = socket;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException initializeStreamsException) {
                initializeStreamsException.printStackTrace();
            }
        }

        // Read:
        void read(byte[] buffer) throws IOException {
            int bytes = inputStream.read(buffer);
            systemHandler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();
        }

        // Write:
        public void write(@NonNull String data) throws IOException {
            byte[] messageBuffer = data.getBytes();
            outputStream.write(messageBuffer);
        }

        public void run() {
            byte[] buffer = new byte[256];
            while (true) {
                // Receive the data and send it to the Handler:
                try {
                    read(buffer);
                } catch (IOException readFromBufferException) {
                    readFromBufferException.printStackTrace();
                }
            }
        }

        // Disconnect:
        void disconnect() throws IOException {
            bluetoothSocket.close();
        }

        ConnectionThread(@NonNull BluetoothSocket bluetoothSocket) {
            initializeStreams(bluetoothSocket);
        }
    }

    private TextView tvShowTemp;
    private TextView tvShowHumid;
    private TextView tvShowMAXT;
    private TextView tvShowMINT;
    private TextView tvShowMAXH;
    private TextView tvShowMINH;
    private TextView tvShowSelectedDevice;
    private ListView vlPairedDevices;
    private ImageButton ibBluetoothOnOff;
    private ImageButton ibConnectDisconnect;

    private final UUID BLUETOOTH_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final int RECEIVE_MESSAGE = 1;

    // Control connection and I/O:
    private ConnectionThread connectionThread;

    // Symbols for obtaining data from Arduino:
    private final String TEMP_SYMBOL = "t"; // Temperature symbol.
    private final String HUMID_SYMBOL = "h"; // Humidity symbol.
    private final String MAXT_SYMBOL = "m"; // Maximum temperature.
    private final String MINT_SYMBOL = "i"; // Minimum temperature.
    private final String MAXH_SYMBOL = "w"; // Maximum humidity.
    private final String MINH_SYMBOL = "q"; // Minimum humidity.
    private final String REBOOT_SYMBOL = "r"; // Reboot system.

    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothSocket bluetoothSocket;

    // Variable for checking connection status:
    private boolean bluetoothConnectionState = false;

    private Handler systemHandler;
    private ArrayList<String> devicesList = new ArrayList<>(); // Save the list of paired devices.
    private StringBuilder messageData = new StringBuilder(); // Working with Arduino data.
    private String macAddress; // Variable to store the MAC address of the selected device.
    private String toOutputDataBuilder; // Save the received message.

    // Get the paired devices list:
    private void initializePairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            final ArrayList<String> pairedDeviceArrayList = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceArrayList.add(device.getName() + '\n' + device.getAddress());
                devicesList.add(device.getAddress());
            }
            final ArrayAdapter<String> pairedDeviceAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, pairedDeviceArrayList);
            vlPairedDevices.setAdapter(pairedDeviceAdapter);
        }
    }

    // Handler to work with the received data:
    @SuppressLint("HandlerLeak")
    private void mHandlerMessageView() {
        systemHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case RECEIVE_MESSAGE:
                        byte[] readBuffer = (byte[]) msg.obj;
                        String strInCOM = new String(readBuffer, 0, msg.arg1);
                        messageData.append(strInCOM);
                        int endOfLineIndex = messageData.indexOf("\r\n");
                        if (endOfLineIndex > 0) {
                            toOutputDataBuilder = messageData.substring(0, endOfLineIndex);
                            messageData.delete(0, messageData.length());
                            switch (toOutputDataBuilder.substring(0, 1)) {
                                case TEMP_SYMBOL:
                                    tvShowTemp.setText(toOutputDataBuilder.substring(1, endOfLineIndex));
                                    break;
                                case HUMID_SYMBOL:
                                    tvShowHumid.setText(toOutputDataBuilder.substring(1, endOfLineIndex));
                                    break;
                                case MAXT_SYMBOL:
                                    tvShowMAXT.setText(toOutputDataBuilder.substring(1, endOfLineIndex));
                                    break;
                                case MINT_SYMBOL:
                                    tvShowMINT.setText(toOutputDataBuilder.substring(1, endOfLineIndex));
                                    break;
                                case MAXH_SYMBOL:
                                    tvShowMAXH.setText(toOutputDataBuilder.substring(1, endOfLineIndex));
                                    break;
                                case MINH_SYMBOL:
                                    tvShowMINH.setText(toOutputDataBuilder.substring(1, endOfLineIndex));
                                    break;
                            }
                        }
                }
            }
        };
    }

    /*
     * Changes the state of the application elements when changing Bluetooth state using the operating
     * system or other applications.
     */
    private final BroadcastReceiver bluetoothWaitingReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                    case BluetoothAdapter.STATE_ON:
                        ibBluetoothOnOff.setImageResource(R.drawable.ic_bluetooth_off);
                        initializePairedDevices(); // Show the list of paired devices.
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        ibBluetoothOnOff.setImageResource(R.drawable.ic_bluetooth_on);
                        break;
                }
            }
        }
    };

    // Connection method:
    private void connect() {
        BluetoothDevice BtDevice = bluetoothAdapter.getRemoteDevice(macAddress);
        try {
            bluetoothSocket = BtDevice.createRfcommSocketToServiceRecord(BLUETOOTH_UUID);
            bluetoothSocket.connect();
            connectionThread = new ConnectionThread(bluetoothSocket);
            connectionThread.start();
            bluetoothConnectionState = true;
            ibConnectDisconnect.setImageResource(R.drawable.ic_bluetooth_connected);
        } catch (IOException bluetoothConnectionException) {
            try {
                bluetoothSocket.close();
            } catch (IOException bluetoothConnectException) {
                bluetoothConnectException.printStackTrace();
            }
        }
    }

    // Disconnection method:
    private void disconnect() {
        try {
            connectionThread.disconnect();
            bluetoothConnectionState = false;
            ibConnectDisconnect.setImageResource(R.drawable.ic_bluetooth_connect);
        } catch (IOException bluetoothDisconnectException) {
            bluetoothDisconnectException.printStackTrace();
        }
    }

    // A simple toast for viewing warning messages:
    private void showToast(@NonNull String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void initialization() {
        tvShowTemp = findViewById(R.id.tv_show_temp);
        tvShowHumid = findViewById(R.id.tv_show_humid);
        tvShowMAXT = findViewById(R.id.tv_show_max_t);
        tvShowMINT = findViewById(R.id.tv_show_min_t);
        tvShowMAXH = findViewById(R.id.tv_show_max_h);
        tvShowMINH = findViewById(R.id.tv_show_min_h);
        tvShowSelectedDevice = findViewById(R.id.tv_selected_device);
        vlPairedDevices = findViewById(R.id.lv_paired_devices);
        ibBluetoothOnOff = findViewById(R.id.ib_bluetooth_on_off);
        ibConnectDisconnect = findViewById(R.id.ib_connect_disconnect);

        ibBluetoothOnOff.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable(); // Disable.
                    ibBluetoothOnOff.setImageResource(R.drawable.ic_bluetooth_on);
                } else {
                    bluetoothAdapter.enable(); // Enable:
                    ibBluetoothOnOff.setImageResource(R.drawable.ic_bluetooth_off);
                }
            }
        });
        ibConnectDisconnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(bluetoothAdapter.isEnabled()) {
                    if(!bluetoothConnectionState) connect(); // Connect.
                    else disconnect(); // Disconnect.
                } else {
                    showToast("Turn on Bluetooth!");
                }
            }
        });
        findViewById(R.id.ib_refresh).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(bluetoothAdapter.isEnabled())
                    try {
                        connectionThread.write(REBOOT_SYMBOL);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                else {
                    showToast(getString(R.string.enable_bluetooth));
                }
            }
        });

        vlPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                macAddress = devicesList.get(i);
                tvShowSelectedDevice.setText(macAddress); // Set the selected MAC address to tvShowSelectedDevice.
            }
        });

        registerReceiver(bluetoothWaitingReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()) {
            ibBluetoothOnOff.setImageResource(R.drawable.ic_bluetooth_on);
        } else {
            ibBluetoothOnOff.setImageResource(R.drawable.ic_bluetooth_off);
            initializePairedDevices();
        }

        // Waiting for the adoption of data:
        mHandlerMessageView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Only portrait.
        initialization();
    }

    // Save the connection when the application is hidden:
    @Override
    public void onPause() {
        super.onPause();
        // Flush stream:
        if (connectionThread != null) {
            OutputStream bluetoothStream = connectionThread.outputStream;
            if (bluetoothStream != null) {
                try {
                    bluetoothStream.flush();
                } catch (IOException streamFlushException) {
                    streamFlushException.printStackTrace();
                }
            }
        }
    }
}
