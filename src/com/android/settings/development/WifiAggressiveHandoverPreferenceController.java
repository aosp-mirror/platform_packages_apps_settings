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

package com.android.settings.development;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

public class WifiAggressiveHandoverPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String WIFI_AGGRESSIVE_HANDOVER_KEY = "wifi_aggressive_handover";

    @VisibleForTesting
    static final int SETTING_VALUE_ON = 1;
    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;

    private final WifiManager mWifiManager;
    private SwitchPreference mPreference;

    public WifiAggressiveHandoverPreferenceController(Context context) {
        super(context);

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public String getPreferenceKey() {
        return WIFI_AGGRESSIVE_HANDOVER_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean isEnabled = (Boolean) newValue;
        mWifiManager.enableAggressiveHandover(isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        boolean enabled = mWifiManager.getAggressiveHandover() > 0;
        mPreference.setChecked(enabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        mWifiManager.enableAggressiveHandover(SETTING_VALUE_OFF);
        mPreference.setChecked(false);
        mPreference.setEnabled(false);
    }
}
