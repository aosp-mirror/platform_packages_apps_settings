/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;

import com.android.settings.R;

/**
 * BluetoothSettings is the Settings screen for Bluetooth configuration and
 * connection management.
 */
public final class BluetoothSettings extends DeviceListPreferenceFragment {
    private static final String TAG = "BluetoothSettings";

    private static final String KEY_BT_CHECKBOX = "bt_checkbox";
    private static final String KEY_BT_DISCOVERABLE = "bt_discoverable";
    private static final String KEY_BT_DISCOVERABLE_TIMEOUT = "bt_discoverable_timeout";
    private static final String KEY_BT_NAME = "bt_name";
    private static final String KEY_BT_SHOW_RECEIVED = "bt_show_received_files";

    private BluetoothEnabler mEnabler;
    private BluetoothDiscoverableEnabler mDiscoverableEnabler;
    private BluetoothNamePreference mNamePreference;

    /* Private intent to show the list of received files */
    private static final String BTOPP_ACTION_OPEN_RECEIVED_FILES =
            "android.btopp.intent.action.OPEN_RECEIVED_FILES";

    /** Initialize the filter to show bonded devices only. */
    public BluetoothSettings() {
        super(BluetoothDeviceFilter.BONDED_DEVICE_FILTER);
    }

    @Override
    void addPreferencesForActivity() {
        addPreferencesFromResource(R.xml.bluetooth_settings);

        mEnabler = new BluetoothEnabler(getActivity(),
                (CheckBoxPreference) findPreference(KEY_BT_CHECKBOX));

        mDiscoverableEnabler = new BluetoothDiscoverableEnabler(getActivity(),
                mLocalAdapter,
                (CheckBoxPreference) findPreference(KEY_BT_DISCOVERABLE),
                    (ListPreference) findPreference(KEY_BT_DISCOVERABLE_TIMEOUT));

        mNamePreference = (BluetoothNamePreference) findPreference(KEY_BT_NAME);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Repopulate (which isn't too bad since it's cached in the settings
        // bluetooth manager)
        addDevices();

        mEnabler.resume();
        mDiscoverableEnabler.resume();
        mNamePreference.resume();
    }

    @Override
    public void onPause() {
        super.onPause();

        mNamePreference.pause();
        mDiscoverableEnabler.pause();
        mEnabler.pause();
    }

    private final View.OnClickListener mListener = new View.OnClickListener() {
        public void onClick(View v) {
            // User clicked on advanced options icon for a device in the list
            if (v.getTag() instanceof CachedBluetoothDevice) {
                CachedBluetoothDevice
                        device = (CachedBluetoothDevice) v.getTag();

                Preference pref = new Preference(getActivity());
                pref.setTitle(device.getName());
                pref.setFragment(DeviceProfilesSettings.class.getName());
                pref.getExtras().putParcelable(DeviceProfilesSettings.EXTRA_DEVICE,
                        device.getDevice());
                ((PreferenceActivity) getActivity())
                        .onPreferenceStartFragment(BluetoothSettings.this,
                                pref);
            } else {
                Log.w(TAG, "onClick() called for other View: " + v);
            }
        }
    };

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (KEY_BT_SHOW_RECEIVED.equals(preference.getKey())) {
            Intent intent = new Intent(BTOPP_ACTION_OPEN_RECEIVED_FILES);
            getActivity().sendBroadcast(intent);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice,
            int bondState) {
        if (bondState == BluetoothDevice.BOND_BONDED) {
            // add to "Paired devices" list after remote-initiated pairing
            if (mDevicePreferenceMap.get(cachedDevice) == null) {
                createDevicePreference(cachedDevice);
            }
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            // remove unpaired device from paired devices list
            onDeviceDeleted(cachedDevice);
        }
    }

    /**
     * Add a listener, which enables the advanced settings icon.
     * @param preference the newly added preference
     */
    @Override
    void initDevicePreference(BluetoothDevicePreference preference) {
        preference.setOnSettingsClickListener(mListener);
    }
}
