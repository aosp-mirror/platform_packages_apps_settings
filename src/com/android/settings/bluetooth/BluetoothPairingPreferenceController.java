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

import android.app.Fragment;
import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceController;
import com.android.settings.R;


/**
 * Controller that shows and updates the bluetooth device name
 */
public class BluetoothPairingPreferenceController extends PreferenceController {
    private static final String TAG = "BluetoothPairingPrefCtrl";

    public static final String KEY_PAIRING = "pref_bt_pairing";
    private PreferenceFragment mFragment;
    private Preference mPreference;

    public BluetoothPairingPreferenceController(Context context, PreferenceFragment fragment) {
       super(context);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PAIRING;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_PAIRING.equals(preference.getKey())) {
            //TODO: open the pairing page
            return true;
        }

        return false;
    }

    /**
     * Create pairing preference to jump to pairing page
     *
     * @return bluetooth preference that created in this method
     */
    public Preference createBluetoothPairingPreference() {
        mPreference = new Preference(mFragment.getPreferenceScreen().getContext());
        mPreference.setKey(KEY_PAIRING);
        mPreference.setIcon(R.drawable.ic_add);
        mPreference.setTitle(R.string.bluetooth_pairing_pref_title);

        return mPreference;
    }

}
