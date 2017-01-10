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

import com.android.settings.core.PreferenceController;

public class AllowRecommendationPreferenceController extends PreferenceController {

    private static final String KEY_ALLOW_RECOMMENDATIONS = "allow_recommendations";

    public AllowRecommendationPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.NETWORK_RECOMMENDATIONS_ENABLED, 0) == 1);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_ALLOW_RECOMMENDATIONS)) {
            return false;
        }
        if (!(preference instanceof SwitchPreference)) {
            return false;
        }
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.NETWORK_RECOMMENDATIONS_ENABLED,
                ((SwitchPreference) preference).isChecked() ? 1 : 0);
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ALLOW_RECOMMENDATIONS;
    }
}
