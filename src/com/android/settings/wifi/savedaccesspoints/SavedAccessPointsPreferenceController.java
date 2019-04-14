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
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.utils.PreferenceGroupChildrenCache;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.AccessPointPreference.UserBadgeCache;
import com.android.settingslib.wifi.WifiSavedConfigUtils;

import java.util.Collections;
import java.util.List;

/**
 * Controller that manages a PreferenceGroup, which contains a list of saved access points.
 */
public class SavedAccessPointsPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, Preference.OnPreferenceClickListener,
        WifiManager.ActionListener {

    private static final String TAG = "SavedAPPrefCtrl";

    private final WifiManager mWifiManager;
    private final PreferenceGroupChildrenCache mChildrenCache;

    private final UserBadgeCache mUserBadgeCache;
    private PreferenceGroup mPreferenceGroup;
    private SavedAccessPointsWifiSettings mHost;

    public SavedAccessPointsPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mUserBadgeCache = new AccessPointPreference.UserBadgeCache(context.getPackageManager());
        mWifiManager = context.getSystemService(WifiManager.class);
        mChildrenCache = new PreferenceGroupChildrenCache();
    }

    public SavedAccessPointsPreferenceController setHost(SavedAccessPointsWifiSettings host) {
        mHost = host;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        refreshSavedAccessPoints();
    }

    public void postRefreshSavedAccessPoints() {
        ThreadUtils.postOnMainThread(() -> refreshSavedAccessPoints());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mHost != null) {
            mHost.showWifiPage((AccessPointPreference) preference);
        }
        return false;
    }

    @Override
    public void onSuccess() {
        postRefreshSavedAccessPoints();
    }

    @Override
    public void onFailure(int reason) {
        postRefreshSavedAccessPoints();
    }

    @VisibleForTesting
    void refreshSavedAccessPoints() {
        if (mPreferenceGroup == null) {
            Log.w(TAG, "PreferenceGroup is null, skipping.");
            return;
        }
        final Context prefContext = mPreferenceGroup.getContext();

        final List<AccessPoint> accessPoints =
                WifiSavedConfigUtils.getAllConfigs(mContext, mWifiManager);
        Collections.sort(accessPoints, SavedNetworkComparator.INSTANCE);
        mChildrenCache.cacheRemoveAllPrefs(mPreferenceGroup);

        final int accessPointsSize = accessPoints.size();
        for (int i = 0; i < accessPointsSize; ++i) {
            AccessPoint ap = accessPoints.get(i);

            if (mHost != null && mHost.isSubscriptionsFeatureEnabled()
                    && ap.isPasspointConfig()) {
                continue;
            }

            String key = ap.getKey();
            AccessPointPreference preference =
                    (AccessPointPreference) mChildrenCache.getCachedPreference(key);
            if (preference == null) {
                preference = new AccessPointPreference(ap, prefContext, mUserBadgeCache, true);
                preference.setKey(key);
                preference.setIcon(null);
                preference.setOnPreferenceClickListener(this);
                mPreferenceGroup.addPreference(preference);
            }
            preference.setOrder(i);
        }

        mChildrenCache.removeCachedPrefs(mPreferenceGroup);

        if (mPreferenceGroup.getPreferenceCount() < 1) {
            Log.w(TAG, "Saved networks activity loaded, but there are no saved networks!");
            mPreferenceGroup.setVisible(false);
        } else {
            mPreferenceGroup.setVisible(true);
        }

        if (mHost != null && !mHost.isSubscriptionsFeatureEnabled()) {
            mPreferenceGroup.setVisible(true);
            mPreferenceGroup.setTitle(null);
            mPreferenceGroup.setLayoutResource(R.layout.preference_category_no_label);
        }
    }
}
