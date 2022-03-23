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

package com.android.settings.wifi.savedaccesspoints2;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.wifi.WifiEntryPreference;
import com.android.wifitrackerlib.WifiEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller that manages a PreferenceGroup, which contains a list of saved access points.
 */
public class SavedAccessPointsPreferenceController2 extends BasePreferenceController implements
        Preference.OnPreferenceClickListener {

    private PreferenceGroup mPreferenceGroup;
    private SavedAccessPointsWifiSettings2 mHost;
    @VisibleForTesting
    List<WifiEntry> mWifiEntries = new ArrayList<>();

    public SavedAccessPointsPreferenceController2(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * Set {@link SavedAccessPointsWifiSettings2} for click callback action.
     */
    public SavedAccessPointsPreferenceController2 setHost(SavedAccessPointsWifiSettings2 host) {
        mHost = host;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        return mWifiEntries.size() > 0 ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceGroup = screen.findPreference(getPreferenceKey());
        updatePreference();
        super.displayPreference(screen);
    }

    void displayPreference(PreferenceScreen screen, List<WifiEntry> wifiEntries) {
        if (wifiEntries == null || wifiEntries.isEmpty()) {
            mWifiEntries.clear();
        } else {
            mWifiEntries = wifiEntries;
        }

        displayPreference(screen);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mHost != null) {
            mHost.showWifiPage(preference.getKey(), preference.getTitle());
        }
        return false;
    }

    /**
     * mPreferenceGroup is not in a RecyclerView. To keep TalkBack focus, this method should not
     * mPreferenceGroup.removeAll() then mPreferenceGroup.addPreference for mWifiEntries.
     */
    private void updatePreference() {
        // Remove the Preference of removed WifiEntry.
        final List<String> removedPreferenceKeys = new ArrayList<>();
        final int preferenceCount = mPreferenceGroup.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            final String key = mPreferenceGroup.getPreference(i).getKey();
            if (mWifiEntries.stream().filter(wifiEntry ->
                    TextUtils.equals(key, wifiEntry.getKey())).count() == 0) {
                removedPreferenceKeys.add(key);
            }
        }
        for (String removedPreferenceKey : removedPreferenceKeys) {
            mPreferenceGroup.removePreference(
                    mPreferenceGroup.findPreference(removedPreferenceKey));
        }

        // Add the Preference of new added WifiEntry.
        for (WifiEntry wifiEntry : mWifiEntries) {
            if (mPreferenceGroup.findPreference(wifiEntry.getKey()) == null) {
                final WifiEntryPreference preference = new WifiEntryPreference(mContext, wifiEntry);
                preference.setKey(wifiEntry.getKey());
                preference.setOnPreferenceClickListener(this);

                mPreferenceGroup.addPreference(preference);
            }
        }
    }
}
