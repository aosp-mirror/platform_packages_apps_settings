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

package com.android.settings.wifi.p2p;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class P2pThisDevicePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private Preference mPreference;

    public P2pThisDevicePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "p2p_this_device";
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    public void setEnabled(boolean enabled) {
        if (mPreference != null) {
            mPreference.setEnabled(enabled);
        }
    }

    public void updateDeviceName(WifiP2pDevice thisDevice) {
        if (mPreference != null && thisDevice != null) {
            if (TextUtils.isEmpty(thisDevice.deviceName)) {
                mPreference.setTitle(thisDevice.deviceAddress);
            } else {
                mPreference.setTitle(thisDevice.deviceName);
            }
        }
    }
}
