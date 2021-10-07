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

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.GenericSwitchController;
import com.android.settings.wifi.WifiEnabler;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * This controller helps to manage the state of wifi switch preference.
 */
public class WifiSwitchPreferenceController extends AbstractPreferenceController implements
        LifecycleObserver {

    public static final String KEY = "main_toggle_wifi";

    private RestrictedSwitchPreference mPreference;

    private WifiEnabler mWifiEnabler;

    public WifiSwitchPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        if (lifecycle == null) {
            throw new IllegalArgumentException("Lifecycle must be set");
        }

        lifecycle.addObserver(this);
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
    }

    /** Lifecycle.Event.ON_START */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (mPreference != null) {
            mWifiEnabler = new WifiEnabler(mContext, new GenericSwitchController(mPreference),
                    FeatureFactory.getFactory(mContext).getMetricsFeatureProvider());
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
}
