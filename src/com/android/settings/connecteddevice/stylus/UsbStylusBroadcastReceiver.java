/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.stylus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

/** Broadcast receiver for styluses connected via USB **/
public class UsbStylusBroadcastReceiver extends BroadcastReceiver {
    private Context mContext;
    private UsbStylusConnectionListener mUsbConnectionListener;
    private boolean mListeningToUsbEvents;

    public UsbStylusBroadcastReceiver(Context context,
            UsbStylusConnectionListener usbConnectionListener) {
        mContext = context;
        mUsbConnectionListener = usbConnectionListener;
    }

    /** Registers the receiver. **/
    public void register() {
        if (!mListeningToUsbEvents) {
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            intentFilter.addAction(UsbManager.ACTION_USB_STATE);
            final Intent intent = mContext.registerReceiver(this, intentFilter);
            if (intent != null) {
                onReceive(mContext, intent);
            }
            mListeningToUsbEvents = true;
        }
    }

    /** Unregisters the receiver. **/
    public void unregister() {
        if (mListeningToUsbEvents) {
            mContext.unregisterReceiver(this);
            mListeningToUsbEvents = false;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
        if (StylusUsbFirmwareController.hasUsbStylusFirmwareUpdateFeature(usbDevice)) {
            mUsbConnectionListener.onUsbStylusConnectionChanged(usbDevice,
                    intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        }
    }

    /**
     * Interface definition for a callback to be invoked when stylus usb connection is changed.
     */
    interface UsbStylusConnectionListener {
        void onUsbStylusConnectionChanged(UsbDevice device, boolean connected);
    }
}
