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

import static com.android.settings.bluetooth.AmbientVolumePreference.SIDE_UNIFIED;
import static com.android.settings.bluetooth.AmbientVolumePreference.VALID_SIDES;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.ORDER_AMBIENT_VOLUME;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_INVALID;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.Set;

/** A {@link BluetoothDetailsController} that manages ambient volume control preferences. */
public class BluetoothDetailsAmbientVolumePreferenceController extends
        BluetoothDetailsController implements Preference.OnPreferenceChangeListener {

    private static final boolean DEBUG = true;
    private static final String TAG = "AmbientPrefController";

    static final String KEY_AMBIENT_VOLUME = "ambient_volume";
    static final String KEY_AMBIENT_VOLUME_SLIDER = "ambient_volume_slider";
    private static final int ORDER_AMBIENT_VOLUME_CONTROL_UNIFIED = 0;
    private static final int ORDER_AMBIENT_VOLUME_CONTROL_SEPARATED = 1;

    private final Set<CachedBluetoothDevice> mCachedDevices = new ArraySet<>();
    private final BiMap<Integer, BluetoothDevice> mSideToDeviceMap = HashBiMap.create();
    private final BiMap<Integer, SeekBarPreference> mSideToSliderMap = HashBiMap.create();

    @Nullable
    private PreferenceCategory mDeviceControls;
    @Nullable
    private AmbientVolumePreference mPreference;

    public BluetoothDetailsAmbientVolumePreferenceController(@NonNull Context context,
            @NonNull PreferenceFragmentCompat fragment,
            @NonNull CachedBluetoothDevice device,
            @NonNull Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mDeviceControls = screen.findPreference(KEY_HEARING_DEVICE_GROUP);
        if (mDeviceControls == null) {
            return;
        }
        loadDevices();
    }

    @Override
    protected void refresh() {
        if (!isAvailable()) {
            return;
        }
        // TODO: load data from remote
        refreshControlUi();
    }

    @Override
    public boolean isAvailable() {
        boolean isDeviceSupportVcp = mCachedDevice.getProfiles().stream().anyMatch(
                profile -> profile instanceof VolumeControlProfile);
        return isDeviceSupportVcp;
    }

    @Nullable
    @Override
    public String getPreferenceKey() {
        return KEY_AMBIENT_VOLUME;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @Nullable Object newValue) {
        if (preference instanceof SeekBarPreference && newValue instanceof final Integer value) {
            final int side = mSideToSliderMap.inverse().getOrDefault(preference, SIDE_INVALID);
            if (DEBUG) {
                Log.d(TAG, "onPreferenceChange: side=" + side + ", value=" + value);
            }
            if (side == SIDE_UNIFIED) {
                // TODO: set the value on the devices
            } else {
                // TODO: set the value on the side device
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDeviceAttributesChanged() {
        mCachedDevices.forEach(device -> {
            device.unregisterCallback(this);
        });
        mContext.getMainExecutor().execute(() -> {
            loadDevices();
            if (!mCachedDevices.isEmpty()) {
                refresh();
            }
            ThreadUtils.postOnBackgroundThread(() ->
                    mCachedDevices.forEach(device -> {
                        device.registerCallback(ThreadUtils.getBackgroundExecutor(), this);
                    })
            );
        });
    }

    private void loadDevices() {
        mSideToDeviceMap.clear();
        mCachedDevices.clear();
        if (VALID_SIDES.contains(mCachedDevice.getDeviceSide())) {
            mSideToDeviceMap.put(mCachedDevice.getDeviceSide(), mCachedDevice.getDevice());
            mCachedDevices.add(mCachedDevice);
        }
        for (CachedBluetoothDevice memberDevice : mCachedDevice.getMemberDevice()) {
            if (VALID_SIDES.contains(memberDevice.getDeviceSide())) {
                mSideToDeviceMap.put(memberDevice.getDeviceSide(), memberDevice.getDevice());
                mCachedDevices.add(memberDevice);
            }
        }
        createAmbientVolumePreference();
        createSliderPreferences();
        if (mPreference != null) {
            mPreference.setExpandable(mSideToDeviceMap.size() > 1);
            mPreference.setSliders((mSideToSliderMap));
        }
    }

    private void createAmbientVolumePreference() {
        if (mPreference != null || mDeviceControls == null) {
            return;
        }
        mPreference = new AmbientVolumePreference(mDeviceControls.getContext());
        mPreference.setKey(KEY_AMBIENT_VOLUME);
        mPreference.setOrder(ORDER_AMBIENT_VOLUME);
        if (mDeviceControls.findPreference(mPreference.getKey()) == null) {
            mDeviceControls.addPreference(mPreference);
        }
    }

    private void createSliderPreferences() {
        mSideToDeviceMap.forEach((s, d) ->
                createSliderPreference(s, ORDER_AMBIENT_VOLUME_CONTROL_SEPARATED + s));
        createSliderPreference(SIDE_UNIFIED, ORDER_AMBIENT_VOLUME_CONTROL_UNIFIED);
    }

    private void createSliderPreference(int side, int order) {
        if (mSideToSliderMap.containsKey(side) || mDeviceControls == null) {
            return;
        }
        SeekBarPreference preference = new SeekBarPreference(mDeviceControls.getContext());
        preference.setKey(KEY_AMBIENT_VOLUME_SLIDER + "_" + side);
        preference.setOrder(order);
        preference.setOnPreferenceChangeListener(this);
        mSideToSliderMap.put(side, preference);
    }

    /** Refreshes the control UI visibility and enabled state. */
    private void refreshControlUi() {
        if (mPreference != null) {
            mPreference.updateLayout();
        }
    }
}
