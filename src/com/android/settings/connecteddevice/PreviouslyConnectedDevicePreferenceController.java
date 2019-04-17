/*
 * Copyright 2018 The Android Open Source Project
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

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.SavedBluetoothDeviceUpdater;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class PreviouslyConnectedDevicePreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop, DevicePreferenceCallback {

    private static final int MAX_DEVICE_NUM = 3;

    private PreferenceGroup mPreferenceGroup;
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private DockUpdater mSavedDockUpdater;
    private int mPreferenceSize;

    public PreviouslyConnectedDevicePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mSavedDockUpdater = FeatureFactory.getFactory(
                context).getDockUpdaterFeatureProvider().getSavedDockUpdater(context, this);
    }

    @Override
    public int getAvailabilityStatus() {
        return (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                || mSavedDockUpdater != null)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(getPreferenceKey());
        mPreferenceGroup.setVisible(false);

        if (isAvailable()) {
            final Context context = screen.getContext();
            mBluetoothDeviceUpdater.setPrefContext(context);
            mSavedDockUpdater.setPreferenceContext(context);
        }
    }

    @Override
    public void onStart() {
        mBluetoothDeviceUpdater.registerCallback();
        mSavedDockUpdater.registerCallback();
    }

    @Override
    public void onStop() {
        mBluetoothDeviceUpdater.unregisterCallback();
        mSavedDockUpdater.unregisterCallback();
    }

    public void init(DashboardFragment fragment) {
        mBluetoothDeviceUpdater = new SavedBluetoothDeviceUpdater(fragment.getContext(),
                fragment, PreviouslyConnectedDevicePreferenceController.this);
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        mPreferenceSize++;
        if (mPreferenceSize <= MAX_DEVICE_NUM) {
            mPreferenceGroup.addPreference(preference);
        }
        updatePreferenceVisiblity();
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mPreferenceSize--;
        mPreferenceGroup.removePreference(preference);
        updatePreferenceVisiblity();
    }

    @VisibleForTesting
    void setBluetoothDeviceUpdater(BluetoothDeviceUpdater bluetoothDeviceUpdater) {
        mBluetoothDeviceUpdater = bluetoothDeviceUpdater;
    }

    @VisibleForTesting
    void setSavedDockUpdater(DockUpdater savedDockUpdater) {
        mSavedDockUpdater = savedDockUpdater;
    }

    @VisibleForTesting
    void setPreferenceGroup(PreferenceGroup preferenceGroup) {
        mPreferenceGroup = preferenceGroup;
    }

    @VisibleForTesting
    void updatePreferenceVisiblity() {
        mPreferenceGroup.setVisible(mPreferenceSize > 0);
    }
}
