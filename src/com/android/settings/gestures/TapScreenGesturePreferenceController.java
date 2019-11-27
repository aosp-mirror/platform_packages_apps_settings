/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.gestures;

import static android.provider.Settings.Secure.DOZE_TAP_SCREEN_GESTURE;

import android.annotation.UserIdInt;
import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.UserHandle;
import android.provider.Settings;

public class TapScreenGesturePreferenceController extends GesturePreferenceController {

    private static final String PREF_KEY_VIDEO = "gesture_tap_screen_video";

    private AmbientDisplayConfiguration mAmbientConfig;
    @UserIdInt
    private final int mUserId;

    public TapScreenGesturePreferenceController(Context context, String key) {
        super(context, key);
        mUserId = UserHandle.myUserId();
    }

    public TapScreenGesturePreferenceController setConfig(AmbientDisplayConfiguration config) {
        mAmbientConfig = config;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        // No hardware support for this Gesture
        if (!getAmbientConfig().tapSensorAvailable()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        return AVAILABLE;
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public CharSequence getSummary() {
        return super.getSummary();
    }

    @Override
    public boolean isChecked() {
        return getAmbientConfig().tapGestureEnabled(mUserId);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), DOZE_TAP_SCREEN_GESTURE,
                isChecked ? 1 : 0);
    }

    private AmbientDisplayConfiguration getAmbientConfig() {
        if (mAmbientConfig == null) {
            mAmbientConfig = new AmbientDisplayConfiguration(mContext);
        }
        return mAmbientConfig;
    }
}