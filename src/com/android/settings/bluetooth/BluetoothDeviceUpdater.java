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
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HeadsetProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Update the bluetooth devices. It gets bluetooth event from {@link LocalBluetoothManager} using
 * {@link BluetoothCallback}. It notifies the upper level whether to add/remove the preference
 * through {@link DevicePreferenceCallback}
 *
 * In {@link BluetoothDeviceUpdater}, it uses {@link BluetoothDeviceFilter.Filter} to detect
 * whether the {@link CachedBluetoothDevice} is relevant.
 */
public abstract class BluetoothDeviceUpdater implements BluetoothCallback {
    private static final String TAG = "BluetoothDeviceUpdater";
    private static final String BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY =
            "persist.bluetooth.showdeviceswithoutnames";

    protected final LocalBluetoothManager mLocalManager;
    protected final DevicePreferenceCallback mDevicePreferenceCallback;
    protected final Map<BluetoothDevice, Preference> mPreferenceMap;
    protected Context mPrefContext;

    private final boolean mShowDeviceWithoutNames;
    private DashboardFragment mFragment;
    private Preference.OnPreferenceClickListener mDevicePreferenceClickListener = null;

    @VisibleForTesting
    final GearPreference.OnGearClickListener mDeviceProfilesListener = pref -> {
        final CachedBluetoothDevice device =
                ((BluetoothDevicePreference) pref).getBluetoothDevice();
        if (device == null) {
            return;
        }
        final Bundle args = new Bundle();
        args.putString(BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS,
                device.getDevice().getAddress());

        new SubSettingLauncher(mFragment.getContext())
                .setDestination(BluetoothDeviceDetailsFragment.class.getName())
                .setArguments(args)
                .setTitle(R.string.device_details_title)
                .setSourceMetricsCategory(mFragment.getMetricsCategory())
                .launch();

    };

    private class PreferenceClickListener implements
        Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final CachedBluetoothDevice device =
                ((BluetoothDevicePreference) preference).getBluetoothDevice();
            Log.i(TAG, "OnPreferenceClickListener: device=" + device);
            return device.setActive();
        }
    }

    public BluetoothDeviceUpdater(Context context, DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback) {
        this(fragment, devicePreferenceCallback, Utils.getLocalBtManager(context));
    }

    @VisibleForTesting
    BluetoothDeviceUpdater(DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback, LocalBluetoothManager localManager) {
        mFragment = fragment;
        mDevicePreferenceCallback = devicePreferenceCallback;
        mShowDeviceWithoutNames = SystemProperties.getBoolean(
                BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY, false);
        mPreferenceMap = new HashMap<>();
        mLocalManager = localManager;
        mDevicePreferenceClickListener = new PreferenceClickListener();
    }

    /**
     * Register the bluetooth event callback and update the list
     */
    public void registerCallback() {
        mLocalManager.setForegroundActivity(mFragment.getContext());
        mLocalManager.getEventManager().registerCallback(this);
        forceUpdate();
    }

    /**
     * Unregister the bluetooth event callback
     */
    public void unregisterCallback() {
        mLocalManager.setForegroundActivity(null);
        mLocalManager.getEventManager().unregisterCallback(this);
    }

    /**
     * Force to update the list of bluetooth devices
     */
    public void forceUpdate() {
        Collection<CachedBluetoothDevice> cachedDevices =
                mLocalManager.getCachedDeviceManager().getCachedDevicesCopy();
        for (CachedBluetoothDevice cachedBluetoothDevice : cachedDevices) {
            update(cachedBluetoothDevice);
        }
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        forceUpdate();
    }

    @Override
    public void onScanningStateChanged(boolean started) {}

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        update(cachedDevice);
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {}

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        update(cachedDevice);
    }

    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {}

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
    }

    /**
     * Set the context to generate the {@link Preference}, so it could get the correct theme.
     */
    public void setPrefContext(Context context) {
        mPrefContext = context;
    }

    /**
     * Return {@code true} if {@code cachedBluetoothDevice} matches this
     * {@link BluetoothDeviceUpdater} and should stay in the list, otherwise return {@code false}
     */
    public abstract boolean isFilterMatched(CachedBluetoothDevice cachedBluetoothDevice);

    /**
     * Update whether to show {@cde cachedBluetoothDevice} in the list.
     */
    protected void update(CachedBluetoothDevice cachedBluetoothDevice) {
        if (isFilterMatched(cachedBluetoothDevice)) {
            // Add the preference if it is new one
            addPreference(cachedBluetoothDevice);
        } else {
            removePreference(cachedBluetoothDevice);
        }
    }

    /**
     * Add the {@link Preference} that represents the {@code cachedDevice}
     */
    protected void addPreference(CachedBluetoothDevice cachedDevice) {
        final BluetoothDevice device = cachedDevice.getDevice();
        if (!mPreferenceMap.containsKey(device)) {
            BluetoothDevicePreference btPreference =
                    new BluetoothDevicePreference(mPrefContext, cachedDevice,
                            mShowDeviceWithoutNames);
            btPreference.setOnGearClickListener(mDeviceProfilesListener);
            btPreference.setOnPreferenceClickListener(mDevicePreferenceClickListener);
            mPreferenceMap.put(device, btPreference);
            mDevicePreferenceCallback.onDeviceAdded(btPreference);
        }
    }

    /**
     * Remove the {@link Preference} that represents the {@code cachedDevice}
     */
    protected void removePreference(CachedBluetoothDevice cachedDevice) {
        final BluetoothDevice device = cachedDevice.getDevice();
        if (mPreferenceMap.containsKey(device)) {
            mDevicePreferenceCallback.onDeviceRemoved(mPreferenceMap.get(device));
            mPreferenceMap.remove(device);
        }
    }
}
