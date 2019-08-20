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

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.Pair;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

/**
 * This class adds a header with device name and status (connected/disconnected, etc.).
 */
public class BluetoothDetailsHeaderController extends BluetoothDetailsController {
    private static final String KEY_DEVICE_HEADER = "bluetooth_device_header";

    private EntityHeaderController mHeaderController;
    private LocalBluetoothManager mLocalManager;
    private CachedBluetoothDeviceManager mDeviceManager;

    public BluetoothDetailsHeaderController(Context context, PreferenceFragmentCompat fragment,
            CachedBluetoothDevice device, Lifecycle lifecycle,
            LocalBluetoothManager bluetoothManager) {
        super(context, fragment, device, lifecycle);
        mLocalManager = bluetoothManager;
        mDeviceManager = mLocalManager.getCachedDeviceManager();
    }

    @Override
    public boolean isAvailable() {
        final boolean advancedEnabled = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_ADVANCED_HEADER_ENABLED, true);
        return !advancedEnabled
                || !BluetoothUtils.getBooleanMetaData(mCachedDevice.getDevice(),
                        BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET);
    }

    @Override
    protected void init(PreferenceScreen screen) {
        final LayoutPreference headerPreference = screen.findPreference(KEY_DEVICE_HEADER);
        mHeaderController = EntityHeaderController.newInstance(mFragment.getActivity(), mFragment,
                headerPreference.findViewById(R.id.entity_header));
        screen.addPreference(headerPreference);
    }

    protected void setHeaderProperties() {
        final Pair<Drawable, String> pair =
                BluetoothUtils.getBtRainbowDrawableWithDescription(mContext, mCachedDevice);
        String summaryText = mCachedDevice.getConnectionSummary();
        if (TextUtils.isEmpty(summaryText)) {
            // If first summary is unavailable, not to show second summary.
            mHeaderController.setSecondSummary((CharSequence)null);
        } else {
            // If both the hearing aids are connected, two device status should be shown.
            mHeaderController.setSecondSummary(mDeviceManager.getSubDeviceSummary(mCachedDevice));
        }
        mHeaderController.setLabel(mCachedDevice.getName());
        mHeaderController.setIcon(pair.first);
        mHeaderController.setIconContentDescription(pair.second);
        mHeaderController.setSummary(summaryText);
    }

    @Override
    protected void refresh() {
        if (isAvailable()) {
            setHeaderProperties();
            mHeaderController.done(mFragment.getActivity(), true /* rebindActions */);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DEVICE_HEADER;
    }
}