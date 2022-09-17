/*
 * Copyright (C) 2020 The Calyx Institute
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
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;

public class WifiTimeoutPreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {
    private static final String TAG = "WifiTimeoutPrefCtrl";

    public static final int FALLBACK_WIFI_TIMEOUT_VALUE = 0;

    private final String mWifiTimeoutKey;

    protected WifiManager mWifiManager;

    public WifiTimeoutPreferenceController(Context context, String key) {
        super(context, key);
        mWifiTimeoutKey = key;

        mWifiManager = context.getSystemService(WifiManager.class);
        if (mWifiManager == null) {
            Log.e(TAG, "Wifi is not supported on this device");
            return;
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mWifiManager != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return mWifiTimeoutKey;
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference timeoutListPreference = (ListPreference) preference;
        final long currentTimeout = Settings.Global.getLong(mContext.getContentResolver(),
                Settings.Global.WIFI_OFF_TIMEOUT, FALLBACK_WIFI_TIMEOUT_VALUE);
        timeoutListPreference.setValue(String.valueOf(currentTimeout));
        updateTimeoutPreferenceDescription(timeoutListPreference,
                Long.parseLong(timeoutListPreference.getValue()));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            long value = Long.parseLong((String) newValue);
            Settings.Global.putLong(mContext.getContentResolver(), Settings.Global.WIFI_OFF_TIMEOUT, value);
            updateTimeoutPreferenceDescription((ListPreference) preference, value);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist wifi timeout setting", e);
        }
        return true;
    }

    public static CharSequence getTimeoutDescription(
            long currentTimeout, CharSequence[] entries, CharSequence[] values) {
        if (currentTimeout < 0 || entries == null || values == null
                || values.length != entries.length) {
            return null;
        }

        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (currentTimeout == timeout) {
                return entries[i];
            }
        }
        return null;
    }

    private void updateTimeoutPreferenceDescription(ListPreference preference,
                                                    long currentTimeout) {
        final CharSequence[] entries = preference.getEntries();
        final CharSequence[] values = preference.getEntryValues();
        final CharSequence timeoutDescription = getTimeoutDescription(
                currentTimeout, entries, values);
        String summary = "";
        if (timeoutDescription != null) {
            if (currentTimeout != 0)
                summary = mContext.getString(R.string.wifi_timeout_summary, timeoutDescription);
            else
                summary = mContext.getString(R.string.wifi_timeout_summary2);
        }
        preference.setSummary(summary);
    }
}
