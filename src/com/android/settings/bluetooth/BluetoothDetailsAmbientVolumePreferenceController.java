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

import static android.bluetooth.BluetoothDevice.BOND_BONDED;

import static com.android.settings.bluetooth.AmbientVolumePreference.SIDE_UNIFIED;
import static com.android.settings.bluetooth.AmbientVolumePreference.VALID_SIDES;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.ORDER_AMBIENT_VOLUME;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_INVALID;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT;
import static com.android.settingslib.bluetooth.HearingDeviceLocalDataManager.Data.INVALID_VOLUME;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingDeviceLocalDataManager;
import com.android.settingslib.bluetooth.HearingDeviceLocalDataManager.Data;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.Set;

/** A {@link BluetoothDetailsController} that manages ambient volume control preferences. */
public class BluetoothDetailsAmbientVolumePreferenceController extends
        BluetoothDetailsController implements Preference.OnPreferenceChangeListener,
        HearingDeviceLocalDataManager.OnDeviceLocalDataChangeListener, OnStart, OnStop {

    private static final boolean DEBUG = true;
    private static final String TAG = "AmbientPrefController";

    static final String KEY_AMBIENT_VOLUME = "ambient_volume";
    static final String KEY_AMBIENT_VOLUME_SLIDER = "ambient_volume_slider";
    private static final int ORDER_AMBIENT_VOLUME_CONTROL_UNIFIED = 0;
    private static final int ORDER_AMBIENT_VOLUME_CONTROL_SEPARATED = 1;

    private final Set<CachedBluetoothDevice> mCachedDevices = new ArraySet<>();
    private final BiMap<Integer, BluetoothDevice> mSideToDeviceMap = HashBiMap.create();
    private final BiMap<Integer, SeekBarPreference> mSideToSliderMap = HashBiMap.create();
    private final HearingDeviceLocalDataManager mLocalDataManager;

    @Nullable
    private PreferenceCategory mDeviceControls;
    @Nullable
    private AmbientVolumePreference mPreference;

    public BluetoothDetailsAmbientVolumePreferenceController(@NonNull Context context,
            @NonNull PreferenceFragmentCompat fragment,
            @NonNull CachedBluetoothDevice device,
            @NonNull Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mLocalDataManager = new HearingDeviceLocalDataManager(context);
        mLocalDataManager.setOnDeviceLocalDataChangeListener(this,
                ThreadUtils.getBackgroundExecutor());
    }

    @VisibleForTesting
    BluetoothDetailsAmbientVolumePreferenceController(@NonNull Context context,
            @NonNull PreferenceFragmentCompat fragment,
            @NonNull CachedBluetoothDevice device,
            @NonNull Lifecycle lifecycle,
            @NonNull HearingDeviceLocalDataManager localSettings) {
        super(context, fragment, device, lifecycle);
        mLocalDataManager = localSettings;
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
    public void onStart() {
        ThreadUtils.postOnBackgroundThread(() -> {
            mLocalDataManager.start();
            mCachedDevices.forEach(device -> {
                device.registerCallback(ThreadUtils.getBackgroundExecutor(), this);
            });
        });
    }

    @Override
    public void onStop() {
        ThreadUtils.postOnBackgroundThread(() -> {
            mLocalDataManager.stop();
            mCachedDevices.forEach(device -> {
                device.unregisterCallback(this);
            });
        });
    }

    @Override
    protected void refresh() {
        if (!isAvailable()) {
            return;
        }
        // TODO: load data from remote
        loadLocalDataToUi();
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
            setVolumeIfValid(side, value);

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

    @Override
    public void onDeviceLocalDataChange(@NonNull String address, @Nullable Data data) {
        if (data == null) {
            // The local data is removed because the device is unpaired, do nothing
            return;
        }
        for (BluetoothDevice device : mSideToDeviceMap.values()) {
            if (device.getAnonymizedAddress().equals(address)) {
                mContext.getMainExecutor().execute(() -> loadLocalDataToUi(device));
                return;
            }
        }
    }

    private void loadDevices() {
        mSideToDeviceMap.clear();
        mCachedDevices.clear();
        if (VALID_SIDES.contains(mCachedDevice.getDeviceSide())
                && mCachedDevice.getBondState() == BOND_BONDED) {
            mSideToDeviceMap.put(mCachedDevice.getDeviceSide(), mCachedDevice.getDevice());
            mCachedDevices.add(mCachedDevice);
        }
        for (CachedBluetoothDevice memberDevice : mCachedDevice.getMemberDevice()) {
            if (VALID_SIDES.contains(memberDevice.getDeviceSide())
                    && memberDevice.getBondState() == BOND_BONDED) {
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
        mPreference.setOnIconClickListener(() -> {
            mSideToDeviceMap.forEach((s, d) -> {
                // Update new value to local data
                mLocalDataManager.updateAmbientControlExpanded(d, isControlExpanded());
            });
        });
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
        if (side == SIDE_LEFT) {
            preference.setTitle(mContext.getString(R.string.bluetooth_ambient_volume_control_left));
        } else if (side == SIDE_RIGHT) {
            preference.setTitle(
                    mContext.getString(R.string.bluetooth_ambient_volume_control_right));
        }
        mSideToSliderMap.put(side, preference);
    }

    /** Refreshes the control UI visibility and enabled state. */
    private void refreshControlUi() {
        if (mPreference != null) {
            mPreference.updateLayout();
        }
    }

    /** Sets the volume to the corresponding control slider. */
    private void setVolumeIfValid(int side, int volume) {
        if (volume == INVALID_VOLUME) {
            return;
        }
        if (mPreference != null) {
            mPreference.setSliderValue(side, volume);
        }
        // Update new value to local data
        if (side == SIDE_UNIFIED) {
            mSideToDeviceMap.forEach((s, d) -> mLocalDataManager.updateGroupAmbient(d, volume));
        } else {
            mLocalDataManager.updateAmbient(mSideToDeviceMap.get(side), volume);
        }
    }

    private void loadLocalDataToUi() {
        mSideToDeviceMap.forEach((s, d) -> loadLocalDataToUi(d));
    }

    private void loadLocalDataToUi(BluetoothDevice device) {
        final Data data = mLocalDataManager.get(device);
        if (DEBUG) {
            Log.d(TAG, "loadLocalDataToUi, data=" + data + ", device=" + device);
        }
        final int side = mSideToDeviceMap.inverse().getOrDefault(device, SIDE_INVALID);
        setVolumeIfValid(side, data.ambient());
        setVolumeIfValid(SIDE_UNIFIED, data.groupAmbient());
        setControlExpanded(data.ambientControlExpanded());
        refreshControlUi();
    }

    private boolean isControlExpanded() {
        return mPreference != null && mPreference.isExpanded();
    }

    private void setControlExpanded(boolean expanded) {
        if (mPreference != null && mPreference.isExpanded() != expanded) {
            mPreference.setExpanded(expanded);
        }
        mSideToDeviceMap.forEach((s, d) -> {
            // Update new value to local data
            mLocalDataManager.updateAmbientControlExpanded(d, expanded);
        });
    }
}
