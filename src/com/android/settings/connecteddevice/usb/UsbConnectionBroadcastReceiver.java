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
import android.hardware.usb.UsbPortStatus;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * Receiver to receive usb update and use {@link UsbConnectionListener} to invoke callback
 */
public class UsbConnectionBroadcastReceiver extends BroadcastReceiver implements LifecycleObserver,
        OnResume, OnPause {
    private Context mContext;
    private UsbConnectionListener mUsbConnectionListener;
    private boolean mListeningToUsbEvents;
    private UsbBackend mUsbBackend;

    private boolean mConnected;
    private long mFunctions;
    private int mDataRole;
    private int mPowerRole;

    public UsbConnectionBroadcastReceiver(Context context,
            UsbConnectionListener usbConnectionListener, UsbBackend backend) {
        mContext = context;
        mUsbConnectionListener = usbConnectionListener;
        mUsbBackend = backend;

        mFunctions = UsbManager.FUNCTION_NONE;
        mDataRole = UsbPortStatus.DATA_ROLE_NONE;
        mPowerRole = UsbPortStatus.POWER_ROLE_NONE;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (UsbManager.ACTION_USB_STATE.equals(intent.getAction())) {
            mConnected = intent.getExtras().getBoolean(UsbManager.USB_CONNECTED)
                    || intent.getExtras().getBoolean(UsbManager.USB_HOST_CONNECTED);
            long functions = UsbManager.FUNCTION_NONE;
            if (intent.getExtras().getBoolean(UsbManager.USB_FUNCTION_MTP)
                    && intent.getExtras().getBoolean(UsbManager.USB_DATA_UNLOCKED, false)) {
                functions |= UsbManager.FUNCTION_MTP;
            }
            if (intent.getExtras().getBoolean(UsbManager.USB_FUNCTION_PTP)
                    && intent.getExtras().getBoolean(UsbManager.USB_DATA_UNLOCKED, false)) {
                functions |= UsbManager.FUNCTION_PTP;
            }
            if (intent.getExtras().getBoolean(UsbManager.USB_FUNCTION_MIDI)) {
                functions |= UsbManager.FUNCTION_MIDI;
            }
            if (intent.getExtras().getBoolean(UsbManager.USB_FUNCTION_RNDIS)) {
                functions |= UsbManager.FUNCTION_RNDIS;
            }
            if (intent.getExtras().getBoolean(UsbManager.USB_FUNCTION_ACCESSORY)) {
                functions |= UsbManager.FUNCTION_ACCESSORY;
            }
            mFunctions = functions;
            mDataRole = mUsbBackend.getDataRole();
            mPowerRole = mUsbBackend.getPowerRole();
        } else if (UsbManager.ACTION_USB_PORT_CHANGED.equals(intent.getAction())) {
            UsbPortStatus portStatus = intent.getExtras()
                    .getParcelable(UsbManager.EXTRA_PORT_STATUS);
            if (portStatus != null) {
                mDataRole = portStatus.getCurrentDataRole();
                mPowerRole = portStatus.getCurrentPowerRole();
            }
        }
        if (mUsbConnectionListener != null) {
            mUsbConnectionListener.onUsbConnectionChanged(mConnected, mFunctions, mPowerRole,
                    mDataRole);
        }
    }

    public void register() {
        if (!mListeningToUsbEvents) {
            mConnected = false;
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(UsbManager.ACTION_USB_STATE);
            intentFilter.addAction(UsbManager.ACTION_USB_PORT_CHANGED);
            final Intent intent = mContext.registerReceiver(this, intentFilter);
            // TODO b/77240599 use an api instead of sticky intent
            if (intent != null) {
                onReceive(mContext, intent);
            }
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
        void onUsbConnectionChanged(boolean connected, long functions, int powerRole, int dataRole);
    }
}
