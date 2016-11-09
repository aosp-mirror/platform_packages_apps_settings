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

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceController;

public class SwipeToNotificationPreferenceController extends PreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String PREF_KEY_SWIPE_DOWN_FINGERPRINT = "gesture_swipe_down_fingerprint";

    public SwipeToNotificationPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_SWIPE_DOWN_FINGERPRINT;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final boolean isEnabled = isSystemUINavigationEnabled();
        if (preference != null) {
            if (preference instanceof TwoStatePreference) {
                ((TwoStatePreference) preference).setChecked(isEnabled);
            } else {
                preference.setSummary(isEnabled
                        ? R.string.gesture_setting_on
                        : R.string.gesture_setting_off);
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_supportSystemNavigationKeys);
    }

    private boolean isSystemUINavigationEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, 0)
                == 1;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, (boolean) newValue ? 1 : 0);
        return true;
    }
}
