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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private class BluetoothConnection extends Thread {

        private BluetoothSocket bluetoothSocket;
        private InputStream inputStream;
        private OutputStream outputStream;

        void initializeStreams(@NonNull BluetoothSocket socket) {
            bluetoothSocket = socket;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void read(byte[] buffer) throws IOException {
            int bytes = inputStream.read(buffer);
            systemHandler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();
        }

        void write(@NonNull String data) throws IOException {
            byte[] messageBuffer = data.getBytes();
            outputStream.write(messageBuffer);
        }

        public void run() {
            byte[] buffer = new byte[256];
            while (true) {
                try {
                    read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        void disconnect() throws IOException {
            bluetoothSocket.close();
        }

        BluetoothConnection(@NonNull BluetoothSocket bluetoothSocket) {
            initializeStreams(bluetoothSocket);
        }

    }

    @BindView(R.id.temperature) protected TextView temperature;
    @BindView(R.id.humidity) protected TextView humidity;
    @BindView(R.id.max_temperature) protected TextView maxTemperature;
    @BindView(R.id.min_temperature) protected TextView minTemperature;
    @BindView(R.id.max_humidity) protected TextView maxHumidity;
    @BindView(R.id.min_humidity) protected TextView minHumidity;
    @BindView(R.id.paired_device) protected TextView selectedDevice;

    @BindView(R.id.paired_devices_list) protected ListView pairedDevicesList;

    @BindView(R.id.on_off) protected ImageButton onOff;
    @BindView(R.id.connect_disconnect) protected ImageButton connectDisconnect;

    private final UUID BLUETOOTH_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final int RECEIVE_MESSAGE = 1;

    private BluetoothConnection bluetoothConnection;

    private final String TEMP_SYMBOL = "t";
    private final String HUMID_SYMBOL = "h";
    private final String MAXT_SYMBOL = "m";
    private final String MINT_SYMBOL = "i";
    private final String MAXH_SYMBOL = "w";
    private final String MINH_SYMBOL = "q";
    private final String REBOOT_SYMBOL = "r";

    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothSocket bluetoothSocket;

    private boolean bluetoothConnectionState = false;

    private Handler systemHandler;
    private ArrayList<String> devicesList = new ArrayList<>();
    private StringBuilder messageData = new StringBuilder();
    private String macAddress;
    private String toOutputDataBuilder;

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
            this.pairedDevicesList.setAdapter(pairedDeviceAdapter);
        }
    }

    @SuppressLint("HandlerLeak")
    private void setupHandler() {
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
                                    temperature.setText(toOutputDataBuilder.substring(1, endOfLineIndex));
                                    break;
                                case HUMID_SYMBOL:
                                    humidity.setText(toOutputDataBuilder.substring(1, endOfLineIndex));
                                    break;
                                case MAXT_SYMBOL:
                                    maxTemperature.setText(toOutputDataBuilder.substring(1, endOfLineIndex));
                                    break;
                                case MINT_SYMBOL:
                                    minTemperature.setText(toOutputDataBuilder.substring(1, endOfLineIndex));
                                    break;
                                case MAXH_SYMBOL:
                                    maxHumidity.setText(toOutputDataBuilder.substring(1, endOfLineIndex));
                                    break;
                                case MINH_SYMBOL:
                                    minHumidity.setText(toOutputDataBuilder.substring(1, endOfLineIndex));
                                    break;
                            }
                        }
                }
            }
        };
    }

    private final BroadcastReceiver bluetoothWaitingReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                    case BluetoothAdapter.STATE_ON:
                        onOff.setImageResource(R.drawable.ic_bluetooth_off);
                        initializePairedDevices();
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        onOff.setImageResource(R.drawable.ic_bluetooth_on);
                        break;
                }
            }
        }
    };

    private void connect() {
        BluetoothDevice BtDevice = bluetoothAdapter.getRemoteDevice(macAddress);
        try {
            bluetoothSocket = BtDevice.createRfcommSocketToServiceRecord(BLUETOOTH_UUID);
            bluetoothSocket.connect();
            bluetoothConnection = new BluetoothConnection(bluetoothSocket);
            bluetoothConnection.start();
            bluetoothConnectionState = true;
            connectDisconnect.setImageResource(R.drawable.ic_bluetooth_connected);
        } catch (IOException e) {
            try {
                bluetoothSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void disconnect() {
        try {
            bluetoothConnection.disconnect();
            bluetoothConnectionState = false;
            connectDisconnect.setImageResource(R.drawable.ic_bluetooth_connect);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showToast(@NonNull String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.on_off)
    public void onOffClick() {
        if(bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            onOff.setImageResource(R.drawable.ic_bluetooth_on);
        } else {
            bluetoothAdapter.enable();
            onOff.setImageResource(R.drawable.ic_bluetooth_off);
        }
    }

    @OnClick(R.id.connect_disconnect)
    public void connectDisconnectClick() {
        if(bluetoothAdapter.isEnabled()) {
            if(!bluetoothConnectionState) connect();
            else disconnect();
        } else {
            showToast(getString(R.string.common_enable_bluetooth));
        }
    }

    @OnClick(R.id.refresh)
    public void refreshClick() {
        if(bluetoothAdapter.isEnabled())
            try {
                bluetoothConnection.write(REBOOT_SYMBOL);
            } catch (IOException e) {
                e.printStackTrace();
            }
        else {
            showToast(getString(R.string.common_enable_bluetooth));
        }
    }

    public void initialize() {
        pairedDevicesList.setOnItemClickListener((adapterView, view, i, l) -> {
            macAddress = devicesList.get(i);
            selectedDevice.setText(macAddress);
        });

        registerReceiver(bluetoothWaitingReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            onOff.setImageResource(R.drawable.ic_bluetooth_on);
        } else {
            onOff.setImageResource(R.drawable.ic_bluetooth_off);
            initializePairedDevices();
        }

        setupHandler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initialize();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bluetoothConnection != null) {
            OutputStream bluetoothStream = bluetoothConnection.outputStream;
            if (bluetoothStream != null) {
                try {
                    bluetoothStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
