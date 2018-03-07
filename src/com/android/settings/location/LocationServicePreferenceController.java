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
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.widget.RestrictedAppPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;

public class LocationServicePreferenceController extends LocationBasePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    private static final String TAG = "LocationServicePrefCtrl";
    /** Key for preference category "Location services" */
    private static final String KEY_LOCATION_SERVICES = "location_services";
    @VisibleForTesting
    static final IntentFilter INTENT_FILTER_INJECTED_SETTING_CHANGED =
            new IntentFilter(SettingInjectorService.ACTION_INJECTED_SETTING_CHANGED);

    private PreferenceCategory mCategoryLocationServices;
    private final LocationSettings mFragment;
    private final SettingsInjector mInjector;
    /** Receives UPDATE_INTENT  */
    @VisibleForTesting
    BroadcastReceiver mInjectedSettingsReceiver;

    public LocationServicePreferenceController(Context context, LocationSettings fragment,
            Lifecycle lifecycle) {
        this(context, fragment, lifecycle, new SettingsInjector(context));
    }

    @VisibleForTesting
    LocationServicePreferenceController(Context context, LocationSettings fragment,
            Lifecycle lifecycle, SettingsInjector injector) {
        super(context, lifecycle);
        mFragment = fragment;
        mInjector = injector;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LOCATION_SERVICES;
    }

    @Override
    public boolean isAvailable() {
        // If managed profile has lock-down on location access then its injected location services
        // must not be shown.
        return mInjector.hasInjectedSettings(mLocationEnabler.isManagedProfileRestrictedByBase()
                ? UserHandle.myUserId() : UserHandle.USER_CURRENT);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCategoryLocationServices =
                (PreferenceCategory) screen.findPreference(KEY_LOCATION_SERVICES);
    }

    @Override
    public void updateState(Preference preference) {
        mCategoryLocationServices.removeAll();
        final List<Preference> prefs = getLocationServices();
        for (Preference pref : prefs) {
            if (pref instanceof RestrictedAppPreference) {
                ((RestrictedAppPreference) pref).checkRestrictionAndSetDisabled();
            }
        }
        LocationSettings.addPreferencesSorted(prefs, mCategoryLocationServices);
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

    private List<Preference> getLocationServices() {
        // If location access is locked down by device policy then we only show injected settings
        // for the primary profile.
        return mInjector.getInjectedSettings(mFragment.getPreferenceManager().getContext(),
                mLocationEnabler.isManagedProfileRestrictedByBase()
                        ? UserHandle.myUserId() : UserHandle.USER_CURRENT);
    }
}
