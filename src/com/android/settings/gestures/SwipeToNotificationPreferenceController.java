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

import android.util.ArrayMap;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.search2.InlineSwitchPayload;
import com.android.settings.search2.ResultPayload;

public class SwipeToNotificationPreferenceController extends GesturePreferenceController {

    private static final String PREF_KEY_VIDEO = "gesture_swipe_down_fingerprint_video";
    private static final String PREF_KEY_SWIPE_DOWN_FINGERPRINT = "gesture_swipe_down_fingerprint";

    public SwipeToNotificationPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_SWIPE_DOWN_FINGERPRINT;
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_supportSystemNavigationKeys);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, (boolean) newValue ? 1 : 0);
        return true;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, 0)
                == 1;
    }
}
