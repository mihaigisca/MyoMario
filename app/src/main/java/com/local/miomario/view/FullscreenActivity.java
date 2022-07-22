package com.local.miomario.view;

import android.Manifest;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.local.miomario.R;
import com.local.miomario.fragment.MyoGameFragment;
import com.local.miomario.service.BluetoothLeService;
import com.local.miomario.service.BluetoothService;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    private final static String TAG = FullscreenActivity.class.getSimpleName();

    private static final int REQUEST_PERMISSION_BT = 10;
    private static final int REQUEST_PERMISSION_LOCATION = 20;
    private static final int REQUEST_PERMISSION_FOREGROUND_SERVICE = 30;
    private BluetoothAdapter bluetoothAdapter;
    private ActivityResultLauncher<Intent> activityBluetoothResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        setContentView(R.layout.activity_fullscreen);

        initializeActivityResultLaunchers();

        if (initBtAdapter()) {
            tryForBluetooth();
        } else {
            finishAndRemoveTask();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private void initializeActivityResultLaunchers() {
        activityBluetoothResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            tryForLocation();
                        } else {
                            showToast("Bluetooth enabling denied");
                            finishAndRemoveTask();
                        }
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean initBtAdapter() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showToast("BLE not supported!");
            return false;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(
                Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return false;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    private void tryForBluetooth() {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH) && PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH_ADMIN)) {
            if (bluetoothAdapter.isEnabled()) {
                tryForLocation();
            } else {
                Intent bluetoothEnableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activityBluetoothResultLauncher.launch(bluetoothEnableIntent);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN},
                    REQUEST_PERMISSION_BT);
        }
    }

    private void tryForLocation() {
        if(PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) && PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION))
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                tryForForegroundService();
            } else {
                startGameFragment();
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_LOCATION);
        }
    }

    private void tryForForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if(PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this,
                    Manifest.permission.FOREGROUND_SERVICE))
            {
                startGameFragment();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.FOREGROUND_SERVICE},
                        REQUEST_PERMISSION_FOREGROUND_SERVICE);
            }
        } else {
            startGameFragment();
        }
    }

    private void btStartService() {
        Intent serviceNotificationIntent = new Intent(this, BluetoothLeService.class);
        serviceNotificationIntent.putExtra(
                BluetoothService.BT_SERVICE_ID,
                BluetoothService.BT_SERVICE_START);
        ContextCompat.startForegroundService(this, serviceNotificationIntent);
    }

    private void startGameFragment() {
        btStartService();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_holder, MyoGameFragment.class, null)
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_BT: {
                if (0 < grantResults.length &&
                        PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                    final Handler handler = new Handler(Looper.getMainLooper());
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            tryForLocation();
                        }
                    };
                    handler.postDelayed(r, 10);
                } else {
                    showToast("Bluetooth permission not granted");
                    finishAndRemoveTask();
                }
            }
            break;

            case REQUEST_PERMISSION_LOCATION: {
                if (0 < grantResults.length &&
                        PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                    final Handler handler = new Handler(Looper.getMainLooper());
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            tryForForegroundService();
                        }
                    };
                    handler.postDelayed(r, 10);
                } else {
                    showToast("Location permission not granted");
                    finishAndRemoveTask();
                }
            }
            break;

            case REQUEST_PERMISSION_FOREGROUND_SERVICE: {
                if (0 < grantResults.length &&
                        PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                    final Handler handler = new Handler(Looper.getMainLooper());
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            startGameFragment();
                        }
                    };
                    handler.postDelayed(r, 10);
                } else {
                    showToast("Foreground service permission not granted");
                    finishAndRemoveTask();
                }
            }
            break;

            default:
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}