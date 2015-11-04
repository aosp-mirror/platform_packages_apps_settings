/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.os.UserManager;

public class UsbBackend {

    private static final int MODE_POWER_MASK  = 0x01;
    public static final int MODE_POWER_SINK   = 0x00;
    public static final int MODE_POWER_SOURCE = 0x01;

    private static final int MODE_DATA_MASK  = 0x03 << 1;
    public static final int MODE_DATA_NONE   = 0x00 << 1;
    public static final int MODE_DATA_MTP    = 0x01 << 1;
    public static final int MODE_DATA_PTP    = 0x02 << 1;
    public static final int MODE_DATA_MIDI   = 0x03 << 1;

    private final boolean mRestricted;
    private final boolean mMidi;

    private UserManager mUserManager;
    private UsbManager mUsbManager;
    private UsbPort mPort;
    private UsbPortStatus mPortStatus;

    private boolean mIsUnlocked;

    public UsbBackend(Context context) {
        Intent intent = context.registerReceiver(null,
                new IntentFilter(UsbManager.ACTION_USB_STATE));
        mIsUnlocked = intent.getBooleanExtra(UsbManager.USB_DATA_UNLOCKED, false);

        mUserManager = UserManager.get(context);
        mUsbManager = context.getSystemService(UsbManager.class);

        mRestricted = mUserManager.hasUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER);
        mMidi = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI);

        UsbPort[] ports = mUsbManager.getPorts();
        // For now look for a connected port, in the future we should identify port in the
        // notification and pick based on that.
        final int N = ports.length;
        for (int i = 0; i < N; i++) {
            UsbPortStatus status = mUsbManager.getPortStatus(ports[i]);
            if (status.isConnected()) {
                mPort = ports[i];
                mPortStatus = status;
                break;
            }
        }
    }

    public int getCurrentMode() {
        if (mPort != null) {
            int power = mPortStatus.getCurrentPowerRole() == UsbPort.POWER_ROLE_SOURCE
                    ? MODE_POWER_SOURCE : MODE_POWER_SINK;
            return power | getUsbDataMode();
        }
        return MODE_POWER_SINK | getUsbDataMode();
    }

    public int getUsbDataMode() {
        if (!mIsUnlocked) {
            return MODE_DATA_NONE;
        } else if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_MTP)) {
            return MODE_DATA_MTP;
        } else if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_PTP)) {
            return MODE_DATA_PTP;
        } else if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_MIDI)) {
            return MODE_DATA_MIDI;
        }
        return MODE_DATA_NONE; // ...
    }

    private void setUsbFunction(int mode) {
        switch (mode) {
            case MODE_DATA_MTP:
                mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MTP);
                mUsbManager.setUsbDataUnlocked(true);
                break;
            case MODE_DATA_PTP:
                mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_PTP);
                mUsbManager.setUsbDataUnlocked(true);
                break;
            case MODE_DATA_MIDI:
                mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MIDI);
                mUsbManager.setUsbDataUnlocked(true);
                break;
            default:
                mUsbManager.setCurrentFunction(null);
                mUsbManager.setUsbDataUnlocked(false);
                break;
        }
    }

    public void setMode(int mode) {
        if (mPort != null) {
            int powerRole = modeToPower(mode);
            // If we aren't using any data modes and we support host mode, then go to host mode
            // so maybe? the other device can provide data if it wants, otherwise go into device
            // mode because we have no choice.
            int dataRole = (mode & MODE_DATA_MASK) == MODE_DATA_NONE
                    && mPortStatus.isRoleCombinationSupported(powerRole, UsbPort.DATA_ROLE_HOST)
                    ? UsbPort.DATA_ROLE_HOST : UsbPort.DATA_ROLE_DEVICE;
            mUsbManager.setPortRoles(mPort, powerRole, dataRole);
        }
        setUsbFunction(mode & MODE_DATA_MASK);
    }

    private int modeToPower(int mode) {
        return (mode & MODE_POWER_MASK) == MODE_POWER_SOURCE
                    ? UsbPort.POWER_ROLE_SOURCE : UsbPort.POWER_ROLE_SINK;
    }

    public boolean isModeSupported(int mode) {
        if (mRestricted && (mode & MODE_DATA_MASK) != MODE_DATA_NONE
                && (mode & MODE_DATA_MASK) != MODE_DATA_MIDI) {
            // No USB data modes are supported.
            return false;
        }

        if (!mMidi && (mode & MODE_DATA_MASK) == MODE_DATA_MIDI) {
            return false;
        }

        if (mPort != null) {
            int power = modeToPower(mode);
            if ((mode & MODE_DATA_MASK) != 0) {
                // We have a port and data, need to be in device mode.
                return mPortStatus.isRoleCombinationSupported(power,
                        UsbPort.DATA_ROLE_DEVICE);
            } else {
                // No data needed, we can do this power mode in either device or host.
                return mPortStatus.isRoleCombinationSupported(power, UsbPort.DATA_ROLE_DEVICE)
                        || mPortStatus.isRoleCombinationSupported(power, UsbPort.DATA_ROLE_HOST);
            }
        }
        // No port, support sink modes only.
        return (mode & MODE_POWER_MASK) != MODE_POWER_SOURCE;
    }
}