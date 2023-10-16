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

package com.android.settings.accessibility;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.core.BasePreferenceController;

/**
 * Abstract base class for bluetooth preference controller to handle UI logic, e.g. availability
 * status, preference added, and preference removed.
 */
public abstract class BaseBluetoothDevicePreferenceController extends BasePreferenceController
        implements DevicePreferenceCallback {

    private PreferenceCategory mPreferenceCategory;

    public BaseBluetoothDevicePreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mPreferenceCategory.setVisible(false);
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        if (mPreferenceCategory.getPreferenceCount() == 0) {
            mPreferenceCategory.setVisible(true);
        }
        mPreferenceCategory.addPreference(preference);
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mPreferenceCategory.removePreference(preference);
        if (mPreferenceCategory.getPreferenceCount() == 0) {
            mPreferenceCategory.setVisible(false);
        }
    }
}
