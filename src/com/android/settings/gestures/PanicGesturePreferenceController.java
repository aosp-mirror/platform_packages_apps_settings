/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

/**
 * Preference controller for emergency sos gesture setting
 */
public class PanicGesturePreferenceController extends GesturePreferenceController {

    @VisibleForTesting
    static final int ON = 1;
    @VisibleForTesting
    static final int OFF = 0;

    private static final String PREF_KEY_VIDEO = "panic_button_screen_video";

    private static final String SECURE_KEY = Settings.Secure.PANIC_GESTURE_ENABLED;

    public PanicGesturePreferenceController(Context context, String key) {
        super(context, key);
    }

    private static boolean isGestureAvailable(Context context) {
        return context.getResources()
                .getBoolean(R.bool.config_show_panic_gesture_settings);
    }

    @Override
    public int getAvailabilityStatus() {
        return isGestureAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_panic_button");
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(), SECURE_KEY, OFF) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY,
                isChecked ? ON : OFF);
    }
}
