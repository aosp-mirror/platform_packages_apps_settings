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

import static com.android.settingslib.RestrictedLockUtilsInternal.checkIfRestrictionEnforced;
import static com.android.settingslib.Utils.updateLocationEnabled;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;


/**
 * A class that listens to location settings change and modifies location settings
 * settings.
 */
public class LocationEnabler implements LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "LocationEnabler";
    @VisibleForTesting
    static final IntentFilter INTENT_FILTER_LOCATION_MODE_CHANGED =
            new IntentFilter(LocationManager.MODE_CHANGED_ACTION);

    private final Context mContext;
    private final UserManager mUserManager;
    private final LocationModeChangeListener mListener;

    @VisibleForTesting
    BroadcastReceiver mReceiver;

    public interface LocationModeChangeListener {
        /** Called when location mode has changed. */
        void onLocationModeChanged(int mode, boolean restricted);
    }

    public LocationEnabler(Context context, LocationModeChangeListener listener,
            Lifecycle lifecycle) {
        mContext = context;
        mListener = listener;
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onStart() {
        if (mReceiver == null) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Received location mode change intent: " + intent);
                    }
                    refreshLocationMode();
                }
            };
        }
        mContext.registerReceiver(mReceiver, INTENT_FILTER_LOCATION_MODE_CHANGED);
        refreshLocationMode();
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
    }

    void refreshLocationMode() {
        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "Location mode has been changed");
        }
        if (mListener != null) {
            mListener.onLocationModeChanged(mode, isRestricted());
        }
    }

    void setLocationEnabled(boolean enabled) {
        final int currentMode = Settings.Secure.getInt(mContext.getContentResolver(),
            Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);

        if (isRestricted()) {
            // Location toggling disabled by user restriction. Read the current location mode to
            // update the location master switch.
            if (Log.isLoggable(TAG, Log.INFO)) {
                Log.i(TAG, "Restricted user, not setting location mode");
            }
            if (mListener != null) {
                mListener.onLocationModeChanged(currentMode, true);
            }
            return;
        }
        updateLocationEnabled(mContext, enabled, UserHandle.myUserId(),
                Settings.Secure.LOCATION_CHANGER_SYSTEM_SETTINGS);
        refreshLocationMode();
    }

    boolean isEnabled(int mode) {
        return mode != Settings.Secure.LOCATION_MODE_OFF && !isRestricted();
    }

    /**
     * Checking if device policy has put a location access lock-down on the managed profile.
     *
     * @return true if device policy has put a location access lock-down on the managed profile
     */
    boolean isManagedProfileRestrictedByBase() {
        final UserHandle managedProfile = Utils.getManagedProfile(mUserManager);
        return managedProfile != null
                && hasShareLocationRestriction(managedProfile.getIdentifier());
    }

    RestrictedLockUtils.EnforcedAdmin getShareLocationEnforcedAdmin(int userId) {
        RestrictedLockUtils.EnforcedAdmin admin =  checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_SHARE_LOCATION, userId);

        if (admin == null) {
            admin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                    mContext, UserManager.DISALLOW_CONFIG_LOCATION, userId);
        }
        return admin;
    }

    boolean hasShareLocationRestriction(int userId) {
        return RestrictedLockUtilsInternal.hasBaseUserRestriction(
                mContext, UserManager.DISALLOW_SHARE_LOCATION, userId);
    }

    private boolean isRestricted() {
        return mUserManager.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION);
    }
}
