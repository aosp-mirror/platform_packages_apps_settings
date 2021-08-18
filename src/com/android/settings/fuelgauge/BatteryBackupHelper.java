/*
 * Copyright (C) 2021 The Android Open Source Project
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
 *
 */

package com.android.settings.fuelgauge;

import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.os.IDeviceIdleController;
import android.os.RemoteException;
import android.os.ParcelFileDescriptor;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.util.Arrays;

/** An implementation to backup and restore battery configurations. */
public final class BatteryBackupHelper implements BackupHelper {
    /** An inditifier for {@link BackupHelper}. */
    public static final String TAG = "BatteryBackupHelper";
    private static final String DEVICE_IDLE_SERVICE = "deviceidle";
    private static final boolean DEBUG = false;

    @VisibleForTesting
    static final CharSequence DELIMITER = ":";
    @VisibleForTesting
    static final String KEY_FULL_POWER_LIST = "full_power_list";

    @VisibleForTesting
    IDeviceIdleController mIDeviceIdleController;

    private final Context mContext;

    public BatteryBackupHelper(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) {
        if (!isOwner()) {
            Log.w(TAG, "ignore the backup process for non-owner");
            return;
        }
        backupFullPowerList(getIDeviceIdleController(), data);
    }

    @Override
    public void restoreEntity(BackupDataInputStream data) {
        Log.d(TAG, "restoreEntity()");
    }

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor newState) {
    }

    private void backupFullPowerList(
            IDeviceIdleController deviceIdleService, BackupDataOutput data) {
        final long timestamp = System.currentTimeMillis();
        String[] allowlistedApps;
        try {
            allowlistedApps = deviceIdleService.getFullPowerWhitelist();
        } catch (RemoteException e) {
            Log.e(TAG, "backupFullPowerList() failed", e);
            return;
        }
        // Ignores unexpected emptty result case.
        if (allowlistedApps == null || allowlistedApps.length == 0) {
            Log.w(TAG, "no data found in the getFullPowerList()");
            return;
        }
        debugLog("allowlistedApps:" + Arrays.toString(allowlistedApps));
        final String allowedApps = String.join(DELIMITER, allowlistedApps);
        final byte[] allowedAppsBytes = allowedApps.getBytes();
        try {
            data.writeEntityHeader(KEY_FULL_POWER_LIST, allowedAppsBytes.length);
            data.writeEntityData(allowedAppsBytes, allowedAppsBytes.length);
        } catch (IOException e) {
            Log.e(TAG, "backup getFullPowerList() failed", e);
            return;
        }
        Log.d(TAG, String.format("backup getFullPowerList() size=%d in %d/ms",
                allowlistedApps.length, (System.currentTimeMillis() - timestamp)));
    }

    // Provides an opportunity to inject mock IDeviceIdleController for testing.
    private IDeviceIdleController getIDeviceIdleController() {
        if (mIDeviceIdleController != null) {
            return mIDeviceIdleController;
        }
        mIDeviceIdleController = IDeviceIdleController.Stub.asInterface(
                ServiceManager.getService(DEVICE_IDLE_SERVICE));
        return mIDeviceIdleController;
    }

    private void debugLog(String debugContent) {
        if (DEBUG) Log.d(TAG, debugContent);
    }

    private static boolean isOwner() {
        return UserHandle.myUserId() == UserHandle.USER_OWNER;
    }
}
