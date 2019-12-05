/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;

/**
 * BluetoothPairingService shows a notification if there is a pending bond request
 * which can launch the appropriate pairing dialog when tapped.
 */
public final class BluetoothPairingService extends Service {

    private static final int NOTIFICATION_ID = android.R.drawable.stat_sys_data_bluetooth;

    private static final String ACTION_DISMISS_PAIRING =
            "com.android.settings.bluetooth.ACTION_DISMISS_PAIRING";

    private static final String BLUETOOTH_NOTIFICATION_CHANNEL =
            "bluetooth_notification_channel";

    private static final String TAG = "BluetoothPairingService";

    private BluetoothDevice mDevice;

    public static Intent getPairingDialogIntent(Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
                BluetoothDevice.ERROR);
        Intent pairingIntent = new Intent();
        pairingIntent.setClass(context, BluetoothPairingDialog.class);
        pairingIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        pairingIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, type);
        if (type == BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION ||
                type == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY ||
                type == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN) {
            int pairingKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY,
                    BluetoothDevice.ERROR);
            pairingIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_KEY, pairingKey);
        }
        pairingIntent.setAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        pairingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return pairingIntent;
    }

    private boolean mRegistered = false;
    private final BroadcastReceiver mCancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR);
                if ((bondState != BluetoothDevice.BOND_NONE) && (bondState != BluetoothDevice.BOND_BONDED)) {
                    return;
                }
            } else if (action.equals(ACTION_DISMISS_PAIRING)) {
                Log.d(TAG, "Notification cancel " + mDevice.getAddress() + " (" +
                        mDevice.getName() + ")");
                mDevice.cancelPairing();
            } else {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR);
                Log.d(TAG, "Dismiss pairing for " + mDevice.getAddress() + " (" +
                        mDevice.getName() + "), BondState: " + bondState);
            }
            stopForeground(true);
            stopSelf();
        }
    };

    @Override
    public void onCreate() {
      NotificationManager mgr = (NotificationManager)this
         .getSystemService(Context.NOTIFICATION_SERVICE);
      NotificationChannel notificationChannel = new NotificationChannel(
         BLUETOOTH_NOTIFICATION_CHANNEL,
         this.getString(R.string.bluetooth),
         NotificationManager.IMPORTANCE_HIGH);
      mgr.createNotificationChannel(notificationChannel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e(TAG, "Can't start: null intent!");
            stopSelf();
            return START_NOT_STICKY;
        }

        Resources res = getResources();
        Notification.Builder builder = new Notification.Builder(this,
            BLUETOOTH_NOTIFICATION_CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setTicker(res.getString(R.string.bluetooth_notif_ticker))
                .setLocalOnly(true);

        PendingIntent pairIntent = PendingIntent.getActivity(this, 0,
                getPairingDialogIntent(this, intent),
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent dismissIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_DISMISS_PAIRING), PendingIntent.FLAG_ONE_SHOT);

        mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (mDevice != null && mDevice.getBondState() != BluetoothDevice.BOND_BONDING) {
            Log.w(TAG, "Device " + mDevice + " not bonding: " + mDevice.getBondState());
            stopSelf();
            return START_NOT_STICKY;
        }

        String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
        if (TextUtils.isEmpty(name)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            name = device != null ? device.getAlias() : res.getString(android.R.string.unknownName);
        }

        Log.d(TAG, "Show pairing notification for " + mDevice.getAddress() + " (" + name + ")");

        Notification.Action pairAction = new Notification.Action.Builder(0,
                res.getString(R.string.bluetooth_device_context_pair_connect), pairIntent).build();
        Notification.Action dismissAction = new Notification.Action.Builder(0,
                res.getString(android.R.string.cancel), dismissIntent).build();

        builder.setContentTitle(res.getString(R.string.bluetooth_notif_title))
                .setContentText(res.getString(R.string.bluetooth_notif_message, name))
                .setContentIntent(pairIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setColor(getColor(com.android.internal.R.color.system_notification_accent_color))
                .addAction(pairAction)
                .addAction(dismissAction);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_CANCEL);
        filter.addAction(ACTION_DISMISS_PAIRING);
        registerReceiver(mCancelReceiver, filter);
        mRegistered = true;

        startForeground(NOTIFICATION_ID, builder.getNotification());
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        if (mRegistered) {
            unregisterReceiver(mCancelReceiver);
            mRegistered = false;
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // No binding.
        return null;
    }
}
