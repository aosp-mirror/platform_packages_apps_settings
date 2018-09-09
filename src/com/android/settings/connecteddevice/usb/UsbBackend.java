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

import static android.hardware.usb.UsbPortStatus.DATA_ROLE_DEVICE;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_NONE;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_SOURCE;
import static android.service.usb.UsbPortStatusProto.DATA_ROLE_HOST;
import static android.service.usb.UsbPortStatusProto.DATA_ROLE_NONE;
import static android.service.usb.UsbPortStatusProto.POWER_ROLE_SINK;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.net.TetheringManager;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;

import java.util.List;

/**
 * Provides access to underlying system USB functionality.
 */
public class UsbBackend {

    // extend this value from 3s to 4s because of switching data role
    // in USB driver side takes about 3s in some devices, plus the usb
    // port change event dispatching time, 3s is not enough.
    static final int PD_ROLE_SWAP_TIMEOUT_MS = 4000;
    static final int NONPD_ROLE_SWAP_TIMEOUT_MS = 15000;

    private final boolean mFileTransferRestricted;
    private final boolean mFileTransferRestrictedBySystem;
    private final boolean mTetheringRestricted;
    private final boolean mTetheringRestrictedBySystem;
    private final boolean mMidiSupported;
    private final boolean mTetheringSupported;
    private final boolean mIsAdminUser;

    private UsbManager mUsbManager;

    @Nullable
    private UsbPort mPort;
    @Nullable
    private UsbPortStatus mPortStatus;

    public UsbBackend(Context context) {
        this(context, (UserManager) context.getSystemService(Context.USER_SERVICE));
    }

    @VisibleForTesting
    public UsbBackend(Context context, UserManager userManager) {
        mUsbManager = context.getSystemService(UsbManager.class);

        mFileTransferRestricted = isUsbFileTransferRestricted(userManager);
        mFileTransferRestrictedBySystem = isUsbFileTransferRestrictedBySystem(userManager);
        mTetheringRestricted = isUsbTetheringRestricted(userManager);
        mTetheringRestrictedBySystem = isUsbTetheringRestrictedBySystem(userManager);
        mIsAdminUser = userManager.isAdminUser();

        mMidiSupported = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI);
        final TetheringManager tm = context.getSystemService(TetheringManager.class);
        mTetheringSupported = tm.isTetheringSupported();

