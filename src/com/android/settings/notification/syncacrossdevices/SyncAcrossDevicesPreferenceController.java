/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.syncacrossdevices;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;

public class SyncAcrossDevicesPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, SyncAcrossDevicesFeatureCallback {

    private static final String TAG = "SyncXDevicesPrefCtr";

    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private PreferenceGroup mPreferenceGroup;
    private SyncAcrossDevicesFeatureUpdater mSyncAcrossDevicesFeatureUpdater;

    public SyncAcrossDevicesPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
        SyncAcrossDevicesFeatureProvider syncAcrossDevicesFeatureProvider =
                FeatureFactory.getFeatureFactory().getSyncAcrossDevicesFeatureProvider();
        mSyncAcrossDevicesFeatureUpdater =
                syncAcrossDevicesFeatureProvider.getSyncAcrossDevicesFeatureUpdater(context, this);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(getPreferenceKey());
        mPreferenceGroup.setVisible(false);
        if (isAvailable()) {
            final Context context = screen.getContext();
            mSyncAcrossDevicesFeatureUpdater.setPreferenceContext(context);
            mSyncAcrossDevicesFeatureUpdater.forceUpdate();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mSyncAcrossDevicesFeatureUpdater != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onFeatureAdded(@Nullable Preference preference) {
        if (preference == null) {
            if (DEBUG) {
                Log.d(TAG, "onFeatureAdded receives null preference. Ignore.");
            }
            return;
        }
        mPreferenceGroup.addPreference(preference);
        updatePreferenceVisibility();
    }

    @Override
    public void onFeatureRemoved(@Nullable Preference preference) {
        if (preference == null) {
            if (DEBUG) {
                Log.d(TAG, "onFeatureRemoved receives null preference. Ignore.");
            }
            return;
        }
        mPreferenceGroup.removePreference(preference);
        updatePreferenceVisibility();
    }

    private void updatePreferenceVisibility() {
        mPreferenceGroup.setVisible(mPreferenceGroup.getPreferenceCount() > 0);
    }

    @VisibleForTesting
    public void setPreferenceGroup(@NonNull PreferenceGroup preferenceGroup) {
        mPreferenceGroup = preferenceGroup;
    }
}
