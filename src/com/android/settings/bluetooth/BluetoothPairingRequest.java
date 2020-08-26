/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.UserHandle;

/**
 * BluetoothPairingRequest is a receiver for any Bluetooth pairing request. It
 * checks if the Bluetooth Settings is currently visible and brings up the PIN, the passkey or a
 * confirmation entry dialog. Otherwise it starts the BluetoothPairingService which
 * starts a notification in the status bar that can be clicked to bring up the same dialog.
 */
public final class BluetoothPairingRequest extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (action == null || !action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
      return;
    }
    // convert broadcast intent into activity intent (same action string)
    Intent pairingIntent = BluetoothPairingService.getPairingDialogIntent(context, intent);

    PowerManager powerManager =
        (PowerManager)context.getSystemService(Context.POWER_SERVICE);
    BluetoothDevice device =
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    String deviceAddress = device != null ? device.getAddress() : null;
    String deviceName = device != null ? device.getName() : null;
    boolean shouldShowDialog = LocalBluetoothPreferences.shouldShowDialogInForeground(
        context, deviceAddress, deviceName);
    if (powerManager.isInteractive() && shouldShowDialog) {
      // Since the screen is on and the BT-related activity is in the foreground,
      // just open the dialog
      context.startActivityAsUser(pairingIntent, UserHandle.CURRENT);
    } else {
      // Put up a notification that leads to the dialog
      intent.setClass(context, BluetoothPairingService.class);
      context.startServiceAsUser(intent, UserHandle.CURRENT);
    }
  }
}
