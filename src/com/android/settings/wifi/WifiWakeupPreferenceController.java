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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.network.NetworkScoreManagerWrapper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * {@link PreferenceControllerMixin} that controls whether the Wi-Fi Wakeup feature should be
 * enabled.
 */
public class WifiWakeupPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    private static final String KEY_ENABLE_WIFI_WAKEUP = "enable_wifi_wakeup";
    private SettingObserver mSettingObserver;

    public WifiWakeupPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        lifecycle.addObserver(this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSettingObserver = new SettingObserver(screen.findPreference(KEY_ENABLE_WIFI_WAKEUP));
    }

    @Override
    public void onResume() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver(), true /* register */);
        }
    }

    @Override
    public void onPause() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver(), false /* register */);
        }
    }

    @Override
    public boolean isAvailable() {
        final int defaultValue = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_wifi_wakeup_available);
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.WIFI_WAKEUP_AVAILABLE, defaultValue) == 1;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_ENABLE_WIFI_WAKEUP)) {
            return false;
        }
        if (!(preference instanceof SwitchPreference)) {
            return false;
        }
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.WIFI_WAKEUP_ENABLED,
                ((SwitchPreference) preference).isChecked() ? 1 : 0);
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ENABLE_WIFI_WAKEUP;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        final SwitchPreference enableWifiWakeup = (SwitchPreference) preference;

        enableWifiWakeup.setChecked(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.WIFI_WAKEUP_ENABLED, 0) == 1);

        boolean wifiScanningEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0) == 1;
        boolean networkRecommendationsEnabled = Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.NETWORK_RECOMMENDATIONS_ENABLED, 0) == 1;
        enableWifiWakeup.setEnabled(networkRecommendationsEnabled && wifiScanningEnabled);

        if (!networkRecommendationsEnabled) {
            enableWifiWakeup.setSummary(R.string.wifi_wakeup_summary_scoring_disabled);
        } else if (!wifiScanningEnabled) {
            enableWifiWakeup.setSummary(R.string.wifi_wakeup_summary_scanning_disabled);
        } else {
            enableWifiWakeup.setSummary(R.string.wifi_wakeup_summary);
        }
    }

    class SettingObserver extends ContentObserver {
        private final Uri NETWORK_RECOMMENDATIONS_ENABLED_URI =
                Settings.Global.getUriFor(Settings.Global.NETWORK_RECOMMENDATIONS_ENABLED);

        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr, boolean register) {
            if (register) {
                cr.registerContentObserver(NETWORK_RECOMMENDATIONS_ENABLED_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (NETWORK_RECOMMENDATIONS_ENABLED_URI.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
