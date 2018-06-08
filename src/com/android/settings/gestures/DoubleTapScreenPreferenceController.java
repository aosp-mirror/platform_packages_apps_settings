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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;

import static android.provider.Settings.Secure.DOZE_PULSE_ON_DOUBLE_TAP;

public class DoubleTapScreenPreferenceController extends GesturePreferenceController {

    private final int ON = 1;
    private final int OFF = 0;

    private static final String PREF_KEY_VIDEO = "gesture_double_tap_screen_video";
    private final String mDoubleTapScreenPrefKey;

    private final String SECURE_KEY = DOZE_PULSE_ON_DOUBLE_TAP;

    private AmbientDisplayConfiguration mAmbientConfig;
    @UserIdInt
    private final int mUserId;

    public DoubleTapScreenPreferenceController(Context context, String key) {
        super(context, key);
        mUserId = UserHandle.myUserId();
        mDoubleTapScreenPrefKey = key;
    }

    public DoubleTapScreenPreferenceController setConfig(AmbientDisplayConfiguration config) {
        mAmbientConfig = config;
        return this;
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences prefs) {
        return isSuggestionComplete(new AmbientDisplayConfiguration(context), prefs);
    }

    @VisibleForTesting
    static boolean isSuggestionComplete(AmbientDisplayConfiguration config,
            SharedPreferences prefs) {
        return !config.pulseOnDoubleTapAvailable()
                || prefs.getBoolean(DoubleTapScreenSettings.PREF_KEY_SUGGESTION_COMPLETE, false);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mAmbientConfig == null) {
            mAmbientConfig = new AmbientDisplayConfiguration(mContext);
        }

        // No hardware support for Double Tap
        if (!mAmbientConfig.doubleTapSensorAvailable()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        // Can't change Double Tap when AOD is enabled.
        if (!mAmbientConfig.ambientDisplayAvailable()) {
            return DISABLED_DEPENDENT_SETTING;
        }

        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_double_tap_screen");
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY,
                isChecked ? ON : OFF);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isChecked() {
        return mAmbientConfig.pulseOnDoubleTapEnabled(mUserId);
    }

    @Override
    //TODO (b/69808376): Remove result payload
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSearchResultPageIntent(mContext,
                DoubleTapScreenSettings.class.getName(), mDoubleTapScreenPrefKey,
                mContext.getString(R.string.display_settings));

        return new InlineSwitchPayload(SECURE_KEY, ResultPayload.SettingsSource.SECURE,
                ON /* onValue */, intent, isAvailable(), ON /* defaultValue */);
    }

    @Override
    protected boolean canHandleClicks() {
        return !mAmbientConfig.alwaysOnEnabled(mUserId);
    }
}