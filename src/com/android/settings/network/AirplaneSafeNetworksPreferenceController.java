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

import com.android.settings.AirplaneModeEnabler;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.PrimarySwitchController;
import com.android.settings.wifi.WifiEnabler;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;

public class AirplaneSafeNetworksPreferenceController extends AbstractPreferenceController
        implements LifecycleObserver, AirplaneModeEnabler.OnAirplaneModeChangedListener {

    private static final String PREFERENCE_KEY = "airplane_safe_networks";

    private RestrictedSwitchPreference mPreference;

    private AirplaneModeEnabler mAirplaneModeEnabler;
    private WifiEnabler mWifiEnabler;

    public AirplaneSafeNetworksPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        if (lifecycle == null) {
            throw new IllegalArgumentException("Lifecycle must be set");
        }

        mAirplaneModeEnabler = new AirplaneModeEnabler(mContext, this);
        lifecycle.addObserver(this);
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean isAvailable() {
        return mAirplaneModeEnabler.isAirplaneModeOn();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mAirplaneModeEnabler.start();
        if (mPreference != null) {
            mWifiEnabler = new WifiEnabler(mContext, new PrimarySwitchController(mPreference),
                    FeatureFactory.getFactory(mContext).getMetricsFeatureProvider());
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mAirplaneModeEnabler.stop();
        if (mWifiEnabler != null) {
            mWifiEnabler.teardownSwitchController();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        if (mWifiEnabler != null) {
            mWifiEnabler.resume(mContext);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        if (mWifiEnabler != null) {
            mWifiEnabler.pause();
        }
    }

    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        if (mPreference != null) {
            mPreference.setVisible(isAirplaneModeOn);
        }
    }
}
