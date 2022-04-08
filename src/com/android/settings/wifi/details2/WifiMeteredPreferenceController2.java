/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.details2;

import android.app.backup.BackupManager;
import android.content.Context;
import android.net.wifi.WifiConfiguration;

import androidx.annotation.VisibleForTesting;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.wifi.WifiDialog2;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.wifitrackerlib.WifiEntry;

/**
 * {@link AbstractPreferenceController} that controls whether the wifi network is metered or not
 */
public class WifiMeteredPreferenceController2 extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener, WifiDialog2.WifiDialog2Listener {

    private static final String KEY_WIFI_METERED = "metered";
    private WifiEntry mWifiEntry;
    private Preference mPreference;

    public WifiMeteredPreferenceController2(Context context, WifiEntry wifiEntry) {
        super(context, KEY_WIFI_METERED);
        mWifiEntry = wifiEntry;
    }

    @Override
    public void updateState(Preference preference) {
        final DropDownPreference dropDownPreference = (DropDownPreference) preference;
        final int meteredOverride = getMeteredOverride();
        preference.setSelectable(mWifiEntry.canSetMeteredChoice());
        dropDownPreference.setValue(Integer.toString(meteredOverride));
        updateSummary(dropDownPreference, meteredOverride);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mWifiEntry.isSaved() || mWifiEntry.isSubscription()) {
            mWifiEntry.setMeteredChoice(Integer.parseInt((String) newValue));
        }

        // Stage the backup of the SettingsProvider package which backs this up
        BackupManager.dataChanged("com.android.providers.settings");
        updateSummary((DropDownPreference) preference, getMeteredOverride());
        return true;
    }

    @VisibleForTesting
    int getMeteredOverride() {
        if (mWifiEntry.isSaved() || mWifiEntry.isSubscription()) {
            // Wrap the meteredOverride since robolectric cannot recognize it
            return mWifiEntry.getMeteredChoice();
        }
        return WifiEntry.METERED_CHOICE_AUTO;
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
    public void onSubmit(WifiDialog2 dialog) {
        if (dialog.getController() != null && mWifiEntry.canSetMeteredChoice()) {
            final WifiConfiguration newConfig = dialog.getController().getConfig();
            if (newConfig == null) {
                return;
            }

            if (getWifiEntryMeteredChoice(newConfig) != mWifiEntry.getMeteredChoice()) {
                mWifiEntry.setMeteredChoice(getWifiEntryMeteredChoice(newConfig));
                onPreferenceChange(mPreference, String.valueOf(newConfig.meteredOverride));
            }
        }
    }

    private int getWifiEntryMeteredChoice(WifiConfiguration wifiConfiguration) {
        switch (wifiConfiguration.meteredOverride) {
            case WifiConfiguration.METERED_OVERRIDE_METERED:
                return WifiEntry.METERED_CHOICE_METERED;
            case WifiConfiguration.METERED_OVERRIDE_NOT_METERED:
                return WifiEntry.METERED_CHOICE_UNMETERED;
            default:
                return WifiEntry.METERED_CHOICE_AUTO;
        }
    }
}
