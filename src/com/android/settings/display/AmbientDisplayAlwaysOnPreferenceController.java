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
package com.android.settings.display;

import static android.provider.Settings.Secure.DOZE_ALWAYS_ON;


import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class AmbientDisplayAlwaysOnPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin,
        Preference.OnPreferenceChangeListener {

    private static final String KEY_ALWAYS_ON = "ambient_display_always_on";
    private static final int MY_USER = UserHandle.myUserId();

    private final AmbientDisplayConfiguration mConfig;

    public AmbientDisplayAlwaysOnPreferenceController(Context context,
            AmbientDisplayConfiguration config) {
        super(context);
        mConfig = config;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ALWAYS_ON;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(mConfig.alwaysOnEnabled(MY_USER));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int enabled = (boolean) newValue ? 1 : 0;
        Settings.Secure.putInt(mContext.getContentResolver(), DOZE_ALWAYS_ON, enabled);
        return true;
    }

    @Override
    public boolean isAvailable() {
        return mConfig.alwaysOnAvailable();
    }
}
