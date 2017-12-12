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

import android.app.Fragment;
import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class BluetoothDeviceRenamePreferenceController extends
        BluetoothDeviceNamePreferenceController {

    private final Fragment mFragment;
    private String mPrefKey;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    public BluetoothDeviceRenamePreferenceController(Context context, String prefKey,
            Fragment fragment, Lifecycle lifecycle) {
        super(context, lifecycle);
        mPrefKey = prefKey;
        mFragment = fragment;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    /**
     * Constructor exclusively used for Slice.
     */
    public BluetoothDeviceRenamePreferenceController(Context context, String prefKey) {
        super(context, (Lifecycle) null);
        mPrefKey = prefKey;
        mFragment = null;
    }

    @VisibleForTesting
    BluetoothDeviceRenamePreferenceController(Context context, String prefKey, Fragment fragment,
            LocalBluetoothAdapter localAdapter) {
        super(context, localAdapter);
        mPrefKey = prefKey;
        mFragment = fragment;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public String getPreferenceKey() {
        return mPrefKey;
    }

    @Override
    protected void updateDeviceName(final Preference preference, final String deviceName) {
        preference.setSummary(deviceName);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(mPrefKey, preference.getKey()) && mFragment != null) {
            mMetricsFeatureProvider.action(mContext,
                    MetricsProto.MetricsEvent.ACTION_BLUETOOTH_RENAME);
            LocalDeviceNameDialogFragment.newInstance()
                    .show(mFragment.getFragmentManager(), LocalDeviceNameDialogFragment.TAG);
            return true;
        }

        return false;
    }
}
