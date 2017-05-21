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

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.core.lifecycle.Lifecycle;

public class PickupGesturePreferenceController extends GesturePreferenceController {

    private static final String PREF_VIDEO_KEY = "gesture_pick_up_video";
    private final String mPickUpPrefKey;

    private final AmbientDisplayConfiguration mAmbientConfig;
    @UserIdInt
    private final int mUserId;

    public PickupGesturePreferenceController(Context context, Lifecycle lifecycle,
            AmbientDisplayConfiguration config, @UserIdInt int userId, String key) {
        super(context, lifecycle);
        mAmbientConfig = config;
        mUserId = userId;
        mPickUpPrefKey = key;
    }

    @Override
    public boolean isAvailable() {
        return mAmbientConfig.pulseOnPickupAvailable();
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_VIDEO_KEY;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        return mAmbientConfig.pulseOnPickupEnabled(mUserId);
    }

    @Override
    public String getPreferenceKey() {
        return mPickUpPrefKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean enabled = (boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.DOZE_PULSE_ON_PICK_UP, enabled ? 1 : 0);
        return true;
    }
}
