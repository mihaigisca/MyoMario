package com.local.miomario.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.local.miomario.view.FullscreenActivity;
import com.local.miomario.R;
import com.local.miomario.MioMarioApp;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class BluetoothService extends Service {
    public static final String BT_SERVICE_ID = "BT_SERVICE";
    public static final int BT_SERVICE_START = 1;
    public static final int BT_SERVICE_STOP = 2;
    public static final int BT_SERVICE_PAUSE = 3;
    private static final int REQUEST_ENABLE_BT_FOREGROUND_SERVICE = 40;
    private static final int FOREGROUND_SERVICE_BT_ID = 41;
    private static final String MYO_ARMBAND_NAME = "My Myo";

    private BluetoothAdapter mBtAdapter;
    private BluetoothDevice mBtTargetDevice;
    private BluetoothSocket mBtSocket;

    private BroadcastReceiver mBtBroadcastReceiver;
    private Handler mBtHandler;
    private Runnable mBtRunnable;

    private enum BluetoothServiceState {
        SERVICE_STARTED,
        SERVICE_PAUSED,
        SERVICE_STOPPED
    }
    private static BluetoothServiceState mBtServiceState = BluetoothServiceState.SERVICE_STOPPED;
    private static final String MYO_INFO_SERVICE_UUID = "d5060001-a904-deb9-4748-2c7f4a124842";
    private static final String MYO_INFO_CHAR_UUID = "d5060101-a904-deb9-4748-2c7f4a124842";
    private static final String MYO_FW_VERSION_CHAR_UUID = "d5060201-a904-deb9-4748-2c7f4a124842";
    private static final String MYO_CMD_CHAR_UUID = "d5060401-a904-deb9-4748-2c7f4a124842";

    private static final String MYO_EMG_DATA_SERVICE_UUID = "d5060005-a904-deb9-4748-2c7f4a124842";
    private static final String MYO_EMG_DATA_0_CHAR_UUID = "d5060105-a904-deb9-4748-2c7f4a124842";
    private static final String MYO_EMG_DATA_1_CHAR_UUID = "d5060205-a904-deb9-4748-2c7f4a124842";
    private static final String MYO_EMG_DATA_2_CHAR_UUID = "d5060305-a904-deb9-4748-2c7f4a124842";
    private static final String MYO_EMG_DATA_3_CHAR_UUID = "d5060405-a904-deb9-4748-2c7f4a124842";

    private static final String BATTERY_SERVICE_UUID = "d506180f-a904-deb9-4748-2c7f4a124842";
    private static final String BATTERY_LEVEL_CHAR_UUID = "d5062a19-a904-deb9-4748-2c7f4a124842";
    private static final String DEVICE_NAME_CHAR_UUID = "d5062a00-a904-deb9-4748-2c7f4a124842";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int intentCommand = intent.getIntExtra(BT_SERVICE_ID, BT_SERVICE_START);

        switch (intentCommand) {
            case BT_SERVICE_START: {
                switch (mBtServiceState) {
                    case SERVICE_STOPPED: {
                        mBtServiceState = BluetoothServiceState.SERVICE_STARTED;

                        Intent notificationIntent = new Intent(this,
                                FullscreenActivity.class);
                        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                                REQUEST_ENABLE_BT_FOREGROUND_SERVICE, notificationIntent,
                                PendingIntent.FLAG_IMMUTABLE |
                                        PendingIntent.FLAG_UPDATE_CURRENT);

                        Notification notification = new NotificationCompat.Builder(this,
                                MioMarioApp.BT_SERVICE_CHANNEL_ID)
                                .setContentTitle(MioMarioApp.BT_SERVICE_CHANNEL_NAME)
                                .setSmallIcon(R.mipmap.ic_myo)
                                .setContentIntent(pendingIntent)
                                .build();

                        startForeground(FOREGROUND_SERVICE_BT_ID, notification);

                        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

                        btRegisterReceiver();

                        tryForDevice();
                    }
                    break;

                    case SERVICE_PAUSED: {
                        mBtServiceState = BluetoothServiceState.SERVICE_STARTED;

                        if (null != mBtHandler) {
                            mBtHandler.removeCallbacksAndMessages(mBtRunnable);
                            mBtHandler = null;
                        }
                        mBtHandler = new Handler(Looper.getMainLooper());
                        mBtHandler.postDelayed(mBtRunnable, 1000);
                    }
                    break;

                    default:
                    break;
                }
            }
            break;

            case BT_SERVICE_STOP: {
                if (BluetoothServiceState.SERVICE_STOPPED != mBtServiceState) {
                    mBtServiceState = BluetoothServiceState.SERVICE_STOPPED;

                    stopForeground(true);
                    stopSelf();
                }
            }
            break;

            case BT_SERVICE_PAUSE: {
                if (BluetoothServiceState.SERVICE_STARTED == mBtServiceState) {
                    mBtServiceState = BluetoothServiceState.SERVICE_PAUSED;

                    if (null != mBtHandler) {
                        mBtHandler.removeCallbacksAndMessages(mBtRunnable);
                        mBtHandler = null;
                    }
                }
            }
            break;
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BluetoothServiceState.SERVICE_STOPPED == mBtServiceState) {
            mBtHandler = new Handler(Looper.getMainLooper());

            initRunnable();

            initBroadcastReceiver();
        }
    }

    private void initBroadcastReceiver() {
        mBtBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice btDevice = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE);

                    if (null != btDevice) {
                        Toast.makeText(getApplicationContext(), "Device found: " +
                                btDevice.getName(), Toast.LENGTH_SHORT).show();

                        if (btDevice.getName().equals(MYO_ARMBAND_NAME)) {
                            if (BluetoothDevice.BOND_BONDED == btDevice.getBondState()) {
                                connectToDevice(btDevice);
                            } else if (BluetoothDevice.BOND_NONE == btDevice.getBondState()) {
                                btDevice.createBond();
                            }
                        }
                    }
                } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    BluetoothDevice btDevice = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE);

                    if (null != btDevice) {
                        Toast.makeText(getApplicationContext(), "Device connected: " +
                                btDevice.getName(), Toast.LENGTH_SHORT).show();

                        if (btDevice.getName().equals(MYO_ARMBAND_NAME)) {
                            mBtAdapter.cancelDiscovery();
                            mBtHandler.post(mBtRunnable);
                        }
                    }
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    BluetoothDevice btDevice = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE);

                    if (null != btDevice) {
                        Toast.makeText(getApplicationContext(), "Device disconnected: " +
                                btDevice.getName(), Toast.LENGTH_SHORT).show();

                        if (btDevice.getName().equals(MYO_ARMBAND_NAME)) {
                            mBtAdapter.startDiscovery();
                        }
                    }
                } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    BluetoothDevice btDevice = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE);

                    if (null != btDevice) {
                        Toast.makeText(getApplicationContext(), "Device bond change: " +
                                btDevice.getName(), Toast.LENGTH_SHORT).show();

                        if (btDevice.getName().equals(MYO_ARMBAND_NAME)) {
                            if (BluetoothDevice.BOND_BONDED == btDevice.getBondState()) {
                                connectToDevice(btDevice);
                            } else if (BluetoothDevice.BOND_BONDING == btDevice.getBondState()) {
                                Toast.makeText(getApplicationContext(), "Bonding with: " +
                                        btDevice.getName(), Toast.LENGTH_SHORT).show();
                            } else {
                                mBtAdapter.startDiscovery();
                            }
                        }
                    }
                } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                    BluetoothDevice btDevice = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE);

                    if (null != btDevice) {
                        Toast.makeText(getApplicationContext(), "Device pairing request: " +
                                btDevice.getName(), Toast.LENGTH_SHORT).show();

                        if (btDevice.getName().equals(MYO_ARMBAND_NAME)) {
                            btDevice.createBond();
                        }
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    tryForDevice();
                }
            }
        };
    }

    private void initRunnable() {
        mBtRunnable = new Runnable() {
            @Override
            public void run() {
                readDeviceInfoService();

                subscribeToEmgService();

                getUuids(mBtTargetDevice);

                mBtHandler.postDelayed(mBtRunnable, 1000);
            }
        };
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        releaseResources();
    }

    private void releaseResources()
    {
        if (null != mBtBroadcastReceiver) {
            unregisterReceiver(mBtBroadcastReceiver);
            mBtBroadcastReceiver = null;
        }

        if (null != mBtHandler) {
            mBtHandler.removeCallbacksAndMessages(null);
            mBtHandler = null;
        }

        if (null != mBtSocket) {
            if (mBtSocket.isConnected()) {
                try {
                    mBtSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mBtSocket = null;
        }
    }

    private void btRegisterReceiver() {
        IntentFilter btActionFilters = new IntentFilter();

        btActionFilters.addAction(BluetoothDevice.ACTION_FOUND);
        btActionFilters.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        btActionFilters.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        btActionFilters.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        btActionFilters.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        btActionFilters.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mBtBroadcastReceiver, btActionFilters);
    }

    private void tryForDevice() {
        if (isTargetDevicePaired()) {
            connectToDevice(mBtTargetDevice);
        } else {
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }

            mBtAdapter.startDiscovery();
        }
    }

    private boolean isTargetDevicePaired() {
        boolean isTargetPaired = false;

        Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
        Toast.makeText(getApplicationContext(), "Device count: " + bondedDevices.size(),
                Toast.LENGTH_SHORT).show();

        if (0 < bondedDevices.size()) {
            for (BluetoothDevice bondedDevice : bondedDevices) {
                if (MYO_ARMBAND_NAME.equals(bondedDevice.getName())) {
                    isTargetPaired = true;
                    mBtTargetDevice = bondedDevice;
                    break;
                }
            }
        }

        return isTargetPaired;
    }

    private void connectToDevice(BluetoothDevice btDevice) {
        mBtAdapter.cancelDiscovery();

        try {
            mBtSocket = btDevice.createRfcommSocketToServiceRecord(
                    UUID.fromString(BATTERY_SERVICE_UUID));

            mBtSocket.connect();
        } catch (IOException e) {
            e.printStackTrace();

            try {
                mBtSocket.close();
            } catch (IOException eClose) {
                eClose.printStackTrace();
            }

            mBtAdapter.startDiscovery();
        }
    }

    private void subscribeToEmgService() {
        // TODO
    }

    private void readDeviceInfoService() {
        // TODO
    }

    private void getUuids(BluetoothDevice btDevice)
    {
        if (null != btDevice) {
            ParcelUuid[] parcelUuidList = btDevice.getUuids();

            for (ParcelUuid uuid : parcelUuidList) {
                Toast.makeText(getApplicationContext(), uuid.getUuid().toString(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}