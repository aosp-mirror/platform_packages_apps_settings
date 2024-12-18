/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.connecteddevice.audiosharing;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class AudioSharingReceiver extends BroadcastReceiver {
    private static final String TAG = "AudioSharingNotification";
    private static final String ACTION_LE_AUDIO_SHARING_SETTINGS =
            "com.android.settings.BLUETOOTH_AUDIO_SHARING_SETTINGS";
    private static final String ACTION_LE_AUDIO_SHARING_STOP =
            "com.android.settings.action.BLUETOOTH_LE_AUDIO_SHARING_STOP";
    private static final String CHANNEL_ID = "bluetooth_notification_channel";
    private static final int NOTIFICATION_ID =
            com.android.settingslib.R.drawable.ic_bt_le_audio_sharing;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!BluetoothUtils.isAudioSharingEnabled()) {
            Log.w(TAG, "Skip handling received intent, flag is off.");
            return;
        }
        String action = intent.getAction();
        if (action == null) {
            Log.w(TAG, "Received unexpected intent with null action.");
            return;
        }
        MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        switch (action) {
            case LocalBluetoothLeBroadcast.ACTION_LE_AUDIO_SHARING_STATE_CHANGE:
                int state =
                        intent.getIntExtra(
                                LocalBluetoothLeBroadcast.EXTRA_LE_AUDIO_SHARING_STATE, -1);
                if (state == LocalBluetoothLeBroadcast.BROADCAST_STATE_ON) {
                    showSharingNotification(context);
                    metricsFeatureProvider.action(
                            context, SettingsEnums.ACTION_SHOW_AUDIO_SHARING_NOTIFICATION);
                } else if (state == LocalBluetoothLeBroadcast.BROADCAST_STATE_OFF) {
                    cancelSharingNotification(context);
                    metricsFeatureProvider.action(
                            context, SettingsEnums.ACTION_CANCEL_AUDIO_SHARING_NOTIFICATION);
                } else {
                    Log.w(
                            TAG,
                            "Skip handling ACTION_LE_AUDIO_SHARING_STATE_CHANGE, invalid extras.");
                }
                break;
            case ACTION_LE_AUDIO_SHARING_STOP:
                LocalBluetoothManager manager = Utils.getLocalBtManager(context);
                if (BluetoothUtils.isBroadcasting(manager)) {
                    AudioSharingUtils.stopBroadcasting(manager);
                    metricsFeatureProvider.action(
                            context, SettingsEnums.ACTION_STOP_AUDIO_SHARING_FROM_NOTIFICATION);
                } else {
                    cancelSharingNotification(context);
                    metricsFeatureProvider.action(
                            context, SettingsEnums.ACTION_CANCEL_AUDIO_SHARING_NOTIFICATION);
                }
                break;
            default:
                Log.w(TAG, "Received unexpected intent " + intent.getAction());
        }
    }

    private void showSharingNotification(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            Log.d(TAG, "Create bluetooth notification channel");
            NotificationChannel notificationChannel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            context.getString(com.android.settings.R.string.bluetooth),
                            NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(notificationChannel);
        }
        Intent stopIntent =
                new Intent(ACTION_LE_AUDIO_SHARING_STOP).setPackage(context.getPackageName());
        PendingIntent stopPendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        R.string.audio_sharing_stop_button_label,
                        stopIntent,
                        PendingIntent.FLAG_IMMUTABLE);
        Intent settingsIntent =
                new Intent(ACTION_LE_AUDIO_SHARING_SETTINGS)
                        .setPackage(context.getPackageName())
                        .putExtra(
                                MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                                SettingsEnums.NOTIFICATION_AUDIO_SHARING);
        PendingIntent settingsPendingIntent =
                PendingIntent.getActivity(
                        context,
                        R.string.audio_sharing_settings_button_label,
                        settingsIntent,
                        PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action stopAction =
                new NotificationCompat.Action.Builder(
                                0,
                                context.getString(R.string.audio_sharing_stop_button_label),
                                stopPendingIntent)
                        .build();
        NotificationCompat.Action settingsAction =
                new NotificationCompat.Action.Builder(
                                0,
                                context.getString(R.string.audio_sharing_settings_button_label),
                                settingsPendingIntent)
                        .build();
        final Bundle extras = new Bundle();
        extras.putString(
                Notification.EXTRA_SUBSTITUTE_APP_NAME,
                context.getString(R.string.audio_sharing_title));
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing)
                        .setLocalOnly(true)
                        .setContentTitle(
                                context.getString(R.string.audio_sharing_notification_title))
                        .setContentText(
                                context.getString(R.string.audio_sharing_notification_content))
                        .setOngoing(true)
                        .setSilent(true)
                        .setColor(
                                context.getColor(
                                        com.android.internal.R.color
                                                .system_notification_accent_color))
                        .setContentIntent(settingsPendingIntent)
                        .addAction(stopAction)
                        .addAction(settingsAction)
                        .addExtras(extras);
        nm.notify(NOTIFICATION_ID, builder.build());
    }

    private void cancelSharingNotification(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        nm.cancel(NOTIFICATION_ID);
    }
}
