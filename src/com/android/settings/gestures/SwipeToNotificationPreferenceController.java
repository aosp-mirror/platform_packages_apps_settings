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

import static android.provider.Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settings.Utils;

public class SwipeToNotificationPreferenceController extends GesturePreferenceController {

    private static final int ON = 1;
    private static final int OFF = 0;

    private static final String PREF_KEY_VIDEO = "gesture_swipe_down_fingerprint_video";

    private static final String SECURE_KEY = SYSTEM_NAVIGATION_KEYS_ENABLED;

    public SwipeToNotificationPreferenceController(Context context, String key) {
        super(context, key);
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences prefs) {
        return !isGestureAvailable(context)
                || prefs.getBoolean(SwipeToNotificationSettings.PREF_KEY_SUGGESTION_COMPLETE,
                false);
    }

    private static boolean isGestureAvailable(Context context) {
        return Utils.hasFingerprintHardware(context)
                && context.getResources()
                .getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public int getAvailabilityStatus() {
        return isAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_swipe_down_fingerprint");
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        setSwipeToNotification(mContext, isChecked);
        return true;
    }

    @Override
    public boolean isChecked() {
        return isSwipeToNotificationOn(mContext);
    }

    public static boolean isSwipeToNotificationOn(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), SECURE_KEY, OFF) == ON;
    }

    public static boolean setSwipeToNotification(Context context, boolean isEnabled) {
        return Settings.Secure.putInt(
                context.getContentResolver(), SECURE_KEY, isEnabled ? ON : OFF);
    }

    public static boolean isAvailable(Context context) {
        return isGestureAvailable(context);
    }
}
