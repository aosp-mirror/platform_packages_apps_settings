/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.wifi;

import android.content.Context;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.MasterSwitchController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.SummaryUpdater;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

// TODO(b/151133650): Replace AbstractPreferenceController with BasePreferenceController.
public class WifiMasterSwitchPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, SummaryUpdater.OnSummaryChangeListener,
        LifecycleObserver, OnResume, OnPause, OnStart, OnStop {

    public static final String KEY_TOGGLE_WIFI = "main_toggle_wifi";

    private MasterSwitchPreference mWifiPreference;
    private WifiEnabler mWifiEnabler;
    private final WifiSummaryUpdater mSummaryHelper;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public WifiMasterSwitchPreferenceController(Context context,
            MetricsFeatureProvider metricsFeatureProvider) {
        super(context);
        mMetricsFeatureProvider = metricsFeatureProvider;
        mSummaryHelper = new WifiSummaryUpdater(mContext, this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mWifiPreference = screen.findPreference(KEY_TOGGLE_WIFI);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_show_wifi_settings);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TOGGLE_WIFI;
    }

    @Override
    public void onResume() {
        mSummaryHelper.register(true);
        if (mWifiEnabler != null) {
            mWifiEnabler.resume(mContext);
        }
    }

    @Override
    public void onPause() {
        if (mWifiEnabler != null) {
            mWifiEnabler.pause();
        }
        mSummaryHelper.register(false);
    }

    @Override
    public void onStart() {
        mWifiEnabler = new WifiEnabler(mContext, new MasterSwitchController(mWifiPreference),
            mMetricsFeatureProvider);
    }

    @Override
    public void onStop() {
        if (mWifiEnabler != null) {
            mWifiEnabler.teardownSwitchController();
        }
    }

    @Override
    public void onSummaryChanged(String summary) {
        if (mWifiPreference != null) {
            mWifiPreference.setSummary(summary);
        }
    }

}
