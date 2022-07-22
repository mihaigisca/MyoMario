package com.local.miomario;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class MioMarioApp extends Application {
    public static final String BT_SERVICE_CHANNEL_ID = "MIOMARIO_BT_SERVICE_CHANNEL_ID";
    public static final String BT_SERVICE_CHANNEL_NAME = "MioMario BLE Service";
    public static final String BT_SERVICE_CHANNEL_DESCRIPTION =
            "Service handles communication with a Myo Armband.";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            if (null != notificationManager) {
                NotificationChannel btServiceNotificationChannel = new NotificationChannel(
                        BT_SERVICE_CHANNEL_ID,
                        BT_SERVICE_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                btServiceNotificationChannel.setDescription(BT_SERVICE_CHANNEL_DESCRIPTION);

                notificationManager.createNotificationChannel(btServiceNotificationChannel);
            }
        }
    }
}
