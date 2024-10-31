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

package com.android.settings.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.SettingsMainSwitchPreferenceController;

/**
 * {@link SettingsMainSwitchPreferenceController}
 * that controls whether Adaptive connectivity option is enabled.
 */
// LINT.IfChange
public class AdaptiveConnectivityTogglePreferenceController extends
        SettingsMainSwitchPreferenceController {

    private final WifiManager mWifiManager;

    public AdaptiveConnectivityTogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 1) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED,
                isChecked ? 1 : 0);
        mWifiManager.setWifiScoringEnabled(isChecked);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_network;
    }
}
// LINT.ThenChange(AdaptiveConnectivityTogglePreference.kt)