        updatePorts();
    }

    public long getCurrentFunctions() {
        return mUsbManager.getCurrentFunctions();
    }

    public void setCurrentFunctions(long functions) {
        mUsbManager.setCurrentFunctions(functions);
    }

    public long getDefaultUsbFunctions() {
        return mUsbManager.getScreenUnlockedFunctions();
    }

    public void setDefaultUsbFunctions(long functions) {
        mUsbManager.setScreenUnlockedFunctions(functions);
    }

    public boolean areFunctionsSupported(long functions) {
        if ((!mMidiSupported && (functions & UsbManager.FUNCTION_MIDI) != 0)
                || (!mTetheringSupported && (functions & UsbManager.FUNCTION_RNDIS) != 0)) {
            return false;
        }
        return !(areFunctionDisallowed(functions) || areFunctionsDisallowedBySystem(functions)
                || areFunctionsDisallowedByNonAdminUser(functions));
    }

    public int getPowerRole() {
        updatePorts();
        return mPortStatus == null ? POWER_ROLE_NONE : mPortStatus.getCurrentPowerRole();
    }

    public int getDataRole() {
        updatePorts();
        return mPortStatus == null ? DATA_ROLE_NONE : mPortStatus.getCurrentDataRole();
    }

    public void setPowerRole(int role) {
        int newDataRole = getDataRole();
        if (!areAllRolesSupported()) {
            switch (role) {
                case POWER_ROLE_SINK:
                    newDataRole = DATA_ROLE_DEVICE;
                    break;
                case POWER_ROLE_SOURCE:
                    newDataRole = DATA_ROLE_HOST;
                    break;
                default:
                    newDataRole = DATA_ROLE_NONE;
            }
        }
        if (mPort != null) {
            mPort.setRoles(role, newDataRole);
        }
    }

    public void setDataRole(int role) {
        int newPowerRole = getPowerRole();
        if (!areAllRolesSupported()) {
            switch (role) {
                case DATA_ROLE_DEVICE:
                    newPowerRole = POWER_ROLE_SINK;
                    break;
                case DATA_ROLE_HOST:
                    newPowerRole = POWER_ROLE_SOURCE;
                    break;
                default:
                    newPowerRole = POWER_ROLE_NONE;
            }
        }
        if (mPort != null) {
            mPort.setRoles(newPowerRole, role);
        }
    }

    public boolean areAllRolesSupported() {
        return mPort != null && mPortStatus != null
                && mPortStatus.isRoleCombinationSupported(POWER_ROLE_SINK, DATA_ROLE_DEVICE)
                && mPortStatus.isRoleCombinationSupported(POWER_ROLE_SINK, DATA_ROLE_HOST)
                && mPortStatus.isRoleCombinationSupported(POWER_ROLE_SOURCE, DATA_ROLE_DEVICE)
                && mPortStatus.isRoleCombinationSupported(POWER_ROLE_SOURCE, DATA_ROLE_HOST);
    }

    public boolean isSingleDataRoleSupported() {
        return mPort != null && mPortStatus != null
                && ((!mPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SINK, DATA_ROLE_HOST)
                && !mPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SOURCE, DATA_ROLE_HOST))
                || (!mPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SINK, DATA_ROLE_DEVICE)
                && !mPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SOURCE, DATA_ROLE_DEVICE)));
    }

    public boolean isSinglePowerRoleSupported() {
        return mPort != null && mPortStatus != null
                && ((!mPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SINK, DATA_ROLE_DEVICE)
                && !mPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SINK, DATA_ROLE_HOST))
                || (!mPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SOURCE, DATA_ROLE_DEVICE)
                && !mPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SOURCE, DATA_ROLE_HOST)));
    }

    public static String usbFunctionsToString(long functions) {
        // TODO replace with UsbManager.usbFunctionsToString once supported by Roboelectric
        return Long.toBinaryString(functions);
    }

    public static long usbFunctionsFromString(String functions) {
        // TODO replace with UsbManager.usbFunctionsFromString once supported by Roboelectric
        return Long.parseLong(functions, 2);
    }

    public static String dataRoleToString(int role) {
        return Integer.toString(role);
    }

    public static int dataRoleFromString(String role) {
        return Integer.parseInt(role);
    }

    private static boolean isUsbFileTransferRestricted(UserManager userManager) {
        return userManager.hasUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER);
    }

    private static boolean isUsbTetheringRestricted(UserManager userManager) {
        return userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_TETHERING);
    }

    private static boolean isUsbFileTransferRestrictedBySystem(UserManager userManager) {
        return userManager.hasBaseUserRestriction(
                UserManager.DISALLOW_USB_FILE_TRANSFER, UserHandle.of(UserHandle.myUserId()));
    }

    private static boolean isUsbTetheringRestrictedBySystem(UserManager userManager) {
        return userManager.hasBaseUserRestriction(
                UserManager.DISALLOW_CONFIG_TETHERING, UserHandle.of(UserHandle.myUserId()));
    }

    private boolean areFunctionDisallowed(long functions) {
        return (mFileTransferRestricted && ((functions & UsbManager.FUNCTION_MTP) != 0
                || (functions & UsbManager.FUNCTION_PTP) != 0))
                || (mTetheringRestricted && ((functions & UsbManager.FUNCTION_RNDIS) != 0));
    }

    private boolean areFunctionsDisallowedBySystem(long functions) {
        return (mFileTransferRestrictedBySystem && ((functions & UsbManager.FUNCTION_MTP) != 0
                || (functions & UsbManager.FUNCTION_PTP) != 0))
                || (mTetheringRestrictedBySystem && ((functions & UsbManager.FUNCTION_RNDIS) != 0));
    }

    @VisibleForTesting
    boolean areFunctionsDisallowedByNonAdminUser(long functions) {
        return !mIsAdminUser && (functions & UsbManager.FUNCTION_RNDIS) != 0;
    }

    private void updatePorts() {
        mPort = null;
        mPortStatus = null;
        List<UsbPort> ports = mUsbManager.getPorts();
        // For now look for a connected port, in the future we should identify port in the
        // notification and pick based on that.
        final int N = ports.size();
        for (int i = 0; i < N; i++) {
            UsbPortStatus status = ports.get(i).getStatus();
            if (status.isConnected()) {
                mPort = ports.get(i);
                mPortStatus = status;
                break;
            }
        }
    }
}
