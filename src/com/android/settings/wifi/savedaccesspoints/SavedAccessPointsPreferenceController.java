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

package com.android.settings.wifi.savedaccesspoints;

import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.AccessPointPreference.UserBadgeCache;
import com.android.settingslib.wifi.WifiSavedConfigUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller that manages a PreferenceGroup, which contains a list of saved access points.
 *
 * Migrating from Wi-Fi SettingsLib to to WifiTrackerLib, this object will be removed in the near
 * future, please develop in
 * {@link com.android.settings.wifi.savedaccesspoints2.SavedAccessPointsPreferenceController2}.
 */
public class SavedAccessPointsPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceClickListener {

    protected final WifiManager mWifiManager;
    private final UserBadgeCache mUserBadgeCache;
    private PreferenceGroup mPreferenceGroup;
    private SavedAccessPointsWifiSettings mHost;
    @VisibleForTesting
    List<AccessPoint> mAccessPoints;

    public SavedAccessPointsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUserBadgeCache = new AccessPointPreference.UserBadgeCache(context.getPackageManager());
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    public SavedAccessPointsPreferenceController setHost(SavedAccessPointsWifiSettings host) {
        mHost = host;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        refreshSavedAccessPoints();
        return mAccessPoints.size() > 0 ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceGroup = screen.findPreference(getPreferenceKey());
        refreshSavedAccessPoints();
        updatePreference();
        super.displayPreference(screen);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mHost != null) {
            final Preference preferenceInGroup =
                    mPreferenceGroup.findPreference(preference.getKey());
            mHost.showWifiPage((AccessPointPreference) preferenceInGroup);
        }
        return false;
    }

    protected void refreshSavedAccessPoints() {
        mAccessPoints = WifiSavedConfigUtils.getAllConfigs(mContext, mWifiManager).stream()
                .filter(accessPoint -> !accessPoint.isPasspointConfig())
                .sorted(SavedNetworkComparator.INSTANCE)
                .collect(Collectors.toList());
    }

    private void updatePreference() {
        mPreferenceGroup.removeAll();
        for (AccessPoint accessPoint : mAccessPoints) {
            final String key = accessPoint.getKey();

            final AccessPointPreference preference = new AccessPointPreference(accessPoint,
                    mContext, mUserBadgeCache, true /* forSavedNetworks */);
            preference.setKey(key);
            preference.setIcon(null);
            preference.setOnPreferenceClickListener(this);

            mPreferenceGroup.addPreference(preference);
        }
    }
}
