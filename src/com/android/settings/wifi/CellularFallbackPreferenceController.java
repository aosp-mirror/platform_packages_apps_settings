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
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * {@link AbstractPreferenceController} that controls whether we should fall back to celluar when
 * wifi is bad.
 */
public class CellularFallbackPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_CELLULAR_FALLBACK = "wifi_cellular_data_fallback";


    public CellularFallbackPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return !avoidBadWifiConfig();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CELLULAR_FALLBACK;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_CELLULAR_FALLBACK)) {
            return false;
        }
        if (!(preference instanceof SwitchPreference)) {
            return false;
        }
        // On: avoid bad wifi. Off: prompt.
        String settingName = Settings.Global.NETWORK_AVOID_BAD_WIFI;
        Settings.Global.putString(mContext.getContentResolver(), settingName,
                ((SwitchPreference) preference).isChecked() ? "1" : null);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean currentSetting = avoidBadWifiCurrentSettings();
        // TODO: can this ever be null? The return value of avoidBadWifiConfig() can only
        // change if the resources change, but if that happens the activity will be recreated...
        if (preference != null) {
            SwitchPreference pref = (SwitchPreference) preference;
            pref.setChecked(currentSetting);
        }
    }

    private boolean avoidBadWifiConfig() {
        return mContext.getResources().getInteger(
                com.android.internal.R.integer.config_networkAvoidBadWifi) == 1;
    }

    private boolean avoidBadWifiCurrentSettings() {
        return "1".equals(Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.NETWORK_AVOID_BAD_WIFI));
    }
}
