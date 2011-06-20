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

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class AdvancedBluetoothSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_BT_DISCOVERABLE = "bt_discoverable";
    private static final String KEY_BT_DISCOVERABLE_TIMEOUT = "bt_discoverable_timeout";
    private static final String KEY_BT_NAME = "bt_name";
    private static final String KEY_BT_SHOW_RECEIVED = "bt_show_received_files";

    /* Private intent to show the list of received files */
    private static final String BTOPP_ACTION_OPEN_RECEIVED_FILES =
            "android.btopp.intent.action.OPEN_RECEIVED_FILES";

    private BluetoothDiscoverableEnabler mDiscoverableEnabler;
    private BluetoothNamePreference mNamePreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.bluetooth_advanced_settings);

        LocalBluetoothManager localManager = LocalBluetoothManager.getInstance(getActivity());
        if (localManager != null) {
            LocalBluetoothAdapter localAdapter = localManager.getBluetoothAdapter();
            mDiscoverableEnabler = new BluetoothDiscoverableEnabler(getActivity(),
                    localAdapter,
                    (CheckBoxPreference) findPreference(KEY_BT_DISCOVERABLE),
                    (ListPreference) findPreference(KEY_BT_DISCOVERABLE_TIMEOUT));
        }

        mNamePreference = (BluetoothNamePreference) findPreference(KEY_BT_NAME);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDiscoverableEnabler.resume();
        mNamePreference.resume();
    }

    @Override
    public void onPause() {
        super.onPause();

        mNamePreference.pause();
        mDiscoverableEnabler.pause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (KEY_BT_SHOW_RECEIVED.equals(preference.getKey())) {
            Intent intent = new Intent(BTOPP_ACTION_OPEN_RECEIVED_FILES);
            getActivity().sendBroadcast(intent);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }
}
