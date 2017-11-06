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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

public class MainActivity
        extends AppCompatActivity {

    private TextView mTVShowTemp;
    private TextView mTVShowHumid;
    private TextView mTVShowMAXT;
    private TextView mTVShowMINT;
    private TextView mTVShowMAXH;
    private TextView mTVShowMINH;
    private TextView mTVShowSelectedDevice;

    private ListView mLVPairedDevices;

    private ImageButton mIBBluetoothOnOff;
    private ImageButton mIBConnectDisconnect;
    private ImageButton mIBRefresh;

    private static final UUID mUUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // UUID.
    private static final String mTAG = "Message from app: "; // LOG.
    private static final int mRECEIVE_MESSAGE = 1; // Receive message.

    // Symbols for obtaining data from Arduino:
    private static final String mTEMP_SYMBOL = "t"; // Temperature symbol.
    private static final String mHUMID_SYMBOL = "h"; // Humidity symbol.
    private static final String mMAXT_SYMBOL = "m"; // The value of the maximum temperature.
    private static final String mMINT_SYMBOL = "i"; // The value of the minimum temperature.
    private static final String mMAXH_SYMBOL = "w"; // The value of the maximum humidity.
    private static final String mMINH_SYMBOL = "q"; // The value of the minimum humidity.

    // Symbol for sending the Arduino reboot command:
    private static final String mREBOOT_SYMBOL = "r";

    // Bluetooth adapter and Bluetooth socket:
    private static BluetoothAdapter mBtAdapter;
    private static BluetoothSocket mBtSocket;

    // Variable for checking connection status:
    private boolean mBtConnectionState = false;

    // Handler and OutputStream:
    private Handler mHandlerMessage;
    private OutputStream mOutStream;

    private ArrayList<String> mDevices = new ArrayList<>(); // Save the list of paired devices.
    private StringBuilder mStringBuilder = new StringBuilder(); // Working with Arduino data.
    private String mMACAddress; // Variable to store the MAC address of the selected device.
    private String mSBPrint; // Save the received message.

    // Control connection and I/O (Class):
    private class ConnectedThread
            extends Thread {

        private BluetoothSocket mBtSocketCT;
        private InputStream mInStream;
        private OutputStream mOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mBtSocketCT = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                // Receive the data and send it to the Handler:
                try {
                    bytes = mInStream.read(buffer);

                    mHandlerMessage.obtainMessage(mRECEIVE_MESSAGE, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        void write(String msg) {
            byte[] msgBuffer = msg.getBytes();

            // Write:
            try {
                mOutStream.write(msgBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void cancel() {

            // Cancel connection:
            try {
                mBtSocketCT.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Control connection and I/O:
    private ConnectedThread mConnectedThread;

    // Logging:
    private void mLOG(String msg) {
        Log.e(mTAG, msg);
    }

    // Get the paired devices list:
    private void mGetPairedDevicesList() {
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            final ArrayList<String> pairedDeviceArrayList = new ArrayList<>();

            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceArrayList.add(device.getName() + '\n' + device.getAddress());
                mDevices.add(device.getAddress());
            }

            final ArrayAdapter<String> pairedDeviceAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, pairedDeviceArrayList);
            mLVPairedDevices.setAdapter(pairedDeviceAdapter);
        }
    }

    // Handler to work with the received data:
    @SuppressLint("HandlerLeak")
    private void mHandlerMessageView() {
        mHandlerMessage = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case mRECEIVE_MESSAGE:
                        byte[] readBuffer = (byte[]) msg.obj;

                        String strInCOM = new String(readBuffer, 0, msg.arg1);

                        mStringBuilder.append(strInCOM);

                        int endOfLineIndex = mStringBuilder.indexOf("\r\n");

                        if (endOfLineIndex > 0) {
                            mSBPrint = mStringBuilder.substring(0, endOfLineIndex);
                            mStringBuilder.delete(0, mStringBuilder.length());

                            switch (mSBPrint.substring(0, 1)) {
                                case mTEMP_SYMBOL:
                                    mTVShowTemp.setText(mSBPrint.substring(1, endOfLineIndex));

                                    break;
                                case mHUMID_SYMBOL:
                                    mTVShowHumid.setText(mSBPrint.substring(1, endOfLineIndex));

                                    break;
                                case mMAXT_SYMBOL:
                                    mTVShowMAXT.setText(mSBPrint.substring(1, endOfLineIndex));

                                    break;
                                case mMINT_SYMBOL:
                                    mTVShowMINT.setText(mSBPrint.substring(1, endOfLineIndex));

                                    break;
                                case mMAXH_SYMBOL:
                                    mTVShowMAXH.setText(mSBPrint.substring(1, endOfLineIndex));

                                    break;
                                case mMINH_SYMBOL:
                                    mTVShowMINH.setText(mSBPrint.substring(1, endOfLineIndex));

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
    private final BroadcastReceiver mReceiverBluetoothWaiting = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // If Bluetooth is ON:
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                        == BluetoothAdapter.STATE_ON) {
                    mIBBluetoothOnOff.setImageResource(R.drawable.bluetooth_off);

                    mGetPairedDevicesList(); // Show the list of paired devices.
                } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                        == BluetoothAdapter.STATE_OFF) {
                    mIBBluetoothOnOff.setImageResource(R.drawable.bluetooth_on);
                }
            }
        }
    };

    // Connection method:
    private void mConnect() {
        BluetoothDevice BtDevice = mBtAdapter.getRemoteDevice(mMACAddress);

        try {
            mBtSocket = BtDevice.createRfcommSocketToServiceRecord(mUUID);
        } catch (IOException e) {
            mLOG(e.getMessage());
        }

        try {
            mBtSocket.connect();
        } catch (IOException e) {
            try {
                mBtSocket.close();
            } catch (IOException e2) {
                mLOG(e2.getMessage());
            }
        }

        mConnectedThread = new ConnectedThread(mBtSocket);
        mConnectedThread.start();

        mBtConnectionState = true;

        mIBConnectDisconnect.setImageResource(R.drawable.bluetooth_connected);
    }

    // Disconnection method:
    private void mDisconnect() {
        mConnectedThread.cancel();

        mBtConnectionState = false;

        mIBConnectDisconnect.setImageResource(R.drawable.bluetooth_connect);
    }

    // A simple toast for viewing warning messages:
    private void mToast(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void mSetListViewItemSelect() {
        mLVPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mMACAddress = mDevices.get(i); // Get the device MAC address to variable.
                mTVShowSelectedDevice.setText(mMACAddress); // Set the selected MAC address to mTVShowSelectedDevice.
            }
        });

        //mLVPairedDevices.setOnItemClickListener(this);
    }

    private void mSetImageButtons() {
        ImageButton.OnClickListener onClickImageButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.ib_bluetooth_on_off:
                        if(mBtAdapter.isEnabled()) {
                            mBtAdapter.disable(); // Disable.
                            mIBBluetoothOnOff.setImageResource(R.drawable.bluetooth_on);
                        } else {
                            mBtAdapter.enable(); // Enable:
                            mIBBluetoothOnOff.setImageResource(R.drawable.bluetooth_off);
                        }

                        break;
                    case R.id.ib_connect_disconnect:
                        if(mBtAdapter.isEnabled()) {
                            if(!mBtConnectionState) mConnect(); // Connect.
                            else mDisconnect(); // Disconnect.
                        } else mToast("Turn on Bluetooth!");

                        break;
                    case R.id.ib_refresh:
                        if(mBtAdapter.isEnabled()) mConnectedThread.write(mREBOOT_SYMBOL); // Write.
                        else mToast(getString(R.string.enable_bluetooth)); // Show a warning toast.

                        break;
                }
            }
        };

        mIBBluetoothOnOff.setOnClickListener(onClickImageButton);
        mIBConnectDisconnect.setOnClickListener(onClickImageButton);
        mIBRefresh.setOnClickListener(onClickImageButton);
    }

    private void mInitialization() {
        mTVShowTemp = findViewById(R.id.tv_show_temp);
        mTVShowHumid = findViewById(R.id.tv_show_humid);
        mTVShowMAXT = findViewById(R.id.tv_show_MAX_T);
        mTVShowMINT = findViewById(R.id.tv_show_MIN_T);
        mTVShowMAXH = findViewById(R.id.tv_show_MAX_H);
        mTVShowMINH = findViewById(R.id.tv_show_MIN_H);
        mTVShowSelectedDevice = findViewById(R.id.tv_selected_device);

        mLVPairedDevices = findViewById(R.id.lv_paired_devices);

        mIBBluetoothOnOff = findViewById(R.id.ib_bluetooth_on_off);
        mIBConnectDisconnect = findViewById(R.id.ib_connect_disconnect);
        mIBRefresh = findViewById(R.id.ib_refresh);

        // Register a Receiver:
        registerReceiver(mReceiverBluetoothWaiting, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        // Get the default Bluetooth adapter:
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check the Bluetooth status:
        if(!mBtAdapter.isEnabled()) mIBBluetoothOnOff.setImageResource(R.drawable.bluetooth_on);
        else {
            mIBBluetoothOnOff.setImageResource(R.drawable.bluetooth_off);
            mGetPairedDevicesList();
        }

        // Waiting for the adoption of data:
        mHandlerMessageView();

        mSetListViewItemSelect();
        mSetImageButtons();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Only portrait.

        mInitialization();
    }

    // Save the connection when the application is hidden:
    @Override
    public void onPause() {
        super.onPause();

        if(mOutStream != null) {
            try {
                mOutStream.flush();
            } catch (IOException e) {
                mLOG(e.getMessage());
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(mOutStream != null) {
            try {
                mOutStream.flush();
            } catch (IOException e) {
                mLOG(e.getMessage());
            }
        }
    }
}
