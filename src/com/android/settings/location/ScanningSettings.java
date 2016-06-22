/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.location;

import android.provider.Settings.Global;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * A page that configures the background scanning settings for Wi-Fi and Bluetooth.
 */
public class ScanningSettings extends SettingsPreferenceFragment {
    private static final String KEY_WIFI_SCAN_ALWAYS_AVAILABLE = "wifi_always_scanning";
    private static final String KEY_BLUETOOTH_SCAN_ALWAYS_AVAILABLE = "bluetooth_always_scanning";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.LOCATION_SCANNING;
    }

    @Override
    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_scanning);
        root = getPreferenceScreen();
        initPreferences();
        return root;
    }

    private void initPreferences() {
        final SwitchPreference wifiScanAlwaysAvailable =
            (SwitchPreference) findPreference(KEY_WIFI_SCAN_ALWAYS_AVAILABLE);
        wifiScanAlwaysAvailable.setChecked(Global.getInt(getContentResolver(),
                    Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0) == 1);
        final SwitchPreference bleScanAlwaysAvailable =
            (SwitchPreference) findPreference(KEY_BLUETOOTH_SCAN_ALWAYS_AVAILABLE);
        bleScanAlwaysAvailable.setChecked(Global.getInt(getContentResolver(),
                    Global.BLE_SCAN_ALWAYS_AVAILABLE, 0) == 1);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_WIFI_SCAN_ALWAYS_AVAILABLE.equals(key)) {
            Global.putInt(getContentResolver(),
                    Global.WIFI_SCAN_ALWAYS_AVAILABLE,
                    ((SwitchPreference) preference).isChecked() ? 1 : 0);
        } else if (KEY_BLUETOOTH_SCAN_ALWAYS_AVAILABLE.equals(key)) {
            Global.putInt(getContentResolver(),
                    Global.BLE_SCAN_ALWAYS_AVAILABLE,
                    ((SwitchPreference) preference).isChecked() ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }
}
