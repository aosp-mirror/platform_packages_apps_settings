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
package com.android.settings.connecteddevice.usb;



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnPause;

/**
 * Receiver to receive usb update and use {@link UsbConnectionListener} to invoke callback
 */
public class UsbConnectionBroadcastReceiver extends BroadcastReceiver implements LifecycleObserver,
        OnResume, OnPause {
    private Context mContext;
    private UsbConnectionListener mUsbConnectionListener;
    private boolean mListeningToUsbEvents;
    private int mMode;
    private boolean mConnected;
    private UsbBackend mUsbBackend;

    public UsbConnectionBroadcastReceiver(Context context,
            UsbConnectionListener usbConnectionListener, UsbBackend backend) {
        mContext = context;
        mUsbConnectionListener = usbConnectionListener;
        mUsbBackend = backend;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (UsbManager.ACTION_USB_STATE.equals(intent.getAction())) {
            mConnected = intent.getExtras().getBoolean(UsbManager.USB_CONNECTED)
                    || intent.getExtras().getBoolean(UsbManager.USB_HOST_CONNECTED);
            if (mConnected) {
                mMode &= UsbBackend.MODE_POWER_MASK;
                if (intent.getExtras().getBoolean(UsbManager.USB_FUNCTION_MTP)
                        && intent.getExtras().getBoolean(UsbManager.USB_DATA_UNLOCKED, false)) {
                    mMode |= UsbBackend.MODE_DATA_MTP;
                }
                if (intent.getExtras().getBoolean(UsbManager.USB_FUNCTION_PTP)
                        && intent.getExtras().getBoolean(UsbManager.USB_DATA_UNLOCKED, false)) {
                    mMode |= UsbBackend.MODE_DATA_PTP;
                }
                if (intent.getExtras().getBoolean(UsbManager.USB_FUNCTION_MIDI)) {
                    mMode |= UsbBackend.MODE_DATA_MIDI;
                }
                if (intent.getExtras().getBoolean(UsbManager.USB_FUNCTION_RNDIS)) {
                    mMode |= UsbBackend.MODE_DATA_TETHER;
                }
            }
        } else if (UsbManager.ACTION_USB_PORT_CHANGED.equals(intent.getAction())) {
            mMode &= UsbBackend.MODE_DATA_MASK;
            UsbPortStatus portStatus = intent.getExtras()
                    .getParcelable(UsbManager.EXTRA_PORT_STATUS);
            if (portStatus != null) {
                mConnected = portStatus.isConnected();
                if (mConnected) {
                    mMode |= portStatus.getCurrentPowerRole() == UsbPort.POWER_ROLE_SOURCE
                            ? UsbBackend.MODE_POWER_SOURCE : UsbBackend.MODE_POWER_SINK;
                }
            }
        }
        if (mUsbConnectionListener != null) {
            mUsbConnectionListener.onUsbConnectionChanged(mConnected, mMode);
        }
    }

    public void register() {
        if (!mListeningToUsbEvents) {
            mMode = mUsbBackend.getCurrentMode();
            mConnected = false;
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(UsbManager.ACTION_USB_STATE);
            intentFilter.addAction(UsbManager.ACTION_USB_PORT_CHANGED);
            mContext.registerReceiver(this, intentFilter);
            mListeningToUsbEvents = true;
        }
    }

    public void unregister() {
        if (mListeningToUsbEvents) {
            mContext.unregisterReceiver(this);
            mListeningToUsbEvents = false;
        }
    }

    public boolean isConnected() {
        return mConnected;
    }

    @Override
    public void onResume() {
        register();
    }

    @Override
    public void onPause() {
        unregister();
    }

    /**
     * Interface definition for a callback to be invoked when usb connection is changed.
     */
    interface UsbConnectionListener {
        void onUsbConnectionChanged(boolean connected, int newMode);
    }
}
