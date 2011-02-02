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

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.android.settings.R;

/**
 * BluetoothSettings is the Settings screen for Bluetooth configuration and
 * connection management.
 */
public class BluetoothSettings extends DeviceListPreferenceFragment
        implements LocalBluetoothManager.Callback, View.OnClickListener {

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

    void addPreferencesForActivity(Activity activity) {
        addPreferencesFromResource(R.xml.bluetooth_settings);

        mEnabler = new BluetoothEnabler(activity,
                (CheckBoxPreference) findPreference(KEY_BT_CHECKBOX));

        mDiscoverableEnabler = new BluetoothDiscoverableEnabler(activity,
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
                if (addDevicePreference(cachedDevice)) {
                    createDevicePreference(cachedDevice);
                }
            }
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            // remove unpaired device from paired devices list
            onDeviceDeleted(cachedDevice);
        }
    }

    /**
     * Additional check to only add paired devices to list.
     */
    boolean addDevicePreference(CachedBluetoothDevice cachedDevice) {
        if (cachedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            return super.addDevicePreference(cachedDevice);
        } else {
            return false;
        }
    }

    /**
     * Add a listener, which enables the advanced settings icon.
     * @param preference the newly added preference
     */
    void initDevicePreference(BluetoothDevicePreference preference) {
        preference.setOnSettingsClickListener(this);
    }
}
