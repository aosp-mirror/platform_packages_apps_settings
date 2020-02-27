/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.debug.PairDevice;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment shown when clicking on a paired device in the Wireless
 * Debugging fragment.
 */
public class AdbDeviceDetailsFragment extends DashboardFragment {
    private static final String TAG = "AdbDeviceDetailsFrag";
    private PairDevice mPairedDevice;

    public AdbDeviceDetailsFragment() {
        super();
    }

    @Override
    public void onAttach(Context context) {
        // Get the paired device stored in the extras
        Bundle bundle = getArguments();
        if (bundle.containsKey(AdbPairedDevicePreference.PAIRED_DEVICE_EXTRA)) {
            mPairedDevice = (PairDevice) bundle.getParcelable(
                    AdbPairedDevicePreference.PAIRED_DEVICE_EXTRA);
        }
        super.onAttach(context);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ADB_WIRELESS_DEVICE_DETAILS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.adb_device_details_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new AdbDeviceDetailsHeaderController(mPairedDevice, context, this));
        controllers.add(new AdbDeviceDetailsActionController(mPairedDevice, context, this));
        controllers.add(new AdbDeviceDetailsFingerprintController(mPairedDevice, context, this));

        return controllers;
    }
}
