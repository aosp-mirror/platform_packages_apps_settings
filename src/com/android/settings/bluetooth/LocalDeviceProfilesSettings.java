/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * This preference fragment presents the user with the profiles of the local devices and allow them
 * to be modified.
 */
public final class LocalDeviceProfilesSettings extends SettingsPreferenceFragment {
    private static final String TAG = "LocalDeviceProfilesSettings";

    private static final String KEY_RENAME_DEVICE = "rename_device";
    private static final String KEY_BROADCASTING = "broadcasting";
    private static final String KEY_VISIBILITY_TIMEOUT = "visibility_timeout";

    private LocalBluetoothManager mManager;
    private BluetoothDiscoverableEnabler mDiscoverableEnabler;
    private BluetoothAdvertisingEnabler mAdvertisingEnabler;

    private Preference mDeviceNamePref;
    private Preference mVisibilityPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.bluetooth_local_device_profile);
        getPreferenceScreen().setOrderingAsAdded(true);
        mDeviceNamePref = findPreference(KEY_RENAME_DEVICE);
        mDeviceNamePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new BluetoothNameDialogFragment().show(getFragmentManager(), "rename device");
                return true;
            }
        });

        mVisibilityPref = findPreference(KEY_VISIBILITY_TIMEOUT);
        mVisibilityPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new BluetoothVisibilityTimeoutFragment().show(
                        getFragmentManager(), "visibility timeout");
                return true;
            }
        });

        mManager = LocalBluetoothManager.getInstance(getActivity());
        mDiscoverableEnabler = mManager.getDiscoverableEnabler();
        // Set the visibility timeout preference to the enabler so the visibility timeout
        // preference can be updated when the timeout changes.
        mDiscoverableEnabler.setVisibilityPreference(mVisibilityPref);

        PreferenceScreen bluetoothBroadcast = (PreferenceScreen)findPreference(KEY_BROADCASTING);
        mAdvertisingEnabler = new BluetoothAdvertisingEnabler(getActivity(), bluetoothBroadcast);
        mManager.setBluetoothAdvertisingEnabler(mAdvertisingEnabler);
    }

    @Override
    public void onResume() {
        super.onResume();
        mManager.setForegroundActivity(getActivity());
        mAdvertisingEnabler.resume();
        mDiscoverableEnabler.resume(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        mManager.setForegroundActivity(null);
        if (mDiscoverableEnabler != null) {
            mDiscoverableEnabler.pause();
        }
    }

}
