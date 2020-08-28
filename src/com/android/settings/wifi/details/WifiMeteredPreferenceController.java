/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.details;

import android.app.backup.BackupManager;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.wifi.WifiDialog;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * {@link AbstractPreferenceController} that controls whether the wifi network is metered or not.
 *
 * Migrating from Wi-Fi SettingsLib to to WifiTrackerLib, this object will be removed in the near
 * future, please develop in
 * {@link com.android.settings.wifi.details2.WifiMeteredPreferenceControlle2}.
 */
public class WifiMeteredPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener, WifiDialog.WifiDialogListener {

    private static final String KEY_WIFI_METERED = "metered";
    private WifiConfiguration mWifiConfiguration;
    private WifiManager mWifiManager;
    private Preference mPreference;

    public WifiMeteredPreferenceController(Context context, WifiConfiguration wifiConfiguration) {
        super(context, KEY_WIFI_METERED);
        mWifiConfiguration = wifiConfiguration;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public void updateState(Preference preference) {
        final DropDownPreference dropDownPreference = (DropDownPreference) preference;
        final int meteredOverride = getMeteredOverride();
        dropDownPreference.setValue(Integer.toString(meteredOverride));
        updateSummary(dropDownPreference, meteredOverride);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mWifiConfiguration != null) {
            mWifiConfiguration.meteredOverride = Integer.parseInt((String) newValue);
        }
        mWifiManager.updateNetwork(mWifiConfiguration);
        // Stage the backup of the SettingsProvider package which backs this up
        BackupManager.dataChanged("com.android.providers.settings");
        updateSummary((DropDownPreference) preference, getMeteredOverride());
        return true;
    }

    @VisibleForTesting
    int getMeteredOverride() {
        if (mWifiConfiguration != null) {
            // Wrap the meteredOverride since robolectric cannot recognize it
            return mWifiConfiguration.meteredOverride;
        }
        return WifiConfiguration.METERED_OVERRIDE_NONE;
    }

    private void updateSummary(DropDownPreference preference, int meteredOverride) {
        preference.setSummary(preference.getEntries()[meteredOverride]);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onSubmit(WifiDialog dialog) {
        if (dialog.getController() != null) {
            final WifiConfiguration newConfig = dialog.getController().getConfig();
            if (newConfig == null || mWifiConfiguration == null) {
                return;
            }

            if (newConfig.meteredOverride != mWifiConfiguration.meteredOverride) {
                mWifiConfiguration = newConfig;
                onPreferenceChange(mPreference, String.valueOf(newConfig.meteredOverride));
            }
        }
    }
}
