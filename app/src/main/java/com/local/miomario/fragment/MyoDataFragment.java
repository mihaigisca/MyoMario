package com.local.miomario.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.local.miomario.databinding.FragmentMyoDataBinding;
import com.local.miomario.service.BluetoothLeService;

import java.util.Locale;

public class MyoDataFragment extends Fragment {
    private BroadcastReceiver bleReceiver;
    private FragmentMyoDataBinding binding;

    public static final float MyoHwOrientationScale = 16384.0f;
    public static final float MyoHwAccelerometerScale = 2048.0f;
    public static final float MyoHwGyroscopeScale = 16.0f;

    public MyoDataFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != getActivity()) {
            initializeReceiver();
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(bleReceiver,
                    makeIntentFilter());
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(new Intent(
                    BluetoothLeService.ACTION_READABLE_FIELDS_REQUESTED));
        }
    }

    @Override
    public void onPause() {
        if (null != getActivity() && null != bleReceiver) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(bleReceiver);
            bleReceiver = null;
        }
        super.onPause();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMyoDataBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setDataFragment(this);
        return binding.getRoot();
    }

    public void onBackButtonClicked() {
        requireActivity().onBackPressed();
    }

    private void initializeReceiver() {
        bleReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothLeService.ACTION_BLE_CONNECTED.equals(action)) {
                    binding.connectionValue.setText(intent.getStringExtra(BluetoothLeService
                            .EXTRA_DATA));
                } else if (BluetoothLeService.ACTION_BLE_DISCONNECTED.equals(action)) {
                    binding.connectionValue.setText(intent.getStringExtra(BluetoothLeService
                            .EXTRA_DATA));
                } else if (BluetoothLeService.ACTION_BLE_BATTERY_LEVEL_AVAILABLE.equals(action)) {
                    binding.batteryLevelValue.setText(String.format(
                            Locale.getDefault(),"%d", intent.getByteExtra(BluetoothLeService
                                    .EXTRA_DATA, (byte) 0)));
                } else if (BluetoothLeService.ACTION_BLE_INFO_AVAILABLE.equals(action)) {
                    binding.serialNumberValue.setText(intent.getStringExtra(
                            BluetoothLeService.EXTRA_DATA));
                } else if (BluetoothLeService.ACTION_BLE_FW_AVAILABLE.equals(action)) {
                    binding.firmwareVersionValue.setText(intent.getStringExtra(
                            BluetoothLeService.EXTRA_DATA));
                } else if (BluetoothLeService.ACTION_BLE_DEVICE_NAME_AVAILABLE.equals(action)) {
                    binding.deviceNameValue.setText(intent.getStringExtra(
                            BluetoothLeService.EXTRA_DATA));
                } else if (BluetoothLeService.ACTION_BLE_EMG0_AVAILABLE.equals(action)) {
                    final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    binding.emg0Sensor0Value.setText(
                            String.format(Locale.getDefault(), "%d",
                                    calculateAverage(
                                            data,
                                            MyoGameFragment.EMG_SENSOR1_START_INDEX,
                                            MyoGameFragment.EMG_SENSOR1_END_INDEX)));
                    binding.emg0Sensor1Value.setText(
                            String.format(Locale.getDefault(), "%d",
                                    calculateAverage(
                                            data,
                                            MyoGameFragment.EMG_SENSOR2_START_INDEX,
                                            MyoGameFragment.EMG_SENSOR2_END_INDEX)));
                } else if (BluetoothLeService.ACTION_BLE_EMG1_AVAILABLE.equals(action)) {
                    final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    binding.emg1Sensor2Value.setText(
                            String.format(Locale.getDefault(), "%d",
                                    calculateAverage(
                                            data,
                                            MyoGameFragment.EMG_SENSOR1_START_INDEX,
                                            MyoGameFragment.EMG_SENSOR1_END_INDEX)));
                    binding.emg1Sensor3Value.setText(
                            String.format(Locale.getDefault(), "%d",
                                    calculateAverage(
                                            data,
                                            MyoGameFragment.EMG_SENSOR2_START_INDEX,
                                            MyoGameFragment.EMG_SENSOR2_END_INDEX)));
                } else if (BluetoothLeService.ACTION_BLE_EMG2_AVAILABLE.equals(action)) {
                    final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    binding.emg2Sensor4Value.setText(
                            String.format(Locale.getDefault(), "%d",
                                    calculateAverage(
                                            data,
                                            MyoGameFragment.EMG_SENSOR1_START_INDEX,
                                            MyoGameFragment.EMG_SENSOR1_END_INDEX)));
                    binding.emg2Sensor5Value.setText(
                            String.format(Locale.getDefault(), "%d",
                                    calculateAverage(
                                            data,
                                            MyoGameFragment.EMG_SENSOR2_START_INDEX,
                                            MyoGameFragment.EMG_SENSOR2_END_INDEX)));
                } else if (BluetoothLeService.ACTION_BLE_EMG3_AVAILABLE.equals(action)) {
                    final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    binding.emg3Sensor6Value.setText(
                            String.format(Locale.getDefault(), "%d",
                                    calculateAverage(
                                            data,
                                            MyoGameFragment.EMG_SENSOR1_START_INDEX,
                                            MyoGameFragment.EMG_SENSOR1_END_INDEX)));
                    binding.emg3Sensor7Value.setText(
                            String.format(Locale.getDefault(), "%d",
                                    calculateAverage(
                                            data,
                                            MyoGameFragment.EMG_SENSOR2_START_INDEX,
                                            MyoGameFragment.EMG_SENSOR2_END_INDEX)));
                } else if (BluetoothLeService.ACTION_BLE_IMU_AVAILABLE.equals(action)) {
                    short[] imuData = intent.getShortArrayExtra(BluetoothLeService.EXTRA_DATA);
                    binding.imuOrientWValue.setText(
                            String.format(Locale.getDefault(), "%.1f",
                                    imuData[0] / MyoHwOrientationScale));
                    binding.imuOrientXValue.setText(
                            String.format(Locale.getDefault(), "%.1f",
                                    imuData[1] / MyoHwOrientationScale));
                    binding.imuOrientYValue.setText(
                            String.format(Locale.getDefault(), "%.1f",
                                    imuData[2] / MyoHwOrientationScale));
                    binding.imuOrientZValue.setText(
                            String.format(Locale.getDefault(), "%.1f",
                                    imuData[3] / MyoHwOrientationScale));
                    binding.imuAccelXValue.setText(
                            String.format(Locale.getDefault(), "%.1f",
                                    imuData[4] / MyoHwAccelerometerScale));
                    binding.imuAccelYValue.setText(
                            String.format(Locale.getDefault(), "%.1f",
                                    imuData[5] / MyoHwAccelerometerScale));
                    binding.imuAccelZValue.setText(
                            String.format(Locale.getDefault(), "%.1f",
                                    imuData[6] / MyoHwAccelerometerScale));
                    binding.imuGyroXValue.setText(
                            String.format(Locale.getDefault(), "%.1f",
                                    imuData[7] / MyoHwGyroscopeScale));
                    binding.imuGyroYValue.setText(
                            String.format(Locale.getDefault(), "%.1f",
                                    imuData[8] / MyoHwGyroscopeScale));
                    binding.imuGyroZValue.setText(
                            String.format(Locale.getDefault(), "%.1f",
                                    imuData[9] / MyoHwGyroscopeScale));
                } else if (BluetoothLeService.ACTION_READABLE_FIELDS_AVAILABLE.equals(action)) {
                    binding.connectionValue.setText(intent.getStringExtra(BluetoothLeService
                            .EXTRA_CONNECTION_STATE));
                    binding.deviceNameValue.setText(intent.getStringExtra(BluetoothLeService
                            .EXTRA_DEVICE_NAME));
                    binding.serialNumberValue.setText(intent.getStringExtra(BluetoothLeService
                            .EXTRA_SERIAL_NUMBER));
                    binding.firmwareVersionValue.setText(intent.getStringExtra(BluetoothLeService
                            .EXTRA_FIRMWARE_VERSION));
                    binding.batteryLevelValue.setText(String.format(
                            Locale.getDefault(),"%d", intent.getByteExtra(BluetoothLeService
                                    .EXTRA_BATTERY_LEVEL, (byte) 0)));
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

    private byte calculateAverage(byte[] data, int startIndex, int endIndex) {
        short sum = 0;
        for (int index = startIndex; index < endIndex; index++) {
            sum += data[index];
        }
        return (byte) (sum / (endIndex - startIndex));
    }
}