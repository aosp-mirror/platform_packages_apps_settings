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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.BidiFormatter;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Controller that shows and updates the bluetooth device name
 */
public class BluetoothDeviceNamePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "BluetoothNamePrefCtrl";

    public static final String KEY_DEVICE_NAME = "device_name";


    @VisibleForTesting
    Preference mPreference;
    private LocalBluetoothManager mLocalManager;
    private LocalBluetoothAdapter mLocalAdapter;

    public BluetoothDeviceNamePreferenceController(Context context, Lifecycle lifecycle) {
        this(context, (LocalBluetoothAdapter) null);

        mLocalManager = Utils.getLocalBtManager(context);
        if (mLocalManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mLocalAdapter = mLocalManager.getBluetoothAdapter();
        lifecycle.addObserver(this);
    }

    @VisibleForTesting
    BluetoothDeviceNamePreferenceController(Context context, LocalBluetoothAdapter localAdapter) {
        super(context);
        mLocalAdapter = localAdapter;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreference = screen.findPreference(getPreferenceKey());
        super.displayPreference(screen);
    }

    @Override
    public void onStart() {
        mContext.registerReceiver(mReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED));
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public boolean isAvailable() {
        return mLocalAdapter != null;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DEVICE_NAME;
    }

    @Override
    public void updateState(Preference preference) {
        updateDeviceName(preference, mLocalAdapter.getName());
    }

    /**
     * Create preference to show bluetooth device name
     *
     * @param screen to add the preference in
     * @param order  to decide position of the preference
     * @return bluetooth preference that created in this method
     */
    public Preference createBluetoothDeviceNamePreference(PreferenceScreen screen, int order) {
        mPreference = new Preference(screen.getContext());
        mPreference.setOrder(order);
        mPreference.setKey(KEY_DEVICE_NAME);
        screen.addPreference(mPreference);

        return mPreference;
    }

    /**
     * Update device summary with {@code deviceName}, where {@code deviceName} has accent color
     *
     * @param preference to set the summary for
     * @param deviceName bluetooth device name to show in the summary
     */
    protected void updateDeviceName(final Preference preference, final String deviceName) {
        if (deviceName == null) {
            // TODO: show error message in preference subtitle
            return;
        }
        final CharSequence summary = TextUtils.expandTemplate(
                mContext.getText(R.string.bluetooth_device_name_summary),
                BidiFormatter.getInstance().unicodeWrap(deviceName));
        preference.setSelectable(false);
        preference.setSummary(summary);
    }

    /**
     * Receiver that listens to {@link BluetoothAdapter#ACTION_LOCAL_NAME_CHANGED} and updates the
     * device name if possible
     */
    @VisibleForTesting
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (TextUtils.equals(action, BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {
                if (mPreference != null && mLocalAdapter != null && mLocalAdapter.isEnabled()) {
                    updateDeviceName(mPreference, mLocalAdapter.getName());
                }
            }
        }
    };
}
