/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.provider.Settings.Secure.DOZE_WAKE_LOCK_SCREEN_GESTURE;

import android.annotation.UserIdInt;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.hardware.AmbientDisplayConfiguration;

public class WakeLockScreenGesturePreferenceController extends GesturePreferenceController {

    private static final int ON = 1;
    private static final int OFF = 0;

    private static final String PREF_KEY_VIDEO = "gesture_wake_lock_screen_video";
    private final String mWakeLockScreenPrefKey;

    private AmbientDisplayConfiguration mAmbientConfig;
    @UserIdInt
    private final int mUserId;

    public WakeLockScreenGesturePreferenceController(Context context, String key) {
        super(context, key);
        mUserId = UserHandle.myUserId();
        mWakeLockScreenPrefKey = key;
    }

    public WakeLockScreenGesturePreferenceController
        setConfig(AmbientDisplayConfiguration config) {
        mAmbientConfig = config;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        // No hardware support for this Gesture
        if (!getAmbientConfig().wakeScreenGestureAvailable()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_wake_lock_screen");
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isChecked() {
        return getAmbientConfig().wakeLockScreenGestureEnabled(mUserId);
    }

    @Override
    public String getPreferenceKey() {
        return mWakeLockScreenPrefKey;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), DOZE_WAKE_LOCK_SCREEN_GESTURE,
                isChecked ? ON : OFF);
    }

    private AmbientDisplayConfiguration getAmbientConfig() {
        if (mAmbientConfig == null) {
            mAmbientConfig = new AmbientDisplayConfiguration(mContext);
        }

        return mAmbientConfig;
    }
}
