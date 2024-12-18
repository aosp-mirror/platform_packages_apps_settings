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
package com.android.settings.connecteddevice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.SavedBluetoothDeviceUpdater;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.DockUpdaterFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller to maintain the {@link PreferenceGroup} for all
 * saved devices. It uses {@link DevicePreferenceCallback} to add/remove {@link Preference}
 */
public class SavedDeviceGroupController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop,
        DevicePreferenceCallback {

    private static final String KEY = "saved_device_list";

    private final Map<BluetoothDevice, Preference> mDevicePreferenceMap = new HashMap<>();
    private final List<Preference> mDockDevicesList = new ArrayList<>();
    private final BluetoothAdapter mBluetoothAdapter;

    @VisibleForTesting
    PreferenceGroup mPreferenceGroup;
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private DockUpdater mSavedDockUpdater;

    public SavedDeviceGroupController(Context context) {
        super(context, KEY);

        DockUpdaterFeatureProvider dockUpdaterFeatureProvider =
                FeatureFactory.getFeatureFactory().getDockUpdaterFeatureProvider();
        mSavedDockUpdater =
                dockUpdaterFeatureProvider.getSavedDockUpdater(context, this);
        mBluetoothAdapter = context.getSystemService(BluetoothManager.class).getAdapter();
    }

    @Override
    public void onStart() {
        mBluetoothDeviceUpdater.registerCallback();
        mSavedDockUpdater.registerCallback();
        mBluetoothDeviceUpdater.refreshPreference();
        updatePreferenceGroup();
    }

    @Override
    public void onStop() {
        mBluetoothDeviceUpdater.unregisterCallback();
        mSavedDockUpdater.unregisterCallback();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceGroup = screen.findPreference(KEY);
        mPreferenceGroup.setVisible(false);

        if (isAvailable()) {
            final Context context = screen.getContext();
            mBluetoothDeviceUpdater.setPrefContext(context);
            mBluetoothDeviceUpdater.forceUpdate();
            mSavedDockUpdater.setPreferenceContext(context);
            mSavedDockUpdater.forceUpdate();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                || mSavedDockUpdater != null)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        mPreferenceGroup.addPreference(preference);
        if (preference instanceof BluetoothDevicePreference) {
            mDevicePreferenceMap.put(
                    ((BluetoothDevicePreference) preference).getBluetoothDevice().getDevice(),
                    preference);
        } else {
            mDockDevicesList.add(preference);
        }
        updatePreferenceGroup();
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mPreferenceGroup.removePreference(preference);
        if (preference instanceof BluetoothDevicePreference) {
            mDevicePreferenceMap.remove(
                    ((BluetoothDevicePreference) preference).getBluetoothDevice().getDevice(),
                    preference);
        } else {
            mDockDevicesList.remove(preference);
        }
        updatePreferenceGroup();
    }

    /** Sort the preferenceGroup by most recently used. */
    public void updatePreferenceGroup() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            // Bluetooth is unsupported or disabled
            mPreferenceGroup.setVisible(false);
        } else {
            mPreferenceGroup.setVisible(true);
            int order = 0;
            for (BluetoothDevice device : mBluetoothAdapter.getMostRecentlyConnectedDevices()) {
                Preference preference = mDevicePreferenceMap.getOrDefault(device, null);
                if (preference != null) {
                    preference.setOrder(order);
                    order += 1;
                }
            }
            for (Preference preference : mDockDevicesList) {
                preference.setOrder(order);
                order += 1;
            }
        }
    }

    public void init(DashboardFragment fragment) {
        mBluetoothDeviceUpdater = new SavedBluetoothDeviceUpdater(fragment.getContext(),
                SavedDeviceGroupController.this, /* showConnectedDevice= */true,
                fragment.getMetricsCategory());
    }

    @VisibleForTesting
    public void setBluetoothDeviceUpdater(BluetoothDeviceUpdater bluetoothDeviceUpdater) {
        mBluetoothDeviceUpdater = bluetoothDeviceUpdater;
    }

    @VisibleForTesting
    public void setSavedDockUpdater(DockUpdater savedDockUpdater) {
        mSavedDockUpdater = savedDockUpdater;
    }

    @VisibleForTesting
    void setPreferenceGroup(PreferenceGroup preferenceGroup) {
        mPreferenceGroup = preferenceGroup;
    }
}
