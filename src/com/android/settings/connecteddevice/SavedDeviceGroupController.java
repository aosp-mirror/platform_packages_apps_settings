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

import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.SavedBluetoothDeviceUpdater;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Controller to maintain the {@link PreferenceGroup} for all
 * saved devices. It uses {@link DevicePreferenceCallback} to add/remove {@link Preference}
 */
public class SavedDeviceGroupController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop,
        DevicePreferenceCallback {

    private static final String KEY = "saved_device_list";

    @VisibleForTesting
    PreferenceGroup mPreferenceGroup;
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;

    public SavedDeviceGroupController(DashboardFragment fragment, Lifecycle lifecycle) {
        super(fragment.getContext());
        init(lifecycle, new SavedBluetoothDeviceUpdater(fragment, SavedDeviceGroupController.this));
    }

    @VisibleForTesting
    SavedDeviceGroupController(DashboardFragment fragment, Lifecycle lifecycle,
            BluetoothDeviceUpdater bluetoothDeviceUpdater) {
        super(fragment.getContext());
        init(lifecycle, bluetoothDeviceUpdater);
    }

    @Override
    public void onStart() {
        mBluetoothDeviceUpdater.registerCallback();
    }

    @Override
    public void onStop() {
        mBluetoothDeviceUpdater.unregisterCallback();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceGroup = (PreferenceGroup) screen.findPreference(KEY);
        mPreferenceGroup.setVisible(false);
        mBluetoothDeviceUpdater.setPrefContext(screen.getContext());
        mBluetoothDeviceUpdater.forceUpdate();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        if (mPreferenceGroup.getPreferenceCount() == 0) {
            mPreferenceGroup.setVisible(true);
        }
        mPreferenceGroup.addPreference(preference);
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mPreferenceGroup.removePreference(preference);
        if (mPreferenceGroup.getPreferenceCount() == 0) {
            mPreferenceGroup.setVisible(false);
        }
    }

    private void init(Lifecycle lifecycle, BluetoothDeviceUpdater bluetoothDeviceUpdater) {
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mBluetoothDeviceUpdater = bluetoothDeviceUpdater;
    }
}
