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


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;

/**
 * Receiver to receive usb update and use {@link UsbConnectionListener} to invoke callback
 */
public class UsbConnectionBroadcastReceiver extends BroadcastReceiver {
    private Context mContext;
    private UsbConnectionListener mUsbConnectionListener;
    private boolean mListeningToUsbEvents;
    private boolean mConnected;

    public UsbConnectionBroadcastReceiver(Context context,
            UsbConnectionListener usbConnectionListener) {
        mContext = context;
        mUsbConnectionListener = usbConnectionListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mConnected = intent != null
                && intent.getExtras().getBoolean(UsbManager.USB_CONNECTED);
        if (mUsbConnectionListener != null) {
            mUsbConnectionListener.onUsbConnectionChanged(mConnected);
        }
    }

    public void register() {
        if (!mListeningToUsbEvents) {
            final IntentFilter intentFilter = new IntentFilter(UsbManager.ACTION_USB_STATE);
            final Intent intent = mContext.registerReceiver(this, intentFilter);
            mConnected = intent != null
                    && intent.getExtras().getBoolean(UsbManager.USB_CONNECTED);
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

    /**
     * Interface definition for a callback to be invoked when usb connection is changed.
     */
    interface UsbConnectionListener {
        void onUsbConnectionChanged(boolean connected);
    }
}
