/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Intent;
import android.util.FeatureFlagUtils;

import androidx.preference.PreferenceFragmentCompat;

import com.android.settings.ButtonBarHandler;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.wifi.p2p.WifiP2pSettings;
import com.android.settings.wifi.savedaccesspoints.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.savedaccesspoints2.SavedAccessPointsWifiSettings2;

public class WifiPickerActivity extends SettingsActivity implements ButtonBarHandler {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        if (!modIntent.hasExtra(EXTRA_SHOW_FRAGMENT)) {
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getWifiSettingsClass().getName());
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, R.string.wifi_select_network);
        }
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        boolean isSavedAccessPointsWifiSettings;
        if (FeatureFlagUtils.isEnabled(this, FeatureFlagUtils.SETTINGS_WIFITRACKER2)) {
            isSavedAccessPointsWifiSettings =
                    SavedAccessPointsWifiSettings2.class.getName().equals(fragmentName);
        } else {
            isSavedAccessPointsWifiSettings =
                    SavedAccessPointsWifiSettings.class.getName().equals(fragmentName);
        }

        if (WifiSettings.class.getName().equals(fragmentName)
                || WifiP2pSettings.class.getName().equals(fragmentName)
                || isSavedAccessPointsWifiSettings) {
            return true;
        }
        return false;
    }

    /* package */ Class<? extends PreferenceFragmentCompat> getWifiSettingsClass() {
        return WifiSettings.class;
    }
}
