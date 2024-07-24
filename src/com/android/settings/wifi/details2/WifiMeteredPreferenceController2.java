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

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.wifitrackerlib.WifiEntry;

/**
 * A controller that controls whether the Wi-Fi network is metered or not.
 */
public class WifiMeteredPreferenceController2 extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_WIFI_METERED = "metered";
    private final WifiEntry mWifiEntry;

    public WifiMeteredPreferenceController2(Context context, WifiEntry wifiEntry) {
        super(context, KEY_WIFI_METERED);
        mWifiEntry = wifiEntry;
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        final int meteredOverride = getMeteredOverride();
        preference.setSelectable(mWifiEntry.canSetMeteredChoice());
        listPreference.setValue(Integer.toString(meteredOverride));
        updateSummary(listPreference, meteredOverride);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (mWifiEntry.isSaved() || mWifiEntry.isSubscription()) {
            mWifiEntry.setMeteredChoice(Integer.parseInt((String) newValue));
        }

        // Stage the backup of the SettingsProvider package which backs this up
        BackupManager.dataChanged("com.android.providers.settings");
        updateSummary((ListPreference) preference, getMeteredOverride());
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

    private void updateSummary(ListPreference preference, int meteredOverride) {
        preference.setSummary(preference.getEntries()[meteredOverride]);
    }
}
