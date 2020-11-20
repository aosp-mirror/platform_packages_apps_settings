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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.SummaryUpdater;
import com.android.settings.wifi.WifiSummaryUpdater;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * PreferenceController to update the internet state.
 */
public class InternetPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, SummaryUpdater.OnSummaryChangeListener,
        LifecycleObserver, OnResume, OnPause {

    public static final String KEY_INTERNET_SETTINGS = "internet_settings";

    private Preference mPreference;
    private final WifiSummaryUpdater mSummaryHelper;

    public InternetPreferenceController(Context context) {
        super(context);
        mSummaryHelper = new WifiSummaryUpdater(mContext, this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_INTERNET_SETTINGS);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_INTERNET_SETTINGS;
    }

    @Override
    public void onResume() {
        mSummaryHelper.register(true);
    }

    @Override
    public void onPause() {
        mSummaryHelper.register(false);
    }

    @Override
    public void onSummaryChanged(String summary) {
        if (mPreference != null) {
            mPreference.setSummary(summary);
        }
    }
}
