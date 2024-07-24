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

package com.android.settings.devicelock;

import android.content.Context;
import android.devicelock.DeviceLockManager;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

/**
 * The PreferenceController that manages the Device Lock entry preference.
 */
public final class DeviceLockPreferenceController extends BasePreferenceController {

    private static final String TAG = "DeviceLockPreferenceController";

    private final DeviceLockManager mDeviceLockManager;

    public DeviceLockPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mDeviceLockManager = mContext.getSystemService(DeviceLockManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        // TODO(b/282856378): make this entry searchable
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mDeviceLockManager == null) {
            Log.w(TAG, "DeviceLockManager is not available");
            preference.setVisible(false);
            return;
        }
        mDeviceLockManager.getKioskApps(mContext.getMainExecutor(),
                result -> {
                    // if kiosk apps present on the device, the device is provisioned by Device Lock
                    boolean isDeviceProvisionedByDeviceLock = result != null && !result.isEmpty();
                    Log.d(TAG, "Set preference visibility to " + isDeviceProvisionedByDeviceLock);
                    // TODO(b/282179089): find alternatives instead of calling setVisible
                    preference.setVisible(isDeviceProvisionedByDeviceLock);
                });
    }
}
