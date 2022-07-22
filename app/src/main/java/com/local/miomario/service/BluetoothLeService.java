package com.local.miomario.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.local.miomario.MioMarioApp;
import com.local.miomario.R;
import com.local.miomario.view.FullscreenActivity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private final static int REQUEST_ENABLE_BT_FOREGROUND_SERVICE = 40;
    private final static int FOREGROUND_SERVICE_BT_ID = 41;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private boolean mScanning;
    private BroadcastReceiver mGattUpdateReceiver;
    private BroadcastReceiver mAppReceiver;
    private BluetoothGattCharacteristic mMyoCommandCharacteristic;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private int mConnectionState;

    public static final String BT_SERVICE_ID = "BT_SERVICE";
    public static final int BT_SERVICE_START = 1;
    public static final int BT_SERVICE_STOP = 2;
    public static final int BT_SERVICE_PAUSE = 3;

    public final static String ACTION_BLE_UNSUPPORTED =
            "com.local.miomario.service.BluetoothLeService.ACTION_BLE_UNSUPPORTED";
    public final static String ACTION_BLE_CONNECTED =
            "com.local.miomario.service.BluetoothLeService.ACTION_BLE_CONNECTED";
    public final static String ACTION_BLE_DISCONNECTED =
            "com.local.miomario.service.BluetoothLeService.ACTION_BLE_DISCONNECTED";
    public final static String ACTION_BLE_BATTERY_LEVEL_AVAILABLE =
            "com.local.miomario.service.BluetoothLeService.ACTION_BLE_BATTERY_LEVEL_AVAILABLE";
    public final static String ACTION_BLE_DEVICE_NAME_AVAILABLE =
            "com.local.miomario.service.BluetoothLeService.ACTION_BLE_DEVICE_NAME_AVAILABLE";
    public final static String ACTION_BLE_INFO_AVAILABLE =
            "com.local.miomario.service.BluetoothLeService.ACTION_BLE_INFO_AVAILABLE";
    public final static String ACTION_BLE_FW_AVAILABLE =
            "com.local.miomario.service.BluetoothLeService.ACTION_BLE_FW_AVAILABLE";
    public final static String ACTION_BLE_EMG0_AVAILABLE =
            "com.local.miomario.service.BluetoothLeService.ACTION_BLE_EMG0_AVAILABLE";
    public final static String ACTION_BLE_EMG1_AVAILABLE =
            "com.local.miomario.service.BluetoothLeService.ACTION_BLE_EMG1_AVAILABLE";
    public final static String ACTION_BLE_EMG2_AVAILABLE =
            "com.local.miomario.service.BluetoothLeService.ACTION_BLE_EMG2_AVAILABLE";
    public final static String ACTION_BLE_EMG3_AVAILABLE =
            "com.local.miomario.service.BluetoothLeService.ACTION_BLE_EMG3_AVAILABLE";
    public final static String ACTION_BLE_IMU_AVAILABLE =
            "com.local.miomario.service.BluetoothLeService.ACTION_BLE_IMU_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.local.miomario.service.BluetoothLeService.EXTRA_DATA";
    public final static String ACTION_READABLE_FIELDS_REQUESTED =
            "com.local.miomario.service.BluetoothLeService.ACTION_READABLE_FIELDS_REQUESTED";
    public final static String ACTION_READABLE_FIELDS_AVAILABLE =
            "com.local.miomario.service.BluetoothLeService.ACTION_READABLE_FIELDS_AVAILABLE";
    public final static String EXTRA_CONNECTION_STATE =
            "com.local.miomario.service.BluetoothLeService.EXTRA_CONNECTION_STATE";
    public final static String EXTRA_DEVICE_NAME =
            "com.local.miomario.service.BluetoothLeService.EXTRA_DEVICE_NAME";
    public final static String EXTRA_SERIAL_NUMBER =
            "com.local.miomario.service.BluetoothLeService.EXTRA_SERIAL_NUMBER";
    public final static String EXTRA_FIRMWARE_VERSION =
            "com.local.miomario.service.BluetoothLeService.EXTRA_FIRMWARE_VERSION";
    public final static String EXTRA_BATTERY_LEVEL =
            "com.local.miomario.service.BluetoothLeService.EXTRA_BATTERY_LEVEL";

    private enum BluetoothServiceState {
        SERVICE_STARTED,
        SERVICE_PAUSED,
        SERVICE_STOPPED
    }
    private static BluetoothServiceState mBtServiceState;

    private enum BtCharProperty {
        Read,
        Notify,
    }

    private static class BtChar {
        BluetoothGattCharacteristic btChar;
        BtCharProperty btCharProperty;

        public BtChar (BluetoothGattCharacteristic characteristic, BtCharProperty property) {
            btChar = characteristic;
            btCharProperty = property;
        }
    }

    private List<BtChar> btChars;

    private static final String MYO_ARMBAND_NAME = "My Myo";

    private final static String ACTION_GATT_CONNECTED =
            "com.local.miomario.service.BluetoothLeService.ACTION_GATT_CONNECTED";
    private final static String ACTION_GATT_DISCONNECTED =
            "com.local.miomario.service.BluetoothLeService.ACTION_GATT_DISCONNECTED";
    private final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.local.miomario.service.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED";
    private final static String ACTION_DATA_AVAILABLE =
            "com.local.miomario.service.BluetoothLeService.ACTION_DATA_AVAILABLE";
    private final static String ACTION_DATA_UNAVAILABLE =
            "com.local.miomario.service.BluetoothLeService.ACTION_DATA_UNAVAILABLE";
    private final static String DESCRIPTOR_WRITE_SUCCESS =
            "com.local.miomario.service.BluetoothLeService.DESCRIPTOR_WRITE_SUCCESS";
    private final static String DESCRIPTOR_WRITE_FAILURE =
            "com.local.miomario.service.BluetoothLeService.DESCRIPTOR_WRITE_FAILURE";
    private final static String CHAR_UUID =
            "com.local.miomario.service.BluetoothLeService.CHAR_UUID";
    private final static String DATA_SOURCE =
            "com.local.miomario.service.BluetoothLeService.DATA_SOURCE";
