/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.gestures;

import android.annotation.UserIdInt;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.core.PreferenceController;

public class DoubleTapScreenPreferenceController extends PreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String PREF_KEY_DOUBLE_TAP_SCREEN = "gesture_double_tap_screen";

    private final AmbientDisplayConfiguration mAmbientConfig;
    @UserIdInt
    private final int mUserId;

    public DoubleTapScreenPreferenceController(Context context, AmbientDisplayConfiguration config,
            @UserIdInt int userId) {
        super(context);
        mAmbientConfig = config;
        mUserId = userId;
    }

    @Override
    public boolean isAvailable() {
        return mAmbientConfig.pulseOnDoubleTapAvailable();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isEnabled = mAmbientConfig.pulseOnDoubleTapEnabled(mUserId);
        if (preference != null) {
            if (preference instanceof TwoStatePreference) {
                ((TwoStatePreference) preference).setChecked(isEnabled);
            } else {
                preference.setSummary(isEnabled
                        ? com.android.settings.R.string.gesture_setting_on
                        : com.android.settings.R.string.gesture_setting_off);
            }
        }
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_DOUBLE_TAP_SCREEN;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean enabled = (boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.DOZE_PULSE_ON_DOUBLE_TAP, enabled ? 1 : 0);
        return true;
    }
}
