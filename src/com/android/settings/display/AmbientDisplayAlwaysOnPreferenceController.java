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

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settingslib.core.AbstractPreferenceController;

public class AmbientDisplayAlwaysOnPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin,
        Preference.OnPreferenceChangeListener {

    private final int ON = 1;
    private final int OFF = 0;

    public static final String KEY_ALWAYS_ON = "ambient_display_always_on";
    private static final int MY_USER = UserHandle.myUserId();

    private final AmbientDisplayConfiguration mConfig;
    private final OnPreferenceChangedCallback mCallback;

    public interface OnPreferenceChangedCallback {
        void onPreferenceChanged();
    }

    public AmbientDisplayAlwaysOnPreferenceController(Context context,
            AmbientDisplayConfiguration config, OnPreferenceChangedCallback callback) {
        super(context);
        mConfig = config;
        mCallback = callback;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ALWAYS_ON;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(isAlwaysOnEnabled(mConfig));
    }

    public static boolean isAlwaysOnEnabled(AmbientDisplayConfiguration config) {
        return config.alwaysOnEnabled(MY_USER);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int enabled = (boolean) newValue ? ON : OFF;
        Settings.Secure.putInt(
                mContext.getContentResolver(), Settings.Secure.DOZE_ALWAYS_ON, enabled);
        if (mCallback != null) {
            mCallback.onPreferenceChanged();
        }
        return true;
    }

    @Override
    public boolean isAvailable() {
        return isAvailable(mConfig);
    }

    public static boolean isAvailable(AmbientDisplayConfiguration config) {
        return config.alwaysOnAvailableForUser(MY_USER);
    }

    public static boolean accessibilityInversionEnabled(AmbientDisplayConfiguration config) {
        return config.accessibilityInversionEnabled(MY_USER);
    }

    @Override
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSubsettingIntent(mContext,
                AmbientDisplaySettings.class.getName(), KEY_ALWAYS_ON,
                mContext.getString(R.string.ambient_display_screen_title));

        return new InlineSwitchPayload(Settings.Secure.DOZE_ALWAYS_ON,
                ResultPayload.SettingsSource.SECURE, ON /* onValue */, intent, isAvailable(),
                ON /* defaultValue */);
    }
}
