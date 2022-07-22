package com.local.miomario.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.local.miomario.R;

import com.local.miomario.databinding.FragmentMyoGameSvBinding;
import com.local.miomario.service.BluetoothLeService;

import java.util.Locale;

public class MyoGameFragment extends Fragment {
    private final static String TAG = MyoGameFragment.class.getSimpleName();
    public static final int EMG_SENSOR1_START_INDEX = 0;
    public static final int EMG_SENSOR1_END_INDEX = 8;
    public static final int EMG_SENSOR2_START_INDEX = 8;
    public static final int EMG_SENSOR2_END_INDEX = 16;

    private final byte EMG_LOW_LIMIT = 3;
    private final byte EMG_HIGH_LIMIT = 28;
    private int Y_LOW_POS;
    private int Y_HIGH_POS;
    private int Y_STEP;

    private BroadcastReceiver bleReceiver;
    private MyoGameSurface myoGameSurface;
    private FragmentMyoGameSvBinding binding;
    private Handler processEmgHandler;
    private Runnable processEmgRunnable;
    private byte emg0Sensor0Value, emg0Sensor1Value;
    private byte emg1Sensor2Value, emg1Sensor3Value;
    private byte emg2Sensor4Value, emg2Sensor5Value;
    private byte emg3Sensor6Value, emg3Sensor7Value;

    public MyoGameFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.emg0Sensor0Value = 0;
        this.emg0Sensor1Value = 0;
        this.emg1Sensor2Value = 0;
        this.emg1Sensor3Value = 0;
        this.emg2Sensor4Value = 0;
        this.emg2Sensor5Value = 0;
        this.emg3Sensor6Value = 0;
        this.emg3Sensor7Value = 0;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.binding = FragmentMyoGameSvBinding.inflate(inflater, container, false);
        this.binding.setLifecycleOwner(getViewLifecycleOwner());
        this.binding.setGameFragment(this);

        Point point = new Point();
        requireActivity().getWindowManager().getDefaultDisplay().getSize(point);
        this.myoGameSurface = new MyoGameSurface(requireContext(), point.x, point.y);
        this.Y_LOW_POS = (int)(point.y * 0.75f);
        this.Y_HIGH_POS = (int)(point.y * 0.25f);
        this.Y_STEP = (Y_LOW_POS - Y_HIGH_POS) / (EMG_HIGH_LIMIT - EMG_LOW_LIMIT);

        this.binding.myoGameFragment.addView(this.myoGameSurface);
        this.binding.myoDataButton.bringToFront();
        return this.binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        this.myoGameSurface.resume();

        if (null != getActivity()) {
            this.initializeReceiver();
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(bleReceiver,
                    makeIntentFilter());
        }

        this.processEmgHandler = new Handler(Looper.getMainLooper());

