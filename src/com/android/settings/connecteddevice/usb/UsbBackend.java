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
package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.net.ConnectivityManager;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;

import com.android.settings.wrapper.UsbManagerWrapper;
import com.android.settings.wrapper.UserManagerWrapper;

public class UsbBackend {

    public static final int MODE_POWER_MASK  = 0x01;
    public static final int MODE_POWER_SINK   = 0x00;
    public static final int MODE_POWER_SOURCE = 0x01;

    public static final int MODE_DATA_MASK  = 0x0f << 1;
    public static final int MODE_DATA_NONE   = 0;
    public static final int MODE_DATA_MTP    = 0x01 << 1;
    public static final int MODE_DATA_PTP    = 0x01 << 2;
    public static final int MODE_DATA_MIDI   = 0x01 << 3;
    public static final int MODE_DATA_TETHER   = 0x01 << 4;

    private final boolean mFileTransferRestricted;
    private final boolean mFileTransferRestrictedBySystem;
    private final boolean mTetheringRestricted;
    private final boolean mTetheringRestrictedBySystem;
    private final boolean mMidiSupported;
    private final boolean mTetheringSupported;

    private UsbManager mUsbManager;
    @VisibleForTesting
    UsbManagerWrapper mUsbManagerWrapper;
    private UsbPort mPort;
    private UsbPortStatus mPortStatus;

    private Context mContext;

    public UsbBackend(Context context) {
        this(context, new UserManagerWrapper(UserManager.get(context)), null);
    }

    @VisibleForTesting
    public UsbBackend(Context context, UserManagerWrapper userManagerWrapper,
            UsbManagerWrapper usbManagerWrapper) {
        mContext = context;
        mUsbManager = context.getSystemService(UsbManager.class);

        mUsbManagerWrapper = usbManagerWrapper;
        if (mUsbManagerWrapper == null) {
            mUsbManagerWrapper = new UsbManagerWrapper(mUsbManager);
        }

        mFileTransferRestricted = userManagerWrapper.isUsbFileTransferRestricted();
        mFileTransferRestrictedBySystem = userManagerWrapper.isUsbFileTransferRestrictedBySystem();
        mTetheringRestricted = userManagerWrapper.isUsbTetheringRestricted();
        mTetheringRestrictedBySystem = userManagerWrapper.isUsbTetheringRestrictedBySystem();

        mMidiSupported = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI);
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTetheringSupported = cm.isTetheringSupported();

        UsbPort[] ports = mUsbManager.getPorts();
        if (ports == null) {
            return;
        }
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
                    && mPortStatus.isConnected()
                    ? MODE_POWER_SOURCE : MODE_POWER_SINK;
            return power | getUsbDataMode();
        }
        return MODE_POWER_SINK | getUsbDataMode();
    }

    public int getUsbDataMode() {
        return usbFunctionToMode(mUsbManagerWrapper.getCurrentFunctions());
    }

    public void setDefaultUsbMode(int mode) {
        mUsbManager.setScreenUnlockedFunctions(modeToUsbFunction(mode & MODE_DATA_MASK));
    }

    public int getDefaultUsbMode() {
        return usbFunctionToMode(mUsbManager.getScreenUnlockedFunctions());
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

    public boolean isModeDisallowed(int mode) {
        if (mFileTransferRestricted && ((mode & MODE_DATA_MASK) == MODE_DATA_MTP
                || (mode & MODE_DATA_MASK) == MODE_DATA_PTP)) {
            return true;
        } else if (mTetheringRestricted && ((mode & MODE_DATA_MASK) == MODE_DATA_TETHER)) {
            return true;
        }
        return false;
    }

    public boolean isModeDisallowedBySystem(int mode) {
        if (mFileTransferRestrictedBySystem && ((mode & MODE_DATA_MASK) == MODE_DATA_MTP
                || (mode & MODE_DATA_MASK) == MODE_DATA_PTP)) {
            return true;
        } else if (mTetheringRestrictedBySystem && ((mode & MODE_DATA_MASK) == MODE_DATA_TETHER)) {
            return true;
        }
        return false;
    }

    public boolean isModeSupported(int mode) {
        if (!mMidiSupported && (mode & MODE_DATA_MASK) == MODE_DATA_MIDI) {
            return false;
        }
        if (!mTetheringSupported && (mode & MODE_DATA_MASK) == MODE_DATA_TETHER) {
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

    private static int usbFunctionToMode(long functions) {
        if (functions == UsbManager.FUNCTION_MTP) {
            return MODE_DATA_MTP;
        } else if (functions == UsbManager.FUNCTION_PTP) {
            return MODE_DATA_PTP;
        } else if (functions == UsbManager.FUNCTION_MIDI) {
            return MODE_DATA_MIDI;
        } else if (functions == UsbManager.FUNCTION_RNDIS) {
            return MODE_DATA_TETHER;
        }
        return MODE_DATA_NONE;
    }

    private static long modeToUsbFunction(int mode) {
        switch (mode) {
            case MODE_DATA_MTP:
                return UsbManager.FUNCTION_MTP;
            case MODE_DATA_PTP:
                return UsbManager.FUNCTION_PTP;
            case MODE_DATA_MIDI:
                return UsbManager.FUNCTION_MIDI;
            case MODE_DATA_TETHER:
                return UsbManager.FUNCTION_RNDIS;
            default:
                return UsbManager.FUNCTION_NONE;
        }
    }

    private static int modeToPower(int mode) {
        return (mode & MODE_POWER_MASK) == MODE_POWER_SOURCE
                ? UsbPort.POWER_ROLE_SOURCE : UsbPort.POWER_ROLE_SINK;
    }

    private void setUsbFunction(int mode) {
        mUsbManager.setCurrentFunctions(modeToUsbFunction(mode));
    }
}
