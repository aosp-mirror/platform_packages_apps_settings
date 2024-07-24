/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.Context;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityHearingAidsFragment;
import com.android.settings.accessibility.ArrowPreference;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.google.common.annotations.VisibleForTesting;

/**
 * The controller of the hearing device settings to launch Hearing device page.
 */
public class BluetoothDetailsHearingDeviceControlsController extends BluetoothDetailsController
        implements Preference.OnPreferenceClickListener {

    @VisibleForTesting
    static final String KEY_DEVICE_CONTROLS_GENERAL_GROUP = "device_controls_general";
    @VisibleForTesting
    static final String KEY_HEARING_DEVICE_CONTROLS = "hearing_device_controls";

    public BluetoothDetailsHearingDeviceControlsController(Context context,
            PreferenceFragmentCompat fragment, CachedBluetoothDevice device, Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return mCachedDevice.isHearingAidDevice();
    }

    @Override
    protected void init(PreferenceScreen screen) {
        if (!mCachedDevice.isHearingAidDevice()) {
            return;
        }

        final PreferenceCategory prefCategory = screen.findPreference(getPreferenceKey());
        final Preference pref = createHearingDeviceControlsPreference(prefCategory.getContext());
        prefCategory.addPreference(pref);
    }

    @Override
    protected void refresh() {}

    @Override
    public String getPreferenceKey() {
        return KEY_DEVICE_CONTROLS_GENERAL_GROUP;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), KEY_HEARING_DEVICE_CONTROLS)) {
            launchAccessibilityHearingDeviceSettings();
            return true;
        }
        return false;
    }

    private Preference createHearingDeviceControlsPreference(Context context) {
        final ArrowPreference preference = new ArrowPreference(context);
        preference.setKey(KEY_HEARING_DEVICE_CONTROLS);
        preference.setTitle(context.getString(R.string.bluetooth_device_controls_title));
        preference.setSummary(context.getString(R.string.bluetooth_device_controls_summary));
        preference.setOnPreferenceClickListener(this);

        return preference;
    }

    private void launchAccessibilityHearingDeviceSettings() {
        new SubSettingLauncher(mContext)
                .setDestination(AccessibilityHearingAidsFragment.class.getName())
                .setSourceMetricsCategory(
                        ((BluetoothDeviceDetailsFragment) mFragment).getMetricsCategory())
                .launch();
    }
}
