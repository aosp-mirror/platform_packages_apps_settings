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
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;

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
    UsbManagerPassThrough mUsbManagerPassThrough;
    private UsbPort mPort;
    private UsbPortStatus mPortStatus;

    private Context mContext;

    public UsbBackend(Context context) {
        this(context, new UserRestrictionUtil(context), null);
    }

    @VisibleForTesting
    public UsbBackend(Context context, UserRestrictionUtil userRestrictionUtil,
            UsbManagerPassThrough usbManagerPassThrough) {
        mContext = context;
        mUsbManager = context.getSystemService(UsbManager.class);

        mUsbManagerPassThrough = usbManagerPassThrough;
        if (mUsbManagerPassThrough == null) {
            mUsbManagerPassThrough = new UsbManagerPassThrough(mUsbManager);
        }

        mFileTransferRestricted = userRestrictionUtil.isUsbFileTransferRestricted();
        mFileTransferRestrictedBySystem = userRestrictionUtil.isUsbFileTransferRestrictedBySystem();
        mTetheringRestricted = userRestrictionUtil.isUsbTetheringRestricted();
        mTetheringRestrictedBySystem = userRestrictionUtil.isUsbTetheringRestrictedBySystem();

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
        long functions = mUsbManagerPassThrough.getCurrentFunctions();
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

    private void setUsbFunction(int mode) {
        switch (mode) {
            case MODE_DATA_MTP:
                mUsbManager.setCurrentFunctions(UsbManager.FUNCTION_MTP);
                break;
            case MODE_DATA_PTP:
                mUsbManager.setCurrentFunctions(UsbManager.FUNCTION_PTP);
                break;
            case MODE_DATA_MIDI:
                mUsbManager.setCurrentFunctions(UsbManager.FUNCTION_MIDI);
                break;
            case MODE_DATA_TETHER:
                mUsbManager.setCurrentFunctions(UsbManager.FUNCTION_RNDIS);
                break;
            default:
                mUsbManager.setCurrentFunctions(UsbManager.FUNCTION_NONE);
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

    // Wrapper class to enable testing with UserManager APIs
    public static class UserRestrictionUtil {
        private UserManager mUserManager;

        public UserRestrictionUtil(Context context) {
            mUserManager = UserManager.get(context);
        }

        public boolean isUsbFileTransferRestricted() {
            return mUserManager.hasUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER);
        }

        public boolean isUsbTetheringRestricted() {
            return mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_TETHERING);
        }

        public boolean isUsbFileTransferRestrictedBySystem() {
            return mUserManager.hasBaseUserRestriction(
                UserManager.DISALLOW_USB_FILE_TRANSFER, UserHandle.of(UserHandle.myUserId()));
        }

        public boolean isUsbTetheringRestrictedBySystem() {
            return mUserManager.hasBaseUserRestriction(
                UserManager.DISALLOW_CONFIG_TETHERING, UserHandle.of(UserHandle.myUserId()));
        }
    }

    // Temporary pass-through to allow roboelectric to use getCurrentFunctions()
    public static class UsbManagerPassThrough {
        private UsbManager mUsbManager;

        public UsbManagerPassThrough(UsbManager manager) {
            mUsbManager = manager;
        }

        public long getCurrentFunctions() {
            return mUsbManager.getCurrentFunctions();
        }

        public long usbFunctionsFromString(String str) {
            return UsbManager.usbFunctionsFromString(str);
        }
    }
}