        this.initializeRunnable();
        this.processEmgHandler.post(this.processEmgRunnable);
    }

    @Override
    public void onPause() {
        if (null != this.processEmgHandler) {
            this.processEmgHandler.removeCallbacksAndMessages(null);
            this.processEmgHandler = null;
        }
        if (null != getActivity() && null != this.bleReceiver) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this.bleReceiver);
            this.bleReceiver = null;
        }
        this.myoGameSurface.pause();
        super.onPause();
    }

    public void onSettingsButtonClicked() {
        getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .addToBackStack("MyoGameFragment")
                .add(R.id.fragment_holder, MyoDataFragment.class, null)
                .commit();
    }

    private void initializeReceiver() {
        this.bleReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothLeService.ACTION_BLE_CONNECTED.equals(action)) {
                    String connectionValue = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                } else if (BluetoothLeService.ACTION_BLE_DISCONNECTED.equals(action)) {
                    String connectionValue = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                } else if (BluetoothLeService.ACTION_BLE_BATTERY_LEVEL_AVAILABLE.equals(action)) {
                    String batteryLevelValue = String.format(Locale.getDefault(),"%d",
                            intent.getByteExtra(BluetoothLeService.EXTRA_DATA, (byte) 0));
                } else if (BluetoothLeService.ACTION_BLE_INFO_AVAILABLE.equals(action)) {
                    String serialNumberValue = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                } else if (BluetoothLeService.ACTION_BLE_FW_AVAILABLE.equals(action)) {
                    String firmwareVersionValue = intent.getStringExtra(
                            BluetoothLeService.EXTRA_DATA);
                } else if (BluetoothLeService.ACTION_BLE_DEVICE_NAME_AVAILABLE.equals(action)) {
                    String deviceNameValue = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                } else if (BluetoothLeService.ACTION_BLE_EMG0_AVAILABLE.equals(action)) {
                    final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    emg0Sensor0Value = calculateAverageAbs(
                            data, EMG_SENSOR1_START_INDEX, EMG_SENSOR1_END_INDEX);
                    emg0Sensor1Value = calculateAverageAbs(
                            data, EMG_SENSOR2_START_INDEX, EMG_SENSOR2_END_INDEX);
                } else if (BluetoothLeService.ACTION_BLE_EMG1_AVAILABLE.equals(action)) {
                    final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    emg1Sensor2Value = calculateAverageAbs(
                            data, EMG_SENSOR1_START_INDEX, EMG_SENSOR1_END_INDEX);
                    emg1Sensor3Value = calculateAverageAbs(
                            data, EMG_SENSOR2_START_INDEX, EMG_SENSOR2_END_INDEX);
                } else if (BluetoothLeService.ACTION_BLE_EMG2_AVAILABLE.equals(action)) {
                    final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    emg2Sensor4Value = calculateAverageAbs(
                            data, EMG_SENSOR1_START_INDEX, EMG_SENSOR1_END_INDEX);
                    emg2Sensor5Value = calculateAverageAbs(
                            data, EMG_SENSOR2_START_INDEX, EMG_SENSOR2_END_INDEX);
                } else if (BluetoothLeService.ACTION_BLE_EMG3_AVAILABLE.equals(action)) {
                    final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    emg3Sensor6Value = calculateAverageAbs(
                            data, EMG_SENSOR1_START_INDEX, EMG_SENSOR1_END_INDEX);
                    emg3Sensor7Value = calculateAverageAbs(
                            data, EMG_SENSOR2_START_INDEX, EMG_SENSOR2_END_INDEX);
                } else if (BluetoothLeService.ACTION_BLE_IMU_AVAILABLE.equals(action)) {
                    short[] imuData = intent.getShortArrayExtra(BluetoothLeService.EXTRA_DATA);
                    float imuOrientWValue = imuData[0] / MyoDataFragment.MyoHwOrientationScale;
                    float imuOrientXValue = imuData[1] / MyoDataFragment.MyoHwOrientationScale;
                    float imuOrientYValue = imuData[2] / MyoDataFragment.MyoHwOrientationScale;
                    float imuOrientZValue = imuData[3] / MyoDataFragment.MyoHwOrientationScale;
                    float imuAccelXValue = imuData[4] / MyoDataFragment.MyoHwAccelerometerScale;
                    float imuAccelYValue = imuData[5] / MyoDataFragment.MyoHwAccelerometerScale;
                    float imuAccelZValue = imuData[6] / MyoDataFragment.MyoHwAccelerometerScale;
                    float imuGyroXValue = imuData[7] / MyoDataFragment.MyoHwGyroscopeScale;
                    float imuGyroYValue = imuData[8] / MyoDataFragment.MyoHwGyroscopeScale;
                    float imuGyroZValue = imuData[9] / MyoDataFragment.MyoHwGyroscopeScale;
                } else if (BluetoothLeService.ACTION_READABLE_FIELDS_AVAILABLE.equals(action)) {
                    String connectionValue = intent.getStringExtra(
                            BluetoothLeService.EXTRA_CONNECTION_STATE);
                    String deviceNameValue = intent.getStringExtra(
                            BluetoothLeService.EXTRA_DEVICE_NAME);
                    String serialNumberValue = intent.getStringExtra(
                            BluetoothLeService.EXTRA_SERIAL_NUMBER);
                    String firmwareVersionValue = intent.getStringExtra(
                            BluetoothLeService.EXTRA_FIRMWARE_VERSION);
                    String batteryLevelValue = String.format(Locale.getDefault(),"%d", intent
                            .getByteExtra(BluetoothLeService.EXTRA_BATTERY_LEVEL, (byte) 0));
                }
            }
        };
    }

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_BATTERY_LEVEL_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_INFO_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_FW_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_DEVICE_NAME_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_EMG0_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_EMG1_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_EMG2_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_EMG3_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_IMU_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_READABLE_FIELDS_AVAILABLE);
        return intentFilter;
    }

    private void processEmg() {
        byte averageEmg = (byte)((this.emg0Sensor0Value + this.emg0Sensor1Value +
                this.emg1Sensor2Value + this.emg1Sensor3Value + this.emg2Sensor4Value +
                this.emg2Sensor5Value + this.emg3Sensor6Value + this.emg3Sensor7Value) / 8);

        Log.i(TAG, "Average EMG value: " + averageEmg);

        if (averageEmg > this.EMG_HIGH_LIMIT) {
            averageEmg = this.EMG_HIGH_LIMIT;
        } else if (averageEmg < this.EMG_LOW_LIMIT) {
            averageEmg = this.EMG_LOW_LIMIT;
        }

        this.myoGameSurface.updateMarioY(this.Y_LOW_POS - (averageEmg * this.Y_STEP));
    }

    private void initializeRunnable() {
        this.processEmgRunnable = new Runnable() {
            @Override
            public void run() {
                processEmg();
                processEmgHandler.postDelayed(processEmgRunnable, 25);
            }
        };
    }

    private byte calculateAverageAbs(byte[] data, int startIndex, int endIndex) {
        short sum = 0;
        for (int index = startIndex; index < endIndex; index++) {
            sum += Math.abs(data[index]);
        }
        return (byte) (sum / (endIndex - startIndex));
    }
}