/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import android.util.ArrayMap;
import com.android.settings.core.PreferenceController;
import com.android.settings.search2.InlineSwitchPayload;
import com.android.settings.search2.ResultPayload;

import java.util.Map;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;


public class AutoBrightnessPreferenceController extends PreferenceController implements
        Preference.OnPreferenceChangeListener {

    private final String mAutoBrightnessKey;

    public AutoBrightnessPreferenceController(Context context, String key) {
        super(context);
        mAutoBrightnessKey = key;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
    }

    @Override
    public String getPreferenceKey() {
        return mAutoBrightnessKey;
    }

    @Override
    public void updateState(Preference preference) {
        int brightnessMode = Settings.System.getInt(mContext.getContentResolver(),
                SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        ((SwitchPreference) preference).setChecked(brightnessMode != SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean auto = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL);
        return true;
    }

    @Override
    public ResultPayload getResultPayload() {
        final Map<Integer, Boolean> valueMap = new ArrayMap<>();
        valueMap.put(SCREEN_BRIGHTNESS_MODE_AUTOMATIC, true);
        valueMap.put(SCREEN_BRIGHTNESS_MODE_MANUAL, false);

        return new InlineSwitchPayload(SCREEN_BRIGHTNESS_MODE,
                ResultPayload.SettingsSource.SYSTEM, valueMap);
    }
}
