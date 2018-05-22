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
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import android.text.BidiFormatter;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Controller that shows and updates the bluetooth device name
 */
public class BluetoothDeviceNamePreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "BluetoothNamePrefCtrl";

    @VisibleForTesting
    Preference mPreference;
    private LocalBluetoothManager mLocalManager;
    protected LocalBluetoothAdapter mLocalAdapter;

    /**
     * Constructor exclusively used for Slice.
     */
    public BluetoothDeviceNamePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mLocalManager = Utils.getLocalBtManager(context);
        if (mLocalManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mLocalAdapter = mLocalManager.getBluetoothAdapter();
    }

    @VisibleForTesting
    BluetoothDeviceNamePreferenceController(Context context, LocalBluetoothAdapter localAdapter,
            String preferenceKey) {
        super(context, preferenceKey);
        mLocalAdapter = localAdapter;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreference = screen.findPreference(getPreferenceKey());
        super.displayPreference(screen);
    }

    @Override
    public void onStart() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public int getAvailabilityStatus() {
        return mLocalAdapter != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        updatePreferenceState(preference);
    }

    @Override
    public CharSequence getSummary() {
        String deviceName = getDeviceName();
        if (TextUtils.isEmpty(deviceName)) {
            return super.getSummary();
        }

        return TextUtils.expandTemplate(
                mContext.getText(R.string.bluetooth_device_name_summary),
                BidiFormatter.getInstance().unicodeWrap(deviceName)).toString();
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
        mPreference.setKey(getPreferenceKey());
        screen.addPreference(mPreference);

        return mPreference;
    }

    /**
     * Update device summary with {@code deviceName}, where {@code deviceName} has accent color
     *
     * @param preference to set the summary for
     */
    protected void updatePreferenceState(final Preference preference) {
        preference.setSelectable(false);
        preference.setSummary(getSummary());
    }

    protected String getDeviceName() {
        return mLocalAdapter.getName();
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
                    updatePreferenceState(mPreference);
                }
            } else if (TextUtils.equals(action, BluetoothAdapter.ACTION_STATE_CHANGED)) {
                updatePreferenceState(mPreference);
            }
        }
    };
}
