/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.wifi.tether;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;

/**
 * Shows hotspot footer information.
 */
public class WifiTetherFooterPreferenceController extends WifiTetherBasePreferenceController {

    private static final String PREF_KEY = "tether_prefs_footer_2";

    public WifiTetherFooterPreferenceController(Context context) {
        super(context, null /* listener */);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        if (mWifiManager.isStaApConcurrencySupported()) {
            mPreference.setTitle(R.string.tethering_footer_info_sta_ap_concurrency);
        } else {
            mPreference.setTitle(R.string.tethering_footer_info);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }
}
