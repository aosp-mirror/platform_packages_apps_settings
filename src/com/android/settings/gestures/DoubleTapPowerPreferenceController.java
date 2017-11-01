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
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settingslib.core.lifecycle.Lifecycle;

import static android.provider.Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED;

public class DoubleTapPowerPreferenceController extends GesturePreferenceController {

    private final int ON = 0;
    private final int OFF = 1;

    private static final String PREF_KEY_VIDEO = "gesture_double_tap_power_video";
    private final String mDoubleTapPowerKey;

    private final String SECURE_KEY = CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED;

    public DoubleTapPowerPreferenceController(Context context, Lifecycle lifecycle, String key) {
        super(context, lifecycle);
        mDoubleTapPowerKey = key;
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences prefs) {
        return !isGestureAvailable(context)
                || prefs.getBoolean(DoubleTapPowerSettings.PREF_KEY_SUGGESTION_COMPLETE, false);
    }

    private static boolean isGestureAvailable(Context context) {
        return context.getResources()
                .getBoolean(com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled);
    }

    @Override
    public boolean isAvailable() {
        return isGestureAvailable(mContext);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public String getPreferenceKey() {
        return mDoubleTapPowerKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY, enabled ? ON : OFF);
        return true;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        final int cameraDisabled = Settings.Secure.getInt(mContext.getContentResolver(),
                SECURE_KEY, ON);
        return cameraDisabled == 0;
    }

    @Override
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSubsettingIntent(mContext,
                DoubleTapPowerSettings.class.getName(), mDoubleTapPowerKey,
                mContext.getString(R.string.display_settings));

        return new InlineSwitchPayload(SECURE_KEY, ResultPayload.SettingsSource.SECURE,
                ON /* onValue */, intent, isAvailable(), ON /* defaultValue */);
    }
}
