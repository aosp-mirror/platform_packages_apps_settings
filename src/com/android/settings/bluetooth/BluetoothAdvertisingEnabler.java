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

import android.content.Context;
import android.preference.PreferenceScreen;

import com.android.settings.R;

/**
 * BluetoothAdvertisingEnabler helps manager change of bluetooth advertising preferences.
 */
final class BluetoothAdvertisingEnabler {

    private final Context mContext;
    private final PreferenceScreen mBluetoothAdvertisingPreference;

    public BluetoothAdvertisingEnabler(Context context, PreferenceScreen bluetoothBroadcast) {
        mContext = context;
        mBluetoothAdvertisingPreference = bluetoothBroadcast;
    }

    public void resume() {
        boolean isBroadcastingEnable = LocalBluetoothPreferences.isAdvertisingEnabled(mContext);
        handleAdvertisingStateChange(isBroadcastingEnable);
    }

    private void handleAdvertisingStateChange(boolean isBroadcastingEnable) {
        mBluetoothAdvertisingPreference.setSummary(isBroadcastingEnable ?
                R.string.bluetooth_broadcasting_state_on :
                    R.string.bluetooth_broadcasting_state_off);
    }
}
