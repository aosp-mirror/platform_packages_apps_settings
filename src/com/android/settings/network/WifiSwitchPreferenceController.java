/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import static com.android.settingslib.wifi.WifiEnterpriseRestrictionUtils.isChangeWifiStateAllowed;

import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.GenericSwitchController;
import com.android.settings.wifi.WifiEnabler;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import com.google.common.annotations.VisibleForTesting;

/**
 * This controller helps to manage the state of wifi switch preference.
 */
public class WifiSwitchPreferenceController extends AbstractPreferenceController implements
        LifecycleObserver {

    public static final String KEY = "main_toggle_wifi";

    @VisibleForTesting
    boolean mIsChangeWifiStateAllowed;
    @VisibleForTesting
    WifiEnabler mWifiEnabler;

    private RestrictedSwitchPreference mPreference;

    public WifiSwitchPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        if (lifecycle == null) {
            throw new IllegalArgumentException("Lifecycle must be set");
        }

        lifecycle.addObserver(this);
        mIsChangeWifiStateAllowed = isChangeWifiStateAllowed(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference == null) return;

        mPreference.setChecked(isWifiEnabled());
        if (!mIsChangeWifiStateAllowed) {
            mPreference.setEnabled(false);
            mPreference.setSummary(R.string.not_allowed_by_ent);
        }
    }

    /** Lifecycle.Event.ON_START */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        // Don't use WifiEnabler when user is not allowed to change Wi-Fi state,
        // Because the preference needs to be disabled when the user is not allowed to change the
        // Wi-Fi state, but WifiEnabler will enable the preference when the Wi-Fi state changes.
        if (mPreference != null && mIsChangeWifiStateAllowed) {
            mWifiEnabler = new WifiEnabler(mContext, new GenericSwitchController(mPreference),
                    FeatureFactory.getFeatureFactory().getMetricsFeatureProvider());
        }
    }

    /** Lifecycle.Event.ON_STOP */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        if (mWifiEnabler != null) {
            mWifiEnabler.teardownSwitchController();
        }
    }

    /** Lifecycle.Event.ON_RESUME */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        if (mWifiEnabler != null) {
            mWifiEnabler.resume(mContext);
        }
    }

    /** Lifecycle.Event.ON_PAUSE */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        if (mWifiEnabler != null) {
            mWifiEnabler.pause();
        }
    }

    private boolean isWifiEnabled() {
        WifiManager wifiManager = mContext.getSystemService(WifiManager.class);
        if (wifiManager == null) return false;
        return wifiManager.isWifiEnabled();
    }
}
