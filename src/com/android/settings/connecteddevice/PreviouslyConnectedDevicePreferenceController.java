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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
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
    private static final String KEY_SEE_ALL = "previously_connected_devices_see_all";

    private PreferenceGroup mPreferenceGroup;
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private DockUpdater mSavedDockUpdater;
    private int mPreferenceSize;
    private BluetoothAdapter mBluetoothAdapter;

    @VisibleForTesting
    Preference mSeeAllPreference;
    @VisibleForTesting
    IntentFilter mIntentFilter;

    @VisibleForTesting
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updatePreferenceVisibility();
        }
    };

    public PreviouslyConnectedDevicePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mSavedDockUpdater = FeatureFactory.getFactory(
                context).getDockUpdaterFeatureProvider().getSavedDockUpdater(context, this);
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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
        mSeeAllPreference = mPreferenceGroup.findPreference(KEY_SEE_ALL);
        updatePreferenceVisibility();

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
        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onStop() {
        mBluetoothDeviceUpdater.unregisterCallback();
        mSavedDockUpdater.unregisterCallback();
        mContext.unregisterReceiver(mReceiver);
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
        updatePreferenceVisibility();
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mPreferenceSize--;
        mPreferenceGroup.removePreference(preference);
        updatePreferenceVisibility();
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
    void updatePreferenceVisibility() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mSeeAllPreference.setSummary("");
        } else {
            mSeeAllPreference.setSummary(
                    mContext.getString(R.string.connected_device_see_all_summary));
        }
    }
}
