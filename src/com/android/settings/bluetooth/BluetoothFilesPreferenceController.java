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

package com.android.settings.bluetooth;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Controller that shows received files
 */
public class BluetoothFilesPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin {
    private static final String TAG = "BluetoothFilesPrefCtrl";

    public static final String KEY_RECEIVED_FILES = "bt_received_files";

    /* Private intent to show the list of received files */
    @VisibleForTesting
    static final String ACTION_OPEN_FILES = "com.android.bluetooth.action.TransferHistory";
    @VisibleForTesting
    static final String EXTRA_SHOW_ALL_FILES = "android.btopp.intent.extra.SHOW_ALL";
    @VisibleForTesting
    static final String EXTRA_DIRECTION = "direction";

    private MetricsFeatureProvider mMetricsFeatureProvider;

    public BluetoothFilesPreferenceController(Context context) {
        super(context, KEY_RECEIVED_FILES);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_RECEIVED_FILES;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_RECEIVED_FILES.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_BLUETOOTH_FILES);
            Intent intent = new Intent(ACTION_OPEN_FILES);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(EXTRA_DIRECTION, 1 /* DIRECTION_INBOUND */);
            intent.putExtra(EXTRA_SHOW_ALL_FILES, true);
            mContext.startActivity(intent);
            return true;
        }

        return false;
    }


}
