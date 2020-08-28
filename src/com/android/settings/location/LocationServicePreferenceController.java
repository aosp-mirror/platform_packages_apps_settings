/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.SettingInjectorService;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.RestrictedAppPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;
import java.util.Map;

public class LocationServicePreferenceController extends LocationBasePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    private static final String TAG = "LocationPrefCtrl";
    @VisibleForTesting
    static final IntentFilter INTENT_FILTER_INJECTED_SETTING_CHANGED =
            new IntentFilter(SettingInjectorService.ACTION_INJECTED_SETTING_CHANGED);

    protected PreferenceCategory mCategoryLocationServices;
    @VisibleForTesting
    AppSettingsInjector mInjector;
    /** Receives UPDATE_INTENT */
    @VisibleForTesting
    BroadcastReceiver mInjectedSettingsReceiver;

    public LocationServicePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void init(DashboardFragment fragment) {
        super.init(fragment);
        mInjector = new AppSettingsInjector(mContext, getMetricsCategory());
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCategoryLocationServices = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        mCategoryLocationServices.removeAll();
        final Map<Integer, List<Preference>> prefs = getLocationServices();
        boolean show = false;
        for (Map.Entry<Integer, List<Preference>> entry : prefs.entrySet()) {
            for (Preference pref : entry.getValue()) {
                if (pref instanceof RestrictedAppPreference) {
                    ((RestrictedAppPreference) pref).checkRestrictionAndSetDisabled();
                }
            }
            if (entry.getKey() == UserHandle.myUserId()) {
                if (mCategoryLocationServices != null) {
                    LocationSettings.addPreferencesSorted(entry.getValue(),
                            mCategoryLocationServices);
                }
                show = true;
            }
        }
        mCategoryLocationServices.setVisible(show);
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        // As a safety measure, also reloads on location mode change to ensure the settings are
        // up-to-date even if an affected app doesn't send the setting changed broadcast.
        mInjector.reloadStatusMessages();
    }

    @Override
    public void onResume() {
        if (mInjectedSettingsReceiver == null) {
            mInjectedSettingsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Received settings change intent: " + intent);
                    }
                    mInjector.reloadStatusMessages();
                }
            };
        }
        mContext.registerReceiver(
                mInjectedSettingsReceiver, INTENT_FILTER_INJECTED_SETTING_CHANGED);
    }

    @Override
    public void onPause() {
        mContext.unregisterReceiver(mInjectedSettingsReceiver);
    }

    protected Map<Integer, List<Preference>> getLocationServices() {
        // If location access is locked down by device policy then we only show injected settings
        // for the primary profile.
        final int profileUserId = Utils.getManagedProfileId(mUserManager, UserHandle.myUserId());

        return mInjector.getInjectedSettings(mFragment.getPreferenceManager().getContext(),
                (profileUserId != UserHandle.USER_NULL
                        && mLocationEnabler.getShareLocationEnforcedAdmin(profileUserId) != null)
                        ? UserHandle.myUserId() : UserHandle.USER_CURRENT);
    }
}
