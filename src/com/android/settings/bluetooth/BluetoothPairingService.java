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
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

  private static final String TAG = "BluetoothPairingService";

  private BluetoothDevice mDevice;

  public static Intent getPairingDialogIntent(Context context, Intent intent) {

    BluetoothDevice device =
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
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
        Log.d(TAG, "Dismiss pairing for " + mDevice.getAddress() + " (" + mDevice.getName() + "), BondState: " + bondState);
      } else {
        Log.d(TAG, "Dismiss pairing for " + mDevice.getAddress() + " (" + mDevice.getName() + "), Cancelled.");
      }
      stopForeground(true);
    }
  };

  @Override
  public void onCreate() {
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Resources res = getResources();
    Notification.Builder builder = new Notification.Builder(this)
        .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
        .setTicker(res.getString(R.string.bluetooth_notif_ticker));

    PendingIntent pending = PendingIntent.getActivity(this, 0,
        getPairingDialogIntent(this, intent), PendingIntent.FLAG_ONE_SHOT);

    mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

    String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
    if (TextUtils.isEmpty(name)) {
      BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
      name = device != null ? device.getAliasName() : getString(android.R.string.unknownName);
    }

    Log.d(TAG, "Show pairing notification for " + mDevice.getAddress() + " (" + name + ")");

    builder.setContentTitle(res.getString(R.string.bluetooth_notif_title))
        .setContentText(res.getString(R.string.bluetooth_notif_message, name))
        .setContentIntent(pending)
        .setDefaults(Notification.DEFAULT_SOUND)
        .setColor(getColor(com.android.internal.R.color.system_notification_accent_color));

    IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    filter.addAction(BluetoothDevice.ACTION_PAIRING_CANCEL);
    registerReceiver(mCancelReceiver, filter);

    startForeground(NOTIFICATION_ID, builder.getNotification());
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    unregisterReceiver(mCancelReceiver);
    stopForeground(true);
  }

  @Override
  public IBinder onBind(Intent intent) {
    // No binding.
    return null;
  }

}