/**
 *   private static final UUID MYO_INFO_SERVICE_UUID =
 *            UUID.fromString("d5060001-a904-deb9-4748-2c7f4a124842");
 *    private static final UUID MYO_EMG_DATA_SERVICE_UUID =
 *            UUID.fromString("d5060005-a904-deb9-4748-2c7f4a124842");
 *    private static final UUID MYO_IMU_DATA_SERVICE_UUID =
 *            UUID.fromString("d5060002-a904-deb9-4748-2c7f4a124842");
 *    private static final UUID BATTERY_SERVICE_UUID =
 *            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
 *    private static final UUID GENERIC_ACCESS_SERVICE_UUID =
 *            UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
 */

    private static final UUID MYO_INFO_CHAR_UUID =
            UUID.fromString("d5060101-a904-deb9-4748-2c7f4a124842");
    private static final UUID MYO_FW_VERSION_CHAR_UUID =
            UUID.fromString("d5060201-a904-deb9-4748-2c7f4a124842");
    private static final UUID MYO_CMD_CHAR_UUID =
            UUID.fromString("d5060401-a904-deb9-4748-2c7f4a124842");
    private static final UUID MYO_EMG_DATA_0_CHAR_UUID =
            UUID.fromString("d5060105-a904-deb9-4748-2c7f4a124842");
    private static final UUID MYO_EMG_DATA_1_CHAR_UUID =
            UUID.fromString("d5060205-a904-deb9-4748-2c7f4a124842");
    private static final UUID MYO_EMG_DATA_2_CHAR_UUID =
            UUID.fromString("d5060305-a904-deb9-4748-2c7f4a124842");
    private static final UUID MYO_EMG_DATA_3_CHAR_UUID =
            UUID.fromString("d5060405-a904-deb9-4748-2c7f4a124842");
    private static final UUID MYO_IMU_DATA_CHAR_UUID =
            UUID.fromString("d5060402-a904-deb9-4748-2c7f4a124842");
    private static final UUID BATTERY_LEVEL_CHAR_UUID =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_NAME_CHAR_UUID =
            UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    private static final UUID CCC_DESCRIPTOR_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int MyoSkuUnknown = 0;
    private static final int MyoSkuBlack = 1;
    private static final int MyoSkuWhite = 2;
    private static final int MyoHwRevUnknown = 0;
    private static final int MyoHwRevC = 1;
    private static final int MyoHwRevD = 2;
    private static final byte SerialNumberSize = 6;
    private static final byte ReadOperation = 0;
    private static final byte NotifyOperation = 1;
    private static final String SKU =
            "com.local.miomario.service.BluetoothLeService.SKU";
    private static final String MAJOR_FW_VERSION =
            "com.local.miomario.service.BluetoothLeService.MAJOR_FW_VERSION";
    private static final String MINOR_FW_VERSION =
            "com.local.miomario.service.BluetoothLeService.MINOR_FW_VERSION";
    private static final String FW_PATCH =
            "com.local.miomario.service.BluetoothLeService.FW_PATCH";
    private static final byte NO_INDEX = -1;
    private static final byte FrameSize = 5;
    private static final byte PayloadSize = 3;
    private static final byte CommandSetMode = 1;
    private static final byte EmgModeSendEmg = 2;
    /** private static final byte ImuModeNone = 0; */
    private static final byte ImuModeSendData = 1;
    private static final byte ClassifierModeDisabled = 0;
    private static final byte EmgCharacteristicDataSize = 16;
    private static final byte ImuCharacteristicDataSize = 20;

    private String connectionState;
    private String serialNumber;
    private String firmwareVersion;
    private String deviceName;
    private byte batteryLevel;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "GATT services discovered.");
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "GATT data available via read.");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, ReadOperation);
            } else {
                Log.i(TAG, "GATT data not available via read.");
                broadcastUpdate(ACTION_DATA_UNAVAILABLE, characteristic, ReadOperation);
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "GATT data available via notify.");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, NotifyOperation);
        }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      final BluetoothGattDescriptor descriptor,
                                      final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "writeDescriptor succeeded for characteristic: " +
                        descriptor.getCharacteristic().getUuid().toString());
                broadcastUpdate(DESCRIPTOR_WRITE_SUCCESS);
            } else {
                Log.i(TAG, "writeDescriptor failed for characteristic: " +
                        descriptor.getCharacteristic().getUuid().toString());
                broadcastUpdate(DESCRIPTOR_WRITE_FAILURE);
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic,
                                 final byte dataSource) {
        final Intent intent = new Intent(action);

        if (MYO_INFO_CHAR_UUID.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                if (data.length > SerialNumberSize) {
                    intent.putExtra(CHAR_UUID, MYO_INFO_CHAR_UUID.toString());
                    intent.putExtra(EXTRA_DATA, Arrays.copyOfRange(data, 0, SerialNumberSize));

                    if (data.length > 12) {
                        intent.putExtra(SKU, data[12]);
                    }
                }
            }
        } else if (MYO_FW_VERSION_CHAR_UUID.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                intent.putExtra(CHAR_UUID, MYO_FW_VERSION_CHAR_UUID.toString());
                final int majorFw = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 2))
                        .order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;
                final int minorFw = ByteBuffer.wrap(Arrays.copyOfRange(data, 2, 4))
                        .order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;
                final int patchFw = ByteBuffer.wrap(Arrays.copyOfRange(data, 4, 6))
                        .order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;
                final int revHw = ByteBuffer.wrap(Arrays.copyOfRange(data, 6, 8))
                        .order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;

                intent.putExtra(MAJOR_FW_VERSION, majorFw);
                intent.putExtra(MINOR_FW_VERSION, minorFw);
                intent.putExtra(FW_PATCH, patchFw);
                intent.putExtra(EXTRA_DATA, revHw);
            }
        } else if (MYO_EMG_DATA_0_CHAR_UUID.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length == EmgCharacteristicDataSize) {
                intent.putExtra(CHAR_UUID, MYO_EMG_DATA_0_CHAR_UUID.toString());
                intent.putExtra(EXTRA_DATA, data);
            }
        } else if (MYO_EMG_DATA_1_CHAR_UUID.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length == EmgCharacteristicDataSize) {
                intent.putExtra(CHAR_UUID, MYO_EMG_DATA_1_CHAR_UUID.toString());
                intent.putExtra(EXTRA_DATA, data);
            }
        } else if (MYO_EMG_DATA_2_CHAR_UUID.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length == EmgCharacteristicDataSize) {
                intent.putExtra(CHAR_UUID, MYO_EMG_DATA_2_CHAR_UUID.toString());
                intent.putExtra(EXTRA_DATA, data);
            }
        } else if (MYO_EMG_DATA_3_CHAR_UUID.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length == EmgCharacteristicDataSize) {
                intent.putExtra(CHAR_UUID, MYO_EMG_DATA_3_CHAR_UUID.toString());
                intent.putExtra(EXTRA_DATA, data);
            }
        } else if (BATTERY_LEVEL_CHAR_UUID.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                intent.putExtra(CHAR_UUID, BATTERY_LEVEL_CHAR_UUID.toString());
                intent.putExtra(DATA_SOURCE, dataSource);
                intent.putExtra(EXTRA_DATA, data[0]);
            }
        } else if (DEVICE_NAME_CHAR_UUID.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                intent.putExtra(CHAR_UUID, DEVICE_NAME_CHAR_UUID.toString());
                intent.putExtra(EXTRA_DATA, new String(data));
            }
        } else if (MYO_IMU_DATA_CHAR_UUID.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length == ImuCharacteristicDataSize) {
                intent.putExtra(CHAR_UUID, MYO_IMU_DATA_CHAR_UUID.toString());
                intent.putExtra(EXTRA_DATA, data);
            }
        }
        sendBroadcast(intent);
    }

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();

            if (device != null) {
                String deviceName = device.getName();

                if (deviceName != null && deviceName.equals(MYO_ARMBAND_NAME))
                {
                    scanLeDevice(false);

                    if (!connect(device.getAddress())) {
                        scanLeDevice(true);
                    }
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results) {
                BluetoothDevice device = result.getDevice();

                if (device != null) {
                    String deviceName = device.getName();

                    if (deviceName != null && deviceName.equals(MYO_ARMBAND_NAME))
                    {
                        scanLeDevice(false);

                        if (!connect(device.getAddress())) {
                            scanLeDevice(true);
                        }
                    }
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

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
                        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                                Intent.FLAG_ACTIVITY_SINGLE_TOP);

                        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                                REQUEST_ENABLE_BT_FOREGROUND_SERVICE, notificationIntent,
                                PendingIntent.FLAG_IMMUTABLE |
                                        PendingIntent.FLAG_UPDATE_CURRENT);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                                this, MioMarioApp.BT_SERVICE_CHANNEL_ID)
                                .setSmallIcon(R.mipmap.ic_myo)
                                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                        R.mipmap.ic_myo))
                                .setContentTitle(MioMarioApp.BT_SERVICE_CHANNEL_NAME)
                                .setContentText(MioMarioApp.BT_SERVICE_CHANNEL_DESCRIPTION)
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setContentIntent(pendingIntent);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            builder.setColor(0xffffffff);
                        }

                        Notification notification = builder.build();

                        startForeground(FOREGROUND_SERVICE_BT_ID, notification);

                        scanLeDevice(true);
                    }
                    break;

                    case SERVICE_PAUSED: {
                        mBtServiceState = BluetoothServiceState.SERVICE_STARTED;
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
                }
            }
            break;
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (initBtAdapter()) {
            mScanning = false;
            mConnectionState = STATE_DISCONNECTED;
            mBtServiceState = BluetoothServiceState.SERVICE_STOPPED;
            connectionState = "Unknown";
            deviceName = "Unknown";
            serialNumber = "Unknown";
            firmwareVersion = "Unknown";
            batteryLevel = 0;
            initBroadcastReceivers();
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            LocalBroadcastManager.getInstance(this).registerReceiver(mAppReceiver,
                    makeAppIntentFilter());

        } else {
            sendServiceBroadcast(ACTION_BLE_UNSUPPORTED, new Intent());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        releaseResources();

        disconnect();
    }

    private void releaseResources()
    {
        if (null != mGattUpdateReceiver) {
            unregisterReceiver(mGattUpdateReceiver);
            mGattUpdateReceiver = null;
        }

        if (null != mAppReceiver) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mAppReceiver);
            mAppReceiver = null;
        }
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    private boolean initBtAdapter() {
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "BLE not supported!");
            return false;
        }
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    // Handles various events fired by the Service.
    private void initBroadcastReceivers() {
        mAppReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (ACTION_READABLE_FIELDS_REQUESTED.equals(action)) {
                    sendServiceBroadcast(ACTION_READABLE_FIELDS_AVAILABLE, new Intent()
                            .putExtra(EXTRA_CONNECTION_STATE, connectionState)
                            .putExtra(EXTRA_DEVICE_NAME, deviceName)
                            .putExtra(EXTRA_SERIAL_NUMBER, serialNumber)
                            .putExtra(EXTRA_FIRMWARE_VERSION, firmwareVersion)
                            .putExtra(EXTRA_BATTERY_LEVEL, batteryLevel));
                }
            }
        };

        mGattUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (ACTION_GATT_CONNECTED.equals(action)) {
                    BluetoothDevice btDevice = mBluetoothGatt.getDevice();

                    if (null != btDevice) {
                        if (null != btDevice.getName()) {
                            Log.i(TAG, "Device connected: " + btDevice.getName());

                            if (btDevice.getName().equals(MYO_ARMBAND_NAME)) {
                                connectionState = "Connected";
                                sendServiceBroadcast(ACTION_BLE_CONNECTED, new Intent().putExtra(
                                        EXTRA_DATA, connectionState));
                            }
                        }
                    }
                } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                    BluetoothDevice btDevice = mBluetoothGatt.getDevice();

                    if (null != btDevice) {
                        if (null != btDevice.getName()) {
                            Log.i(TAG, "Device disconnected: " + btDevice.getName());

                            if (btDevice.getName().equals(MYO_ARMBAND_NAME)) {
                                connectionState = "Not connected";
                                sendServiceBroadcast(ACTION_BLE_DISCONNECTED, new Intent().putExtra(
                                        EXTRA_DATA, connectionState));
                            }
                        }
                    }

                    scanLeDevice(true);
                } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    List<BluetoothGattService> gattServices = getSupportedGattServices();

                    if (null != gattServices)
                    {
                        Log.i(TAG, "Discovered " + gattServices.size() + " GATT services!");
                        processGattServices(gattServices);
                    }
                } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                    String characteristicUuidString = intent.getStringExtra(CHAR_UUID);

                    if (null != characteristicUuidString) {
                        UUID characteristicUuid = UUID.fromString(characteristicUuidString);

                        if (characteristicUuid.equals(MYO_INFO_CHAR_UUID)) {
                            final byte[] snBytes = intent.getByteArrayExtra(EXTRA_DATA);
                            StringBuilder builder = new StringBuilder();
                            for (byte index = 0; index < snBytes.length; index++) {
                                builder.append(String.format("%02X", snBytes[index]));

                                if (index < (snBytes.length - 1)) {
                                    builder.append(".");
                                }
                            }

                            serialNumber = builder.toString();
                            String str = "Serial number: " + serialNumber;
                            Log.i(TAG, str);

                            byte sku = intent.getByteExtra(SKU, (byte) 0);
                            String skuStr;
                            switch (sku)
                            {
                                case MyoSkuBlack:
                                    skuStr = "Black Myo";
                                    break;
                                case MyoSkuWhite:
                                    skuStr = "White Myo";
                                    break;
                                case MyoSkuUnknown:
                                default:
                                    skuStr = "Unknown";
                                    break;
                            }

                            if (sku == MyoSkuWhite || sku == MyoSkuBlack) {
                                String str2 = "SKU: " + skuStr;
                                Log.i(TAG, str2);
                            }

                            sendServiceBroadcast(ACTION_BLE_INFO_AVAILABLE, new Intent().putExtra(
                                    EXTRA_DATA, serialNumber));

                            processFoundCharacteristics();
                        } else if (characteristicUuid.equals(MYO_FW_VERSION_CHAR_UUID)) {
                            int majorFw = intent.getIntExtra(MAJOR_FW_VERSION, 0);
                            int minorFw = intent.getIntExtra(MINOR_FW_VERSION, 0);
                            int patchFw = intent.getIntExtra(FW_PATCH, 0);
                            int revHw = intent.getIntExtra(EXTRA_DATA, 0);

                            String revHwStr;

                            switch (revHw) {
                                case MyoHwRevUnknown:
                                    revHwStr = "Unknown";
                                    break;
                                case MyoHwRevC:
                                    revHwStr = "REV-C";
                                    break;
                                case MyoHwRevD:
                                    revHwStr = "REV-D";
                                    break;
                                default:
                                    revHwStr = revHw + " known revisions";
                                    break;
                            }

                            firmwareVersion = majorFw + "." + minorFw + "-" + patchFw;
                            String str = "Myo FW: V" + firmwareVersion + "\nMyo HW: " + revHwStr;
                            Log.i(TAG, str);

                            sendServiceBroadcast(ACTION_BLE_FW_AVAILABLE, new Intent().putExtra(
                                    EXTRA_DATA, firmwareVersion));

                            processFoundCharacteristics();
                        } else if (characteristicUuid.equals(MYO_EMG_DATA_0_CHAR_UUID)) {
                            final byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
                            if (data != null) {
                                Log.i(TAG, "EMG0 data received: " + Arrays.toString(data));
                                sendServiceBroadcast(
                                        ACTION_BLE_EMG0_AVAILABLE,
                                        new Intent().putExtra(EXTRA_DATA, data));
                            }
                        } else if (characteristicUuid.equals(MYO_EMG_DATA_1_CHAR_UUID)) {
                            final byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
                            if (data != null) {
                                Log.i(TAG, "EMG1 data received: " + Arrays.toString(data));
                                sendServiceBroadcast(
                                        ACTION_BLE_EMG1_AVAILABLE,
                                        new Intent().putExtra(EXTRA_DATA, data));
                            }
                        } else if (characteristicUuid.equals(MYO_EMG_DATA_2_CHAR_UUID)) {
                            final byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
                            if (data != null) {
                                Log.i(TAG, "EMG2 data received: " + Arrays.toString(data));
                                sendServiceBroadcast(ACTION_BLE_EMG2_AVAILABLE,
                                        new Intent().putExtra(EXTRA_DATA, data));
                            }
                        } else if (characteristicUuid.equals(MYO_EMG_DATA_3_CHAR_UUID)) {
                            final byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
                            if (data != null) {
                                Log.i(TAG, "EMG3 data received: " + Arrays.toString(data));
                                sendServiceBroadcast(ACTION_BLE_EMG3_AVAILABLE,
                                        new Intent().putExtra(EXTRA_DATA, data));
                            }
                        } else if (characteristicUuid.equals(BATTERY_LEVEL_CHAR_UUID)) {
                            batteryLevel = intent.getByteExtra(EXTRA_DATA, (byte) 0);
                            String str = "Myo Battery Level: " + batteryLevel + "%";
                            Log.i(TAG, str);

                            sendServiceBroadcast(ACTION_BLE_BATTERY_LEVEL_AVAILABLE, new Intent()
                                    .putExtra(EXTRA_DATA, batteryLevel));

                            if (ReadOperation == intent.getByteExtra(DATA_SOURCE, ReadOperation)) {
                                processFoundCharacteristics();
                            }
                        } else if (characteristicUuid.equals(DEVICE_NAME_CHAR_UUID)) {
                            deviceName = intent.getStringExtra(EXTRA_DATA);
                            Log.i(TAG, "Device Name: " + deviceName);

                            sendServiceBroadcast(ACTION_BLE_DEVICE_NAME_AVAILABLE, new Intent()
                                    .putExtra(EXTRA_DATA, deviceName));

                            processFoundCharacteristics();
                        } else if (characteristicUuid.equals(MYO_IMU_DATA_CHAR_UUID)) {
                            final byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
                            if (data != null) {
                                Log.i(TAG, "IMU data received: " + Arrays.toString(data));

                                final short orientW = ByteBuffer.wrap(Arrays.copyOfRange(data,
                                        0, 2)).order(ByteOrder.LITTLE_ENDIAN).getShort();
                                final short orientX = ByteBuffer.wrap(Arrays.copyOfRange(data,
                                        2, 4)).order(ByteOrder.LITTLE_ENDIAN).getShort();
                                final short orientY = ByteBuffer.wrap(Arrays.copyOfRange(data,
                                        4, 6)).order(ByteOrder.LITTLE_ENDIAN).getShort();
                                final short orientZ = ByteBuffer.wrap(Arrays.copyOfRange(data,
                                        6, 8)).order(ByteOrder.LITTLE_ENDIAN).getShort();

                                final short accelX = ByteBuffer.wrap(Arrays.copyOfRange(data,
                                        8, 10)).order(ByteOrder.LITTLE_ENDIAN).getShort();
                                final short accelY = ByteBuffer.wrap(Arrays.copyOfRange(data,
                                        10, 12)).order(ByteOrder.LITTLE_ENDIAN).getShort();
                                final short accelZ = ByteBuffer.wrap(Arrays.copyOfRange(data,
                                        12, 14)).order(ByteOrder.LITTLE_ENDIAN).getShort();

                                final short gyroX = ByteBuffer.wrap(Arrays.copyOfRange(data,
                                        14, 16)).order(ByteOrder.LITTLE_ENDIAN).getShort();
                                final short gyroY = ByteBuffer.wrap(Arrays.copyOfRange(data,
                                        16, 18)).order(ByteOrder.LITTLE_ENDIAN).getShort();
                                final short gyroZ = ByteBuffer.wrap(Arrays.copyOfRange(data,
                                        18, 20)).order(ByteOrder.LITTLE_ENDIAN).getShort();

                                sendServiceBroadcast(ACTION_BLE_IMU_AVAILABLE,
                                        new Intent().putExtra(EXTRA_DATA, new short[] {
                                                orientW, orientX, orientY, orientZ,
                                                accelX, accelY, accelZ,
                                                gyroX, gyroY, gyroZ }));
                            }
                        }
                    }
                } else if (ACTION_DATA_UNAVAILABLE.equals(action)) {
                    processFoundCharacteristics();
                } else if (DESCRIPTOR_WRITE_SUCCESS.equals(action)) {
                    processFoundCharacteristics();
                } else if (DESCRIPTOR_WRITE_FAILURE.equals(action)) {
                    processFoundCharacteristics();
                } else {
                    Log.i(TAG, "Undefined action: " + action);
                }
            }
        };
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device. Try to reconnect.
        if (address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }
    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }
    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(
     * android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     */
    public void readCharacteristic(byte characteristicIndex) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        try {
            UUID uuid = btChars.get(characteristicIndex).btChar.getUuid();

            if (uuid.equals(MYO_INFO_CHAR_UUID) ||
                    uuid.equals(MYO_FW_VERSION_CHAR_UUID) ||
                    uuid.equals(BATTERY_LEVEL_CHAR_UUID) ||
                    uuid.equals(DEVICE_NAME_CHAR_UUID)) {
                mBluetoothGatt.readCharacteristic(btChars.get(characteristicIndex).btChar);
                btChars.remove(characteristicIndex);
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }
    }
    /**
     * Enables or disables notification on a given characteristic.
     */
    public void setCharacteristicNotification(byte characteristicIndex) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        try {
            UUID uuid = btChars.get(characteristicIndex).btChar.getUuid();

            if (uuid.equals(MYO_EMG_DATA_0_CHAR_UUID) ||
                    uuid.equals(MYO_EMG_DATA_1_CHAR_UUID) ||
                    uuid.equals(MYO_EMG_DATA_2_CHAR_UUID) ||
                    uuid.equals(MYO_EMG_DATA_3_CHAR_UUID) ||
                    uuid.equals(MYO_IMU_DATA_CHAR_UUID) ||
                    uuid.equals(BATTERY_LEVEL_CHAR_UUID)) {
                BluetoothGattDescriptor descriptor = btChars.get(characteristicIndex).btChar
                        .getDescriptor(CCC_DESCRIPTOR_UUID);

                if(descriptor == null) {
                    Log.e(TAG, "Could not get CCC descriptor for characteristic: " +
                            btChars.get(characteristicIndex).btChar.getUuid().toString());
                } else {
                    if (!mBluetoothGatt.setCharacteristicNotification(
                            btChars.get(characteristicIndex).btChar, true)) {
                        Log.e(TAG, "setCharacteristicNotification failed for descriptor: " +
                                descriptor.getUuid().toString());
                    }

                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                    if(!mBluetoothGatt.writeDescriptor(descriptor)) {
                        Log.e(TAG, "writeDescriptor failed for descriptor: " +
                                descriptor.getUuid().toString());
                    }
                }
                btChars.remove(characteristicIndex);
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }
    }
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

    private void scanLeDevice(final boolean enable) {
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (enable) {
            if (mScanning) {
                mScanning = false;
                bluetoothLeScanner.stopScan(mScanCallback);
            }
            mScanning = true;
            bluetoothLeScanner.startScan(mScanCallback);
        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan(mScanCallback);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ACTION_DATA_UNAVAILABLE);
        intentFilter.addAction(DESCRIPTOR_WRITE_SUCCESS);
        intentFilter.addAction(DESCRIPTOR_WRITE_FAILURE);
        return intentFilter;
    }

    private static IntentFilter makeAppIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_READABLE_FIELDS_REQUESTED);
        return intentFilter;
    }

    private void processGattServices(List<BluetoothGattService> gattServices) {
        btChars = new ArrayList<>();

        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            Log.i(TAG, "GATT service: " + gattService.getUuid().toString());

            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                Log.i(TAG, "GATT characteristic: " + gattCharacteristic.getUuid().toString());

                findGattCharacteristic(gattCharacteristic);
            }
        }

        processFoundCharacteristics();
    }

    private void findGattCharacteristic(BluetoothGattCharacteristic characteristic) {
        UUID uuid = characteristic.getUuid();

        if (uuid.equals(MYO_INFO_CHAR_UUID) ||
                uuid.equals(MYO_FW_VERSION_CHAR_UUID) ||
                uuid.equals(BATTERY_LEVEL_CHAR_UUID) ||
                uuid.equals(DEVICE_NAME_CHAR_UUID)) {
            btChars.add(new BtChar(characteristic, BtCharProperty.Read));
        }

        if (uuid.equals(MYO_EMG_DATA_0_CHAR_UUID) ||
                uuid.equals(MYO_EMG_DATA_1_CHAR_UUID) ||
                uuid.equals(MYO_EMG_DATA_2_CHAR_UUID) ||
                uuid.equals(MYO_EMG_DATA_3_CHAR_UUID) ||
                uuid.equals(MYO_IMU_DATA_CHAR_UUID) ||
                uuid.equals(BATTERY_LEVEL_CHAR_UUID)) {
            btChars.add(new BtChar(characteristic, BtCharProperty.Notify));
        }

        if (uuid.equals(MYO_CMD_CHAR_UUID)) {
            mMyoCommandCharacteristic = characteristic;
        }
    }

    private void processFoundCharacteristics() {
        if (btChars != null) {
            if (btChars.size() > 0) {
                byte charIndex = NO_INDEX;

                for (byte index = 0; index < btChars.size(); index++) {
                    if (btChars.get(index).btCharProperty == BtCharProperty.Read) {
                        charIndex = index;
                        break;
                    }
                }

                if (charIndex == NO_INDEX) {
                    for (byte index = 0; index < btChars.size(); index++) {
                        if (btChars.get(index).btCharProperty == BtCharProperty.Notify) {
                            charIndex = index;
                            break;
                        }
                    }

                    if (charIndex != NO_INDEX) {
                        setCharacteristicNotification(charIndex);
                    }
                } else {
                    readCharacteristic(charIndex);
                }
            } else {
                writeCommandCharacteristic();
            }
        }
    }

    private void writeCommandCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        if (mMyoCommandCharacteristic == null) {
            Log.w(TAG, "Myo command characteristic not initialized");
            return;
        }

        byte[] frame = new byte[FrameSize];
        frame[0] = CommandSetMode;
        frame[1] = PayloadSize;
        frame[2] = EmgModeSendEmg;
        frame[3] = ImuModeSendData;
        frame[4] = ClassifierModeDisabled;
        mMyoCommandCharacteristic.setValue(frame);
        if (!mBluetoothGatt.writeCharacteristic(mMyoCommandCharacteristic)) {
            Log.w(TAG, "Myo command characteristic write failed");
        }
    }

    private void sendServiceBroadcast(String action, Intent intent) {
        intent.setAction(action);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}
