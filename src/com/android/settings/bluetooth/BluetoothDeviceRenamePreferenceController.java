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
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class BluetoothDeviceRenamePreferenceController extends
        BluetoothDeviceNamePreferenceController {

    private Fragment mFragment;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    /**
     * Constructor exclusively used for Slice.
     */
    public BluetoothDeviceRenamePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    /**
     * Set the {@link Fragment} that used to show {@link LocalDeviceNameDialogFragment}
     * in {@code handlePreferenceTreeClick}
     */
    @VisibleForTesting
    public void setFragment(Fragment fragment) {
        mFragment = fragment;
    }

    @Override
    protected void updatePreferenceState(final Preference preference) {
        preference.setSummary(getSummary());
        preference.setVisible(mBluetoothAdapter != null && mBluetoothAdapter.isEnabled());
    }

    @Override
    public CharSequence getSummary() {
        return getDeviceName();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(getPreferenceKey(), preference.getKey()) && mFragment != null) {
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_BLUETOOTH_RENAME);
            new LocalDeviceNameDialogFragment()
                    .show(mFragment.getFragmentManager(), LocalDeviceNameDialogFragment.TAG);
            return true;
        }

        return false;
    }
}
