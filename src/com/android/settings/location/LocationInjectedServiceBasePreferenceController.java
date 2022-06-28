/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.SettingInjectorService;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

import java.util.List;
import java.util.Map;

/**
 * A abstract class which is responsible for creating the injected location services items.
 * Developer needs to implement the {@link #injectLocationServices(PreferenceScreen)}.
 */
public abstract class LocationInjectedServiceBasePreferenceController
        extends LocationBasePreferenceController implements LifecycleObserver{

    private static final String TAG = "LocationPrefCtrl";
    @VisibleForTesting
    static final IntentFilter INTENT_FILTER_INJECTED_SETTING_CHANGED =
            new IntentFilter(SettingInjectorService.ACTION_INJECTED_SETTING_CHANGED);

    @VisibleForTesting
    AppSettingsInjector mInjector;
    /** Receives UPDATE_INTENT */
    @VisibleForTesting
    BroadcastReceiver mInjectedSettingsReceiver;

    public LocationInjectedServiceBasePreferenceController(Context context, String key) {
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
        injectLocationServices(screen);
    }

    /**
     * Override this method to inject location services in preference screen.
     * @param screen
     */
    protected abstract void injectLocationServices(PreferenceScreen screen);

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        // As a safety measure, also reloads on location mode change to ensure the settings are
        // up-to-date even if an affected app doesn't send the setting changed broadcast.
        mInjector.reloadStatusMessages();
    }

    /** @OnLifecycleEvent(ON_RESUME) */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
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
                mInjectedSettingsReceiver, INTENT_FILTER_INJECTED_SETTING_CHANGED,
                Context.RECEIVER_EXPORTED_UNAUDITED);
    }

    /** @OnLifecycleEvent(ON_PAUSE) */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
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
