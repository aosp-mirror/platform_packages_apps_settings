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

package com.android.settings.bluetooth;

import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.ORDER_HEARING_AIDS_PRESETS;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHapClient;
import android.bluetooth.BluetoothHapPresetInfo;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HapClientProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;

/**
 * The controller of the hearing aid presets.
 */
public class BluetoothDetailsHearingAidsPresetsController extends
        BluetoothDetailsController implements Preference.OnPreferenceChangeListener,
        BluetoothHapClient.Callback, OnResume, OnPause {

    private static final boolean DEBUG = true;
    private static final String TAG = "BluetoothDetailsHearingAidsPresetsController";
    static final String KEY_HEARING_AIDS_PRESETS = "hearing_aids_presets";

    private final HapClientProfile mHapClientProfile;
    @Nullable
    private ListPreference mPreference;

    public BluetoothDetailsHearingAidsPresetsController(@NonNull Context context,
            @NonNull PreferenceFragmentCompat fragment,
            @NonNull LocalBluetoothManager manager,
            @NonNull CachedBluetoothDevice device,
            @NonNull Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mHapClientProfile = manager.getProfileManager().getHapClientProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mHapClientProfile != null) {
            mHapClientProfile.registerCallback(ThreadUtils.getBackgroundExecutor(), this);
        }
    }

    @Override
    public void onPause() {
        if (mHapClientProfile != null) {
            mHapClientProfile.unregisterCallback(this);
        }
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @Nullable Object newValue) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            // TODO(b/300015207): Update the settings to remote device
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public String getPreferenceKey() {
        return KEY_HEARING_AIDS_PRESETS;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        PreferenceCategory deviceControls = screen.findPreference(KEY_HEARING_DEVICE_GROUP);
        if (deviceControls != null) {
            mPreference = createPresetPreference(deviceControls.getContext());
            deviceControls.addPreference(mPreference);
        }
    }

    @Override
    protected void refresh() {
        if (!isAvailable() || mPreference == null) {
            return;
        }
        mPreference.setEnabled(mCachedDevice.isConnectedHapClientDevice());
        // TODO(b/300015207): Load preset from remote and show in UI
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void onPresetSelected(@NonNull BluetoothDevice device, int presetIndex, int reason) {
        if (device.equals(mCachedDevice.getDevice())) {
            if (DEBUG) {
                Log.d(TAG, "onPresetSelected, device: " + device.getAddress()
                        + ", presetIndex: " + presetIndex + ", reason: " + reason);
            }
            // TODO(b/300015207): Update the UI
        }
    }

    @Override
    public void onPresetSelectionFailed(@NonNull BluetoothDevice device, int reason) {
        if (device.equals(mCachedDevice.getDevice())) {
            if (DEBUG) {
                Log.d(TAG,
                        "onPresetSelectionFailed, device: " + device.getAddress()
                                + ", reason: " + reason);
            }
            // TODO(b/300015207): Update the UI
        }
    }

    @Override
    public void onPresetSelectionForGroupFailed(int hapGroupId, int reason) {
        if (hapGroupId == mHapClientProfile.getHapGroup(mCachedDevice.getDevice())) {
            if (DEBUG) {
                Log.d(TAG, "onPresetSelectionForGroupFailed, group: " + hapGroupId
                        + ", reason: " + reason);
            }
            // TODO(b/300015207): Update the UI
        }
    }

    @Override
    public void onPresetInfoChanged(@NonNull BluetoothDevice device,
            @NonNull List<BluetoothHapPresetInfo> presetInfoList, int reason) {
        if (device.equals(mCachedDevice.getDevice())) {
            if (DEBUG) {
                Log.d(TAG, "onPresetInfoChanged, device: " + device.getAddress()
                        + ", reason: " + reason
                        + ", infoList: " + presetInfoList);
            }
            // TODO(b/300015207): Update the UI
        }
    }

    @Override
    public void onSetPresetNameFailed(@NonNull BluetoothDevice device, int reason) {
        if (device.equals(mCachedDevice.getDevice())) {
            if (DEBUG) {
                Log.d(TAG,
                        "onSetPresetNameFailed, device: " + device.getAddress()
                                + ", reason: " + reason);
            }
            // TODO(b/300015207): Update the UI
        }
    }

    @Override
    public void onSetPresetNameForGroupFailed(int hapGroupId, int reason) {
        if (hapGroupId == mHapClientProfile.getHapGroup(mCachedDevice.getDevice())) {
            if (DEBUG) {
                Log.d(TAG, "onSetPresetNameForGroupFailed, group: " + hapGroupId
                        + ", reason: " + reason);
            }
            // TODO(b/300015207): Update the UI
        }
    }

    private ListPreference createPresetPreference(Context context) {
        ListPreference preference = new ListPreference(context);
        preference.setKey(KEY_HEARING_AIDS_PRESETS);
        preference.setOrder(ORDER_HEARING_AIDS_PRESETS);
        preference.setTitle(context.getString(R.string.bluetooth_hearing_aids_presets));
        preference.setOnPreferenceChangeListener(this);
        return preference;
    }
}
