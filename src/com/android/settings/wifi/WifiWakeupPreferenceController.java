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

package com.android.settings.wifi;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * {@link PreferenceControllerMixin} that controls whether the Wi-Fi Wakeup feature should be
 * enabled.
 */
public class WifiWakeupPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_ENABLE_WIFI_WAKEUP = "enable_wifi_wakeup";

    public WifiWakeupPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
    }

    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_ENABLE_WIFI_WAKEUP)) {
            return false;
        }
        if (!(preference instanceof SwitchPreference)) {
            return false;
        }
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.WIFI_WAKEUP_ENABLED,
                ((SwitchPreference) preference).isChecked() ? 1 : 0);
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ENABLE_WIFI_WAKEUP;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        final SwitchPreference enableWifiWakeup = (SwitchPreference) preference;

        enableWifiWakeup.setChecked(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.WIFI_WAKEUP_ENABLED, 0) == 1);

        boolean wifiScanningEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0) == 1;
        enableWifiWakeup.setEnabled(wifiScanningEnabled);

        if (wifiScanningEnabled) {
            enableWifiWakeup.setSummary(R.string.wifi_wakeup_summary);
        } else {
            enableWifiWakeup.setSummary(R.string.wifi_wakeup_summary_scanning_disabled);
        }
    }
}
