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
package com.android.settings.connecteddevice;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.deviceinfo.UsbBackend;
import com.android.settings.deviceinfo.UsbModeChooserActivity;
import com.android.settings.widget.GearPreference;

/**
 * Controller to maintain connected usb device
 */
public class ConnectedUsbDeviceUpdater {
    private Context mContext;
    private UsbBackend mUsbBackend;
    private DevicePreferenceCallback mDevicePreferenceCallback;
    @VisibleForTesting
    GearPreference mUsbPreference;
    @VisibleForTesting
    UsbConnectionBroadcastReceiver mUsbReceiver;

    private UsbConnectionBroadcastReceiver.UsbConnectionListener mUsbConnectionListener =
            (connected) -> {
                if (connected) {
                    mUsbPreference.setSummary(
                            UsbModePreferenceController.getSummary(mUsbBackend.getCurrentMode()));
                    mDevicePreferenceCallback.onDeviceAdded(mUsbPreference);
                } else {
                    mDevicePreferenceCallback.onDeviceRemoved(mUsbPreference);
                }
            };

    public ConnectedUsbDeviceUpdater(Context context,
            DevicePreferenceCallback devicePreferenceCallback) {
        this(context, devicePreferenceCallback, new UsbBackend(context));
    }

    @VisibleForTesting
    ConnectedUsbDeviceUpdater(Context context, DevicePreferenceCallback devicePreferenceCallback,
            UsbBackend usbBackend) {
        mContext = context;
        mDevicePreferenceCallback = devicePreferenceCallback;
        mUsbBackend = usbBackend;
        mUsbReceiver = new UsbConnectionBroadcastReceiver(context, mUsbConnectionListener);
    }

    public void registerCallback() {
        // This method could handle multiple register
        mUsbReceiver.register();
    }

    public void unregisterCallback() {
        mUsbReceiver.unregister();
    }

    public void initUsbPreference(Context context) {
        mUsbPreference = new GearPreference(context, null /* AttributeSet */);
        mUsbPreference.setTitle(R.string.usb_pref);
        mUsbPreference.setIcon(R.drawable.ic_usb);
        mUsbPreference.setSelectable(false);
        mUsbPreference.setOnGearClickListener((GearPreference p) -> {
            final Intent intent = new Intent(mContext, UsbModeChooserActivity.class);
            mContext.startActivity(intent);
        });

        forceUpdate();
    }

    private void forceUpdate() {
        // Register so we can get the connection state from sticky intent.
        //TODO(b/70336520): Use an API to get data instead of sticky intent
        mUsbReceiver.register();
        mUsbConnectionListener.onUsbConnectionChanged(mUsbReceiver.isConnected());
    }
}
