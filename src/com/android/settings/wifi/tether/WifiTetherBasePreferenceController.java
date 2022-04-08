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

package com.android.settings.wifi.tether;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

// TODO(b/151133650): Replace AbstractPreferenceController with BasePreferenceController.
public abstract class WifiTetherBasePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    public interface OnTetherConfigUpdateListener {
        void onTetherConfigUpdated(AbstractPreferenceController context);
    }

    protected final WifiManager mWifiManager;
    protected final String[] mWifiRegexs;
    protected final ConnectivityManager mCm;
    protected final OnTetherConfigUpdateListener mListener;

    protected Preference mPreference;

    public WifiTetherBasePreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context);
        mListener = listener;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mCm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiRegexs = mCm.getTetherableWifiRegexs();
    }

    @Override
    public boolean isAvailable() {
        return mWifiManager != null && mWifiRegexs != null && mWifiRegexs.length > 0;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateDisplay();
    }

    public abstract void updateDisplay();
}
